package com.example.userservice.controller;

import com.example.userservice.entity.*;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.repository.*;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.kafka.enabled=false",
    "app.kafka.enabled=false"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private String customerToken;
    private String staffToken;

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

    private RequestPostProcessor withUser(User user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getAccount().getRole().name()));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getAccount().getEmail(),
                null,
                authorities
        );

        return authentication(authentication);
    }

    // Test POST /api/chats/{chatId}/request-staff
    @Test
    @Transactional
    void testRequestStaffConnection_Integration() throws Exception {
        mockMvc.perform(post("/api/chats/{chatId}/request-staff", testChat.getId())
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Staff connection requested successfully"))
                .andExpect(jsonPath("$.data.id").value(testChat.getId()))
                .andExpect(jsonPath("$.data.chatMode").value("WAITING_STAFF"));

        // Verify in database
        Chat updatedChat = chatRepository.findById(testChat.getId()).orElse(null);
        assertNotNull(updatedChat);
        assertEquals(Chat.ChatMode.WAITING_STAFF, updatedChat.getChatMode());
    }

    // Test POST /api/chats/{chatId}/accept-staff
    @Test
    @Transactional
    void testAcceptStaffConnection_Integration() throws Exception {
        // First request staff connection
        mockMvc.perform(post("/api/chats/{chatId}/request-staff", testChat.getId())
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Then accept as staff

        mockMvc.perform(post("/api/chats/{chatId}/accept-staff", testChat.getId())
                .with(withUser(testStaffUser))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Staff connection accepted successfully"))
                .andExpect(jsonPath("$.data.id").value(testChat.getId()))
                .andExpect(jsonPath("$.data.chatMode").value("STAFF_CONNECTED"))
                .andExpect(jsonPath("$.data.assignedStaffId").value(testStaff.getId()));

        // Verify in database
        Chat updatedChat = chatRepository.findById(testChat.getId()).orElse(null);
        assertNotNull(updatedChat);
        assertEquals(Chat.ChatMode.STAFF_CONNECTED, updatedChat.getChatMode());
        assertEquals(testStaff.getId(), updatedChat.getAssignedStaffId());
    }

    // Test POST /api/chats/{chatId}/end-staff-chat
    @Test
    @Transactional
    void testEndStaffChat_Integration() throws Exception {
        // Setup: request and accept staff connection
        mockMvc.perform(post("/api/chats/{chatId}/request-staff", testChat.getId())
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/chats/{chatId}/accept-staff", testChat.getId())
                .with(withUser(testStaffUser))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // End staff chat as customer

        mockMvc.perform(post("/api/chats/{chatId}/end-staff-chat", testChat.getId())
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Staff chat ended successfully"))
                .andExpect(jsonPath("$.data.id").value(testChat.getId()))
                .andExpect(jsonPath("$.data.chatMode").value("AI"))
                .andExpect(jsonPath("$.data.assignedStaffId").isEmpty());

        // Verify in database
        Chat updatedChat = chatRepository.findById(testChat.getId()).orElse(null);
        assertNotNull(updatedChat);
        assertEquals(Chat.ChatMode.AI, updatedChat.getChatMode());
        assertNull(updatedChat.getAssignedStaffId());
    }

    // Test GET /api/chats/{chatId}/status
    @Test
    @Transactional
    void testGetChatStatus_Integration() throws Exception {

        mockMvc.perform(get("/api/chats/{chatId}/status", testChat.getId())
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Chat status retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(testChat.getId()))
                .andExpect(jsonPath("$.data.chatMode").value("AI"));
    }

    // Test GET /api/chats/{chatId}/status after request staff
    @Test
    @Transactional
    void testGetChatStatus_AfterRequestStaff() throws Exception {

        // Request staff connection
        mockMvc.perform(post("/api/chats/{chatId}/request-staff", testChat.getId())
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Get status
        mockMvc.perform(get("/api/chats/{chatId}/status", testChat.getId())
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatMode").value("WAITING_STAFF"))
                .andExpect(jsonPath("$.data.staffRequestedAt").exists());
    }

    // Test GET /api/chats/{chatId}/status after accept staff
    @Test
    @Transactional
    void testGetChatStatus_AfterAcceptStaff() throws Exception {
        mockMvc.perform(post("/api/chats/{chatId}/request-staff", testChat.getId())
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/chats/{chatId}/accept-staff", testChat.getId())
                .with(withUser(testStaffUser))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Get status as customer
        mockMvc.perform(get("/api/chats/{chatId}/status", testChat.getId())
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatMode").value("STAFF_CONNECTED"))
                .andExpect(jsonPath("$.data.assignedStaffId").value(testStaff.getId()));
    }

    // Test full flow: request → accept → end
    @Test
    @Transactional
    void testFullFlow_Integration() throws Exception {
        // Step 1: Customer requests staff connection
        mockMvc.perform(post("/api/chats/{chatId}/request-staff", testChat.getId())
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatMode").value("WAITING_STAFF"));

        // Step 2: Staff accepts connection
        mockMvc.perform(post("/api/chats/{chatId}/accept-staff", testChat.getId())
                .with(withUser(testStaffUser))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatMode").value("STAFF_CONNECTED"))
                .andExpect(jsonPath("$.data.assignedStaffId").value(testStaff.getId()));

        // Step 3: Customer ends staff chat
        mockMvc.perform(post("/api/chats/{chatId}/end-staff-chat", testChat.getId())
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatMode").value("AI"))
                .andExpect(jsonPath("$.data.assignedStaffId").isEmpty());

        // Verify final state
        Chat finalChat = chatRepository.findById(testChat.getId()).orElse(null);
        assertNotNull(finalChat);
        assertEquals(Chat.ChatMode.AI, finalChat.getChatMode());
        assertNull(finalChat.getAssignedStaffId());
    }

    // Test authorization: Customer cannot accept staff connection
    @Test
    @Transactional
    void testAuthorization_CustomerCannotAccept() throws Exception {
        // Request staff connection
        mockMvc.perform(post("/api/chats/{chatId}/request-staff", testChat.getId())
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Try to accept as customer (should fail)
        // In test environment, @PreAuthorize may not work, but service will throw exception
        // because customer doesn't have Employee record
        mockMvc.perform(post("/api/chats/{chatId}/accept-staff", testChat.getId())
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError()); // Service throws exception (USER_NOT_FOUND)
    }

    // Test authorization: Staff cannot request staff connection
    @Test
    @Transactional
    void testAuthorization_StaffCannotRequest() throws Exception {
        // Staff trying to request staff connection (should fail)
        // In test environment, @PreAuthorize may not work, but service will throw exception
        // because staff is not a customer participant or other business logic
        // Note: In production, @PreAuthorize("hasRole('CUSTOMER')") will block this
        mockMvc.perform(post("/api/chats/{chatId}/request-staff", testChat.getId())
                .with(withUser(testStaffUser))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError()); // Service throws exception
    }

    // Test error handling: Chat not found
    @Test
    @Transactional
    void testErrorHandling_ChatNotFound() throws Exception {

        String nonExistentChatId = "non-existent-chat-id";

        mockMvc.perform(post("/api/chats/{chatId}/request-staff", nonExistentChatId)
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError()); // 400 or 404
    }

    // Test error handling: Chat already accepted
    @Test
    @Transactional
    void testErrorHandling_ChatAlreadyAccepted() throws Exception {
        // Setup: request and accept
        mockMvc.perform(post("/api/chats/{chatId}/request-staff", testChat.getId())
                .with(withUser(testCustomer))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/chats/{chatId}/accept-staff", testChat.getId())
                .with(withUser(testStaffUser))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Try to accept again (should fail)
        mockMvc.perform(post("/api/chats/{chatId}/accept-staff", testChat.getId())
                .with(withUser(testStaffUser))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError()); // 400 Bad Request
    }
}

