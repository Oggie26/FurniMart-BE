package com.example.userservice.service;

import com.example.userservice.entity.*;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.repository.*;
import com.example.userservice.response.ChatResponse;
import com.example.userservice.service.inteface.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.kafka.enabled=false",
    "app.kafka.enabled=false"
})
@ActiveProfiles("test")
class ChatServiceLoadTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Chat testChat;
    private List<Employee> testStaff;
    private User testCustomer;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create test customer
        Account customerAccount = Account.builder()
                .email("customer@test.com")
                .password("password")
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .build();
        customerAccount = accountRepository.save(customerAccount);

        testCustomer = User.builder()
                .fullName("Test Customer")
                .account(customerAccount)
                .status(EnumStatus.ACTIVE)
                .build();
        testCustomer = userRepository.save(testCustomer);

        // Create test chat in WAITING_STAFF mode
        testChat = Chat.builder()
                .name("Test Chat")
                .type(Chat.ChatType.PRIVATE)
                .status(EnumStatus.ACTIVE)
                .createdBy(testCustomer)
                .chatMode(Chat.ChatMode.WAITING_STAFF)
                .staffRequestedAt(java.time.LocalDateTime.now())
                .build();
        testChat = chatRepository.save(testChat);

        // Create test staff (10 staff for load test)
        testStaff = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Account staffAccount = Account.builder()
                    .email("staff" + i + "@test.com")
                    .password("password")
                    .role(EnumRole.STAFF)
                    .status(EnumStatus.ACTIVE)
                    .build();
            staffAccount = accountRepository.save(staffAccount);

            Employee staff = Employee.builder()
                    .code("STAFF" + i)
                    .fullName("Staff " + i)
                    .account(staffAccount)
                    .status(EnumStatus.ACTIVE)
                    .build();
            staff = employeeRepository.save(staff);
            testStaff.add(staff);
        }
    }

    /**
     * Helper method to set SecurityContext for a specific staff
     */
    private void setSecurityContextForStaff(Employee staff) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                staff.getAccount().getEmail(),
                null,
                Set.of(new SimpleGrantedAuthority("ROLE_" + staff.getAccount().getRole().name()))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @Transactional
    void testPessimisticLock_LoadTest_10Concurrent() throws InterruptedException {
        // Load test with 10 concurrent requests
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        List<Future<TestResult>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();
        List<String> errors = new CopyOnWriteArrayList<>();

        long startTime = System.currentTimeMillis();

        // Launch concurrent requests
        for (int i = 0; i < numberOfThreads; i++) {
            final int staffIndex = i;
            final Employee staff = testStaff.get(staffIndex);
            
            Future<TestResult> future = executor.submit(() -> {
                try {
                    // Set SecurityContext for this thread
                    setSecurityContextForStaff(staff);
                    
                    latch.countDown();
                    latch.await(); // Wait for all threads to be ready
                    
                    long requestStart = System.currentTimeMillis();
                    
                    // Try to accept the chat
                    ChatResponse response = chatService.acceptStaffConnection(testChat.getId());
                    
                    long requestEnd = System.currentTimeMillis();
                    long responseTime = requestEnd - requestStart;
                    responseTimes.add(responseTime);
                    
                    if (response.getAssignedStaffId() != null) {
                        successCount.incrementAndGet();
                        return new TestResult(true, responseTime, null);
                    } else {
                        failureCount.incrementAndGet();
                        return new TestResult(false, responseTime, "No assigned staff ID");
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    long requestEnd = System.currentTimeMillis();
                    long responseTime = requestEnd - System.currentTimeMillis();
                    responseTimes.add(responseTime);
                    errors.add(e.getMessage());
                    return new TestResult(false, responseTime, e.getMessage());
                }
            });
            futures.add(future);
        }

        // Wait for all threads to complete
        executor.shutdown();
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS), "Test timeout after 60 seconds");

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Collect results
        List<TestResult> results = new ArrayList<>();
        for (Future<TestResult> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                results.add(new TestResult(false, 0, e.getMessage()));
            }
        }

        // Calculate statistics
        long avgResponseTime = responseTimes.isEmpty() ? 0 : 
            (long) responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxResponseTime = responseTimes.isEmpty() ? 0 : 
            responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minResponseTime = responseTimes.isEmpty() ? 0 : 
            responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);

        // Print results
        System.out.println("\n=== LOAD TEST RESULTS ===");
        System.out.println("Total Requests: " + numberOfThreads);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Failed: " + failureCount.get());
        System.out.println("Total Time: " + totalTime + " ms");
        System.out.println("Average Response Time: " + avgResponseTime + " ms");
        System.out.println("Max Response Time: " + maxResponseTime + " ms");
        System.out.println("Min Response Time: " + minResponseTime + " ms");
        
        if (!errors.isEmpty()) {
            System.out.println("\nErrors:");
            errors.forEach(error -> System.out.println("  - " + error));
        }

        // Verify only one staff accepted
        Chat updatedChat = chatRepository.findById(testChat.getId()).orElse(null);
        assertNotNull(updatedChat, "Chat should still exist");
        
        if (successCount.get() > 0) {
            assertEquals(Chat.ChatMode.STAFF_CONNECTED, updatedChat.getChatMode(), 
                "Chat should be in STAFF_CONNECTED mode");
            assertNotNull(updatedChat.getAssignedStaffId(), 
                "Chat should have assigned staff ID");
        }

        // Verify only 1 successful accept (pessimistic lock should prevent race condition)
        assertTrue(successCount.get() <= 1, 
            "Expected at most 1 successful accept, but got: " + successCount.get() + 
            ". Pessimistic lock should prevent race condition.");

        // Performance assertions
        assertTrue(avgResponseTime < 5000, 
            "Average response time should be less than 5 seconds. Got: " + avgResponseTime + " ms");
        assertTrue(maxResponseTime < 10000, 
            "Max response time should be less than 10 seconds. Got: " + maxResponseTime + " ms");
    }

    @Test
    @Transactional
    void testPessimisticLock_LoadTest_20Concurrent() throws InterruptedException {
        // More aggressive load test with 20 concurrent requests
        int numberOfThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();

        long startTime = System.currentTimeMillis();

        // Create additional staff if needed
        while (testStaff.size() < numberOfThreads) {
            int i = testStaff.size() + 1;
            Account staffAccount = Account.builder()
                    .email("staff" + i + "@test.com")
                    .password("password")
                    .role(EnumRole.STAFF)
                    .status(EnumStatus.ACTIVE)
                    .build();
            staffAccount = accountRepository.save(staffAccount);

            Employee staff = Employee.builder()
                    .code("STAFF" + i)
                    .fullName("Staff " + i)
                    .account(staffAccount)
                    .status(EnumStatus.ACTIVE)
                    .build();
            staff = employeeRepository.save(staff);
            testStaff.add(staff);
        }

        // Launch concurrent requests
        for (int i = 0; i < numberOfThreads; i++) {
            final int staffIndex = i;
            final Employee staff = testStaff.get(staffIndex);
            
            executor.submit(() -> {
                try {
                    setSecurityContextForStaff(staff);
                    latch.countDown();
                    latch.await();
                    
                    long requestStart = System.currentTimeMillis();
                    ChatResponse response = chatService.acceptStaffConnection(testChat.getId());
                    long requestEnd = System.currentTimeMillis();
                    
                    responseTimes.add(requestEnd - requestStart);
                    
                    if (response.getAssignedStaffId() != null) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    responseTimes.add(System.currentTimeMillis() - System.currentTimeMillis());
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(120, TimeUnit.SECONDS));

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        long avgResponseTime = responseTimes.isEmpty() ? 0 : 
            (long) responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        System.out.println("\n=== LOAD TEST RESULTS (20 Concurrent) ===");
        System.out.println("Total Requests: " + numberOfThreads);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Failed: " + failureCount.get());
        System.out.println("Total Time: " + totalTime + " ms");
        System.out.println("Average Response Time: " + avgResponseTime + " ms");

        // Verify only 1 successful accept
        assertTrue(successCount.get() <= 1, 
            "Expected at most 1 successful accept with 20 concurrent requests");
    }

    // Helper class for test results
    @SuppressWarnings("unused")
    private static class TestResult {
        boolean success;
        long responseTime;
        String error;

        TestResult(boolean success, long responseTime, String error) {
            this.success = success;
            this.responseTime = responseTime;
            this.error = error;
        }
    }
}

