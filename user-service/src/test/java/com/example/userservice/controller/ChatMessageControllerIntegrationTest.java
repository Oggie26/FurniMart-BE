package com.example.userservice.controller;

import com.example.userservice.entity.*;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.feign.AiServiceClient;
import com.example.userservice.repository.*;
import com.example.userservice.request.ChatMessageRequest;
import com.example.userservice.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.kafka.enabled=false",
    "app.kafka.enabled=false"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ChatMessageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiServiceClient aiServiceClient;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private Chat testChat;
    private User testCustomer;
    private User aiBotUser;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create AI bot user
        Account aiBotAccount = Account.builder()
                .email("ai-bot@furnimart.com")
                .password("password")
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .build();
        aiBotAccount = accountRepository.save(aiBotAccount);

        aiBotUser = User.builder()
                .fullName("AI Assistant")
                .account(aiBotAccount)
                .status(EnumStatus.ACTIVE)
                .build();
        aiBotUser = userRepository.save(aiBotUser);

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

    @Test
    @Transactional
    void testSendMessage_AI_Chat_Success() throws Exception {
        // Mock AI service response
        when(aiServiceClient.chat(any(AiServiceClient.ChatRequest.class)))
                .thenReturn(ApiResponse.<String>builder()
                        .status(200)
                        .message("AI phản hồi thành công")
                        .data("Xin chào! Tôi có thể giúp gì cho bạn?")
                        .build());

        ChatMessageRequest request = ChatMessageRequest.builder()
                .chatId(testChat.getId())
                .content("Xin chào")
                .type(ChatMessage.MessageType.TEXT)
                .build();

        // Send message
        mockMvc.perform(post("/api/chat-messages")
                        .with(withUser(testCustomer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Message sent successfully"))
                .andExpect(jsonPath("$.data.content").value("Xin chào"))
                .andExpect(jsonPath("$.data.type").value("TEXT"));

        // Verify AI service was called
        // Note: In integration test, we can't easily verify mock calls
        // But we can verify that messages were saved
        List<ChatMessage> messages = chatMessageRepository.findMessagesByChatId(testChat.getId());
        assertEquals(2, messages.size(), "Should have customer message and AI response");
        
        // Check customer message
        ChatMessage customerMessage = messages.stream()
                .filter(m -> m.getSender().getId().equals(testCustomer.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(customerMessage, "Customer message should exist");
        assertEquals("Xin chào", customerMessage.getContent());

        // Check AI response
        ChatMessage aiMessage = messages.stream()
                .filter(m -> m.getSender().getId().equals(aiBotUser.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(aiMessage, "AI response should exist");
        assertEquals(ChatMessage.MessageType.SYSTEM, aiMessage.getType());
    }

    @Test
    @Transactional
    void testSendMessage_AI_Service_Failure() throws Exception {
        // Mock AI service to throw exception
        when(aiServiceClient.chat(any(AiServiceClient.ChatRequest.class)))
                .thenThrow(new RuntimeException("AI service unavailable"));

        ChatMessageRequest request = ChatMessageRequest.builder()
                .chatId(testChat.getId())
                .content("Test message")
                .type(ChatMessage.MessageType.TEXT)
                .build();

        // Should still succeed (customer message saved, error message saved)
        mockMvc.perform(post("/api/chat-messages")
                        .with(withUser(testCustomer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201));

        // Verify customer message was saved
        List<ChatMessage> messages = chatMessageRepository.findMessagesByChatId(testChat.getId());
        assertTrue(messages.size() >= 1, "Customer message should be saved even if AI fails");
        
        ChatMessage customerMessage = messages.stream()
                .filter(m -> m.getSender().getId().equals(testCustomer.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(customerMessage, "Customer message should exist");
        assertEquals("Test message", customerMessage.getContent());
    }

    @Test
    @Transactional
    void testSendMessage_ChatNotFound() throws Exception {
        ChatMessageRequest request = ChatMessageRequest.builder()
                .chatId("non-existent-chat-id")
                .content("Test")
                .type(ChatMessage.MessageType.TEXT)
                .build();

        mockMvc.perform(post("/api/chat-messages")
                        .with(withUser(testCustomer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(1216)) // CHAT_NOT_FOUND error code
                .andExpect(jsonPath("$.message").value("Chat not found"));
    }

    @Test
    @Transactional
    void testGetChatMessages() throws Exception {
        // Create some test messages
        ChatMessage message1 = ChatMessage.builder()
                .content("Message 1")
                .type(ChatMessage.MessageType.TEXT)
                .status(EnumStatus.ACTIVE)
                .chat(testChat)
                .sender(testCustomer)
                .isEdited(false)
                .isDeleted(false)
                .build();
        chatMessageRepository.save(message1);

        ChatMessage message2 = ChatMessage.builder()
                .content("Message 2")
                .type(ChatMessage.MessageType.TEXT)
                .status(EnumStatus.ACTIVE)
                .chat(testChat)
                .sender(testCustomer)
                .isEdited(false)
                .isDeleted(false)
                .build();
        chatMessageRepository.save(message2);

        // Get messages
        mockMvc.perform(get("/api/chat-messages/chat/{chatId}", testChat.getId())
                        .with(withUser(testCustomer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}

