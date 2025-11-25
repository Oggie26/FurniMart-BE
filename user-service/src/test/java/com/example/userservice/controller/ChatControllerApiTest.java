package com.example.userservice.controller;

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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.kafka.enabled=false",
    "app.kafka.enabled=false"
})
@ActiveProfiles("test")
class ChatControllerApiTest {

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

    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    private Chat testChat;
    private User testCustomer;
    private Employee testStaff;
    private User testStaffUser;

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

        // Create test staff
        Account staffAccount = Account.builder()
                .email("staff@test.com")
                .password("password")
                .role(EnumRole.STAFF)
                .status(EnumStatus.ACTIVE)
                .build();
        staffAccount = accountRepository.save(staffAccount);

        testStaff = Employee.builder()
                .fullName("Test Staff")
                .account(staffAccount)
                .status(EnumStatus.ACTIVE)
                .code("STAFF001")
                .build();
        testStaff = employeeRepository.save(testStaff);

        // Create user for staff
        testStaffUser = User.builder()
                .fullName("Staff User")
                .account(staffAccount)
                .status(EnumStatus.ACTIVE)
                .build();
        testStaffUser = userRepository.save(testStaffUser);

        // Create test chat in AI mode
        testChat = Chat.builder()
                .name("Test Chat")
                .type(Chat.ChatType.PRIVATE)
                .status(EnumStatus.ACTIVE)
                .createdBy(testCustomer)
                .chatMode(Chat.ChatMode.AI)
                .build();
        testChat = chatRepository.save(testChat);

        // Add customer as participant
        ChatParticipant participant = ChatParticipant.builder()
                .chat(testChat)
                .user(testCustomer)
                .role(ChatParticipant.ParticipantRole.ADMIN)
                .status(EnumStatus.ACTIVE)
                .build();
        chatParticipantRepository.save(participant);
    }

    private void setSecurityContextForUser(User user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getAccount().getRole().name()));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getAccount().getEmail(),
                null,
                authorities
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    // Test requestStaffConnection endpoint
    @Test
    @Transactional
    void testRequestStaffConnection_Success() {
        // Set up customer security context
        setSecurityContextForUser(testCustomer);

        // Request staff connection
        ChatResponse response = chatService.requestStaffConnection(testChat.getId());

        // Verify response
        assertNotNull(response);
        assertEquals(testChat.getId(), response.getId());
        assertEquals(Chat.ChatMode.WAITING_STAFF, response.getChatMode());
        assertNotNull(response.getStaffRequestedAt());

        // Verify chat in database
        Chat updatedChat = chatRepository.findById(testChat.getId()).orElse(null);
        assertNotNull(updatedChat);
        assertEquals(Chat.ChatMode.WAITING_STAFF, updatedChat.getChatMode());
    }

    @Test
    @Transactional
    void testRequestStaffConnection_NotCustomer() {
        // Set up staff security context (should fail)
        setSecurityContextForUser(testStaffUser);

        // Try to request staff connection as staff (should fail)
        // Note: This will fail at authorization level, but we test service level
        // In real scenario, @PreAuthorize will block this
        // The service will check if user is customer, so it should throw ACCESS_DENIED
        AppException exception = assertThrows(AppException.class, () -> {
            chatService.requestStaffConnection(testChat.getId());
        });
        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    // Test acceptStaffConnection endpoint
    @Test
    @Transactional
    void testAcceptStaffConnection_Success() {
        // First, request staff connection
        setSecurityContextForUser(testCustomer);
        chatService.requestStaffConnection(testChat.getId());

        // Then, accept as staff
        setSecurityContextForUser(testStaffUser);
        ChatResponse response = chatService.acceptStaffConnection(testChat.getId());

        // Verify response
        assertNotNull(response);
        assertEquals(testChat.getId(), response.getId());
        assertEquals(Chat.ChatMode.STAFF_CONNECTED, response.getChatMode());
        assertEquals(testStaff.getId(), response.getAssignedStaffId());

        // Verify chat in database
        Chat updatedChat = chatRepository.findById(testChat.getId()).orElse(null);
        assertNotNull(updatedChat);
        assertEquals(Chat.ChatMode.STAFF_CONNECTED, updatedChat.getChatMode());
        assertEquals(testStaff.getId(), updatedChat.getAssignedStaffId());
    }

    @Test
    @Transactional
    void testAcceptStaffConnection_ChatNotWaiting() {
        // Set up staff security context
        setSecurityContextForUser(userRepository.findByEmailAndIsDeletedFalse(testStaff.getAccount().getEmail()).orElse(null));

        // Try to accept chat that is not in WAITING_STAFF mode
        AppException exception = assertThrows(AppException.class, () -> {
            chatService.acceptStaffConnection(testChat.getId());
        });

        assertEquals(ErrorCode.CHAT_ALREADY_ACCEPTED, exception.getErrorCode());
    }

    @Test
    @Transactional
    void testAcceptStaffConnection_NotStaff() {
        // Request staff connection first
        setSecurityContextForUser(testCustomer);
        chatService.requestStaffConnection(testChat.getId());

        // Try to accept as customer (should fail)
        // Note: This will fail at authorization level, but we test service level
        // The service will try to find Employee by accountId, but customer doesn't have Employee record
        // So it will throw USER_NOT_FOUND
        setSecurityContextForUser(testCustomer);
        AppException exception = assertThrows(AppException.class, () -> {
            chatService.acceptStaffConnection(testChat.getId());
        });

        // Service will throw USER_NOT_FOUND because customer doesn't have Employee record
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    // Test endStaffChat endpoint
    @Test
    @Transactional
    void testEndStaffChat_Success_AsCustomer() {
        // Setup: request and accept staff connection
        setSecurityContextForUser(testCustomer);
        chatService.requestStaffConnection(testChat.getId());

        setSecurityContextForUser(testStaffUser);
        chatService.acceptStaffConnection(testChat.getId());

        // End staff chat as customer
        setSecurityContextForUser(testCustomer);
        ChatResponse response = chatService.endStaffChat(testChat.getId());

        // Verify response
        assertNotNull(response);
        assertEquals(testChat.getId(), response.getId());
        assertEquals(Chat.ChatMode.AI, response.getChatMode());
        assertNull(response.getAssignedStaffId());

        // Verify chat in database
        Chat updatedChat = chatRepository.findById(testChat.getId()).orElse(null);
        assertNotNull(updatedChat);
        assertEquals(Chat.ChatMode.AI, updatedChat.getChatMode());
        assertNull(updatedChat.getAssignedStaffId());
    }

    @Test
    @Transactional
    void testEndStaffChat_Success_AsStaff() {
        // Setup: request and accept staff connection
        setSecurityContextForUser(testCustomer);
        chatService.requestStaffConnection(testChat.getId());

        setSecurityContextForUser(testStaffUser);
        chatService.acceptStaffConnection(testChat.getId());

        // End staff chat as staff
        ChatResponse response = chatService.endStaffChat(testChat.getId());

        // Verify response
        assertNotNull(response);
        assertEquals(testChat.getId(), response.getId());
        assertEquals(Chat.ChatMode.AI, response.getChatMode());
    }

    @Test
    @Transactional
    void testEndStaffChat_ChatNotConnected() {
        // Set up customer security context
        setSecurityContextForUser(testCustomer);

        // Try to end staff chat when chat is not in STAFF_CONNECTED mode
        AppException exception = assertThrows(AppException.class, () -> {
            chatService.endStaffChat(testChat.getId());
        });

        assertEquals(ErrorCode.INVALID_CHAT_STATE, exception.getErrorCode());
    }

    // Test getChatStatus endpoint
    @Test
    @Transactional
    void testGetChatStatus_Success() {
        // Set up customer security context
        setSecurityContextForUser(testCustomer);

        // Get chat status
        ChatResponse response = chatService.getChatById(testChat.getId());

        // Verify response
        assertNotNull(response);
        assertEquals(testChat.getId(), response.getId());
        assertEquals(Chat.ChatMode.AI, response.getChatMode());
    }

    @Test
    @Transactional
    void testGetChatStatus_AfterRequestStaff() {
        // Request staff connection
        setSecurityContextForUser(testCustomer);
        chatService.requestStaffConnection(testChat.getId());

        // Get chat status
        ChatResponse response = chatService.getChatById(testChat.getId());

        // Verify response
        assertNotNull(response);
        assertEquals(Chat.ChatMode.WAITING_STAFF, response.getChatMode());
        assertNotNull(response.getStaffRequestedAt());
    }

    @Test
    @Transactional
    void testGetChatStatus_AfterAcceptStaff() {
        // Setup: request and accept staff connection
        setSecurityContextForUser(testCustomer);
        chatService.requestStaffConnection(testChat.getId());

        setSecurityContextForUser(testStaffUser);
        chatService.acceptStaffConnection(testChat.getId());

        // Get chat status
        setSecurityContextForUser(testCustomer);
        ChatResponse response = chatService.getChatById(testChat.getId());

        // Verify response
        assertNotNull(response);
        assertEquals(Chat.ChatMode.STAFF_CONNECTED, response.getChatMode());
        assertEquals(testStaff.getId(), response.getAssignedStaffId());
    }

    // Test full flow: request → accept → end
    @Test
    @Transactional
    void testFullFlow_RequestAcceptEnd() {
        // Step 1: Customer requests staff connection
        setSecurityContextForUser(testCustomer);
        ChatResponse requestResponse = chatService.requestStaffConnection(testChat.getId());
        assertEquals(Chat.ChatMode.WAITING_STAFF, requestResponse.getChatMode());

        // Step 2: Staff accepts connection
        setSecurityContextForUser(userRepository.findByEmailAndIsDeletedFalse(testStaff.getAccount().getEmail()).orElse(null));
        ChatResponse acceptResponse = chatService.acceptStaffConnection(testChat.getId());
        assertEquals(Chat.ChatMode.STAFF_CONNECTED, acceptResponse.getChatMode());
        assertEquals(testStaff.getId(), acceptResponse.getAssignedStaffId());

        // Step 3: Customer ends staff chat
        setSecurityContextForUser(testCustomer);
        ChatResponse endResponse = chatService.endStaffChat(testChat.getId());
        assertEquals(Chat.ChatMode.AI, endResponse.getChatMode());
        assertNull(endResponse.getAssignedStaffId());

        // Verify final state
        Chat finalChat = chatRepository.findById(testChat.getId()).orElse(null);
        assertNotNull(finalChat);
        assertEquals(Chat.ChatMode.AI, finalChat.getChatMode());
        assertNull(finalChat.getAssignedStaffId());
    }
}

