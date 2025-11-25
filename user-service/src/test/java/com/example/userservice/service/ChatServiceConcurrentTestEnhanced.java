package com.example.userservice.service;

import com.example.userservice.entity.*;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.kafka.enabled=false",
    "app.kafka.enabled=false"
})
@ActiveProfiles("test")
class ChatServiceConcurrentTestEnhanced {

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

        // Create test staff (3 staff for race condition test)
        testStaff = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
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
    void testConcurrentAcceptStaffConnection_WithSecurityContext() throws InterruptedException {
        // This test verifies that only one staff can accept the same chat
        // even when multiple staff try to accept simultaneously with proper SecurityContext
        
        int numberOfThreads = 3;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        List<Future<ChatResponse>> futures = new ArrayList<>();
        List<String> acceptedStaffIds = new CopyOnWriteArrayList<>();
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        // Simulate multiple staff trying to accept the same chat concurrently
        for (int i = 0; i < numberOfThreads; i++) {
            final int staffIndex = i;
            final Employee staff = testStaff.get(staffIndex);
            
            Future<ChatResponse> future = executor.submit(() -> {
                try {
                    // Set SecurityContext for this thread
                    setSecurityContextForStaff(staff);
                    
                    latch.countDown();
                    latch.await(); // Wait for all threads to be ready
                    
                    // Try to accept the chat
                    ChatResponse response = chatService.acceptStaffConnection(testChat.getId());
                    if (response.getAssignedStaffId() != null) {
                        acceptedStaffIds.add(response.getAssignedStaffId());
                    }
                    return response;
                } catch (Exception e) {
                    exceptions.add(e);
                    return null;
                }
            });
            futures.add(future);
        }

        // Wait for all threads to complete
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        // Verify results
        // Only one staff should have successfully accepted
        long successfulAccepts = acceptedStaffIds.size();
        
        // Verify: Only 1 successful accept (others should get CHAT_ALREADY_ACCEPTED or similar)
        assertTrue(successfulAccepts <= 1, 
            "Expected at most 1 successful accept, but got: " + successfulAccepts + ". Accepted staff IDs: " + acceptedStaffIds);
        
        // Verify: If one accepted, check the chat state
        if (successfulAccepts == 1) {
            Chat updatedChat = chatRepository.findById(testChat.getId()).orElse(null);
            assertNotNull(updatedChat);
            assertEquals(Chat.ChatMode.STAFF_CONNECTED, updatedChat.getChatMode());
            assertNotNull(updatedChat.getAssignedStaffId());
            assertTrue(acceptedStaffIds.contains(updatedChat.getAssignedStaffId()),
                "Accepted staff ID should match chat's assigned staff ID");
        }

        // Verify: Other attempts should have failed (handled by pessimistic lock)
        // Note: Some may succeed if they complete before others, but only one should win
    }

    @Test
    @Transactional
    void testPessimisticLock_SequentialAccept() {
        // Test sequential acceptance - second staff should fail
        Employee firstStaff = testStaff.get(0);
        Employee secondStaff = testStaff.get(1);
        
        // First staff accepts
        setSecurityContextForStaff(firstStaff);
        ChatResponse firstResponse = chatService.acceptStaffConnection(testChat.getId());
        
        assertNotNull(firstResponse);
        assertEquals(Chat.ChatMode.STAFF_CONNECTED, firstResponse.getChatMode());
        assertEquals(firstStaff.getId(), firstResponse.getAssignedStaffId());
        
        // Second staff tries to accept (should fail with exception)
        setSecurityContextForStaff(secondStaff);
        
        // Expect exception when second staff tries to accept already accepted chat
        AppException exception = assertThrows(AppException.class, () -> {
            chatService.acceptStaffConnection(testChat.getId());
        });
        
        assertEquals(ErrorCode.CHAT_ALREADY_ACCEPTED, exception.getErrorCode());
        
        // Verify chat is still assigned to first staff
        Chat updatedChat = chatRepository.findById(testChat.getId()).orElse(null);
        assertNotNull(updatedChat);
        assertEquals(Chat.ChatMode.STAFF_CONNECTED, updatedChat.getChatMode());
        assertEquals(firstStaff.getId(), updatedChat.getAssignedStaffId());
    }
}

