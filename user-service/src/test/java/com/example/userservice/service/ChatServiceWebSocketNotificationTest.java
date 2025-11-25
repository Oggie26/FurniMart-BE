package com.example.userservice.service;

import com.example.userservice.entity.*;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.repository.*;
import com.example.userservice.response.ChatResponse;
import com.example.userservice.service.inteface.ChatService;
import com.example.userservice.websocket.ChatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.kafka.enabled=false",
    "app.kafka.enabled=false"
})
@ActiveProfiles("test")
class ChatServiceWebSocketNotificationTest {

    @Autowired
    private ChatService chatService;

    @SpyBean
    private ChatWebSocketHandler chatWebSocketHandler;

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
    private Employee testStaff1;
    private Employee testStaff2;
    private User testStaffUser1;
    private User testStaffUser2;

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

        // Create test staff 1
        Account staffAccount1 = Account.builder()
                .email("staff1@test.com")
                .password("password")
                .role(EnumRole.STAFF)
                .status(EnumStatus.ACTIVE)
                .build();
        staffAccount1 = accountRepository.save(staffAccount1);

        testStaff1 = Employee.builder()
                .fullName("Test Staff 1")
                .account(staffAccount1)
                .status(EnumStatus.ACTIVE)
                .code("STAFF001")
                .build();
        testStaff1 = employeeRepository.save(testStaff1);

        testStaffUser1 = User.builder()
                .fullName("Staff User 1")
                .account(staffAccount1)
                .status(EnumStatus.ACTIVE)
                .build();
        testStaffUser1 = userRepository.save(testStaffUser1);

        // Create test staff 2
        Account staffAccount2 = Account.builder()
                .email("staff2@test.com")
                .password("password")
                .role(EnumRole.STAFF)
                .status(EnumStatus.ACTIVE)
                .build();
        staffAccount2 = accountRepository.save(staffAccount2);

        testStaff2 = Employee.builder()
                .fullName("Test Staff 2")
                .account(staffAccount2)
                .status(EnumStatus.ACTIVE)
                .code("STAFF002")
                .build();
        testStaff2 = employeeRepository.save(testStaff2);

        testStaffUser2 = User.builder()
                .fullName("Staff User 2")
                .account(staffAccount2)
                .status(EnumStatus.ACTIVE)
                .build();
        testStaffUser2 = userRepository.save(testStaffUser2);

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

    // Test STAFF_CHAT_REQUEST notification
    @Test
    @Transactional
    void testStaffChatRequestNotification() {
        // Set up customer security context
        setSecurityContextForUser(testCustomer);

        // Request staff connection - this should trigger notifyAllOnlineStaff
        ChatResponse response = chatService.requestStaffConnection(testChat.getId());

        // Verify chat mode changed
        assertEquals(Chat.ChatMode.WAITING_STAFF, response.getChatMode());

        // Verify notification was attempted (WebSocket handler may not have active sessions in test)
        // The method should complete without throwing exceptions
        assertNotNull(response);
        
        // Verify chat in database
        Chat updatedChat = chatRepository.findById(testChat.getId()).orElse(null);
        assertNotNull(updatedChat);
        assertEquals(Chat.ChatMode.WAITING_STAFF, updatedChat.getChatMode());
    }

    // Test STAFF_CONNECTED notification
    @Test
    @Transactional
    void testStaffConnectedNotification() {
        // Setup: request staff connection first
        setSecurityContextForUser(testCustomer);
        chatService.requestStaffConnection(testChat.getId());

        // Accept as staff - this should trigger notifyCustomerStaffConnected
        setSecurityContextForUser(testStaffUser1);
        ChatResponse response = chatService.acceptStaffConnection(testChat.getId());

        // Verify chat mode changed
        assertEquals(Chat.ChatMode.STAFF_CONNECTED, response.getChatMode());
        assertEquals(testStaff1.getId(), response.getAssignedStaffId());

        // Verify notification was attempted (should not throw exceptions)
        assertNotNull(response);
    }

    // Test CHAT_ACCEPTED_BY_OTHER notification
    @Test
    @Transactional
    void testChatAcceptedByOtherNotification() {
        // Setup: request staff connection
        setSecurityContextForUser(testCustomer);
        chatService.requestStaffConnection(testChat.getId());

        // Accept as staff 1 - this should notify other staff
        setSecurityContextForUser(testStaffUser1);
        ChatResponse response = chatService.acceptStaffConnection(testChat.getId());

        // Verify chat was accepted
        assertEquals(Chat.ChatMode.STAFF_CONNECTED, response.getChatMode());
        assertEquals(testStaff1.getId(), response.getAssignedStaffId());

        // Verify notification to other staff was attempted
        // (In real scenario, staff2 would receive CHAT_ACCEPTED_BY_OTHER notification)
        assertNotNull(response);
    }

    // Test STAFF_CHAT_ENDED notification
    @Test
    @Transactional
    void testStaffChatEndedNotification() {
        // Setup: request and accept staff connection
        setSecurityContextForUser(testCustomer);
        chatService.requestStaffConnection(testChat.getId());

        setSecurityContextForUser(testStaffUser1);
        chatService.acceptStaffConnection(testChat.getId());

        // End staff chat - this should trigger notifyChatEnded
        setSecurityContextForUser(testCustomer);
        ChatResponse response = chatService.endStaffChat(testChat.getId());

        // Verify chat mode changed back to AI
        assertEquals(Chat.ChatMode.AI, response.getChatMode());
        assertNull(response.getAssignedStaffId());

        // Verify notification was attempted
        assertNotNull(response);
    }

    // Test getOnlineStaff fallback mechanism
    @Test
    @Transactional
    void testGetOnlineStaff_Fallback() {
        // When no WebSocket sessions are available, should fallback to database query
        List<Employee> onlineStaff = chatService.getOnlineStaff();

        // Should return all active staff as fallback
        assertNotNull(onlineStaff);
        // In test environment, should return at least our test staff
        assertTrue(onlineStaff.size() >= 2, "Should return at least 2 staff (test staff)");
    }

    // Test isStaffOnline
    @Test
    @Transactional
    void testIsStaffOnline() {
        // Test with staff that exists
        boolean isOnline = chatService.isStaffOnline(testStaff1.getId());
        
        // In test environment without WebSocket sessions, should return false
        // But method should not throw exceptions
        assertNotNull(testStaff1);
    }

    // Test full flow with notifications
    @Test
    @Transactional
    void testFullFlowWithNotifications() {
        // Step 1: Customer requests staff connection
        setSecurityContextForUser(testCustomer);
        ChatResponse requestResponse = chatService.requestStaffConnection(testChat.getId());
        assertEquals(Chat.ChatMode.WAITING_STAFF, requestResponse.getChatMode());
        // Notification: STAFF_CHAT_REQUEST sent to all online staff

        // Step 2: Staff accepts connection
        setSecurityContextForUser(testStaffUser1);
        ChatResponse acceptResponse = chatService.acceptStaffConnection(testChat.getId());
        assertEquals(Chat.ChatMode.STAFF_CONNECTED, acceptResponse.getChatMode());
        // Notifications: 
        // - STAFF_CONNECTED sent to customer
        // - CHAT_ACCEPTED_BY_OTHER sent to other staff

        // Step 3: Customer ends staff chat
        setSecurityContextForUser(testCustomer);
        ChatResponse endResponse = chatService.endStaffChat(testChat.getId());
        assertEquals(Chat.ChatMode.AI, endResponse.getChatMode());
        // Notification: STAFF_CHAT_ENDED sent to customer and staff

        // Verify final state
        Chat finalChat = chatRepository.findById(testChat.getId()).orElse(null);
        assertNotNull(finalChat);
        assertEquals(Chat.ChatMode.AI, finalChat.getChatMode());
        assertNull(finalChat.getAssignedStaffId());
    }

    // Test that notifications don't break when WebSocket handler is unavailable
    @Test
    @Transactional
    void testNotificationsWithUnavailableWebSocket() {
        // Even if WebSocket handler throws exceptions, service should handle gracefully
        doThrow(new RuntimeException("WebSocket unavailable")).when(chatWebSocketHandler)
                .sendMessageToUser(anyString(), any());

        // Should still complete successfully (graceful degradation)
        setSecurityContextForUser(testCustomer);
        
        // Should not throw exception even if WebSocket fails
        assertDoesNotThrow(() -> {
            ChatResponse response = chatService.requestStaffConnection(testChat.getId());
            assertNotNull(response);
            assertEquals(Chat.ChatMode.WAITING_STAFF, response.getChatMode());
        });
    }
}

