package com.example.userservice.service;

import com.example.userservice.entity.*;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.feign.AiServiceClient;
import com.example.userservice.repository.*;
import com.example.userservice.request.ChatMessageRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.ChatMessageResponse;
import com.example.userservice.service.inteface.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.kafka.enabled=false",
    "app.kafka.enabled=false"
})
@ActiveProfiles("test")
class ChatMessageServiceAiFailureTest {

    @Autowired
    private ChatMessageService chatMessageService;

    @MockBean
    private AiServiceClient aiServiceClient;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    @Autowired
    private AccountRepository accountRepository;

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

        // Set up SecurityContext for test customer
        setSecurityContextForUser(testCustomer);
    }

    private void setSecurityContextForUser(User user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getAccount().getRole().name()));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getAccount().getEmail(), // principal (email, not accountId)
                null, // credentials
                authorities
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @Transactional
    void testAiServiceFailure_CustomerMessageStillSaved() {
        // Mock AI service to throw exception
        when(aiServiceClient.chat(any(AiServiceClient.ChatRequest.class)))
                .thenThrow(new RuntimeException("AI service unavailable"));

        // Create message request
        ChatMessageRequest request = ChatMessageRequest.builder()
                .chatId(testChat.getId())
                .content("Test message")
                .type(ChatMessage.MessageType.TEXT)
                .build();

        // Send message (should handle AI service failure gracefully)
        ChatMessageResponse response = chatMessageService.sendMessage(request);

        // Verify customer message was saved
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("Test message", response.getContent());
        assertEquals(testCustomer.getId(), response.getSenderId());

        // Verify message exists in database
        ChatMessage savedMessage = chatMessageRepository.findById(response.getId()).orElse(null);
        assertNotNull(savedMessage);
        assertEquals("Test message", savedMessage.getContent());
        assertEquals(testCustomer.getId(), savedMessage.getSender().getId());

        // Verify AI service was called
        verify(aiServiceClient, times(1)).chat(any(AiServiceClient.ChatRequest.class));

        // Verify error message was saved (check for SYSTEM message from AI bot)
        List<ChatMessage> messages = chatMessageRepository.findMessagesByChatId(testChat.getId());
        boolean hasErrorMessage = messages.stream()
                .anyMatch(m -> m.getType() == ChatMessage.MessageType.SYSTEM 
                        && m.getSender().getId().equals(aiBotUser.getId())
                        && m.getContent().contains("Xin lỗi"));
        
        assertTrue(hasErrorMessage, "Error message should be saved when AI service fails");
    }

    @Test
    @Transactional
    void testAiServiceFailure_ErrorHandling() {
        // Mock AI service to return null or error response
        when(aiServiceClient.chat(any(AiServiceClient.ChatRequest.class)))
                .thenReturn(ApiResponse.<String>builder()
                        .status(500)
                        .message("AI service error")
                        .data(null)
                        .build());

        ChatMessageRequest request = ChatMessageRequest.builder()
                .chatId(testChat.getId())
                .content("Test message")
                .type(ChatMessage.MessageType.TEXT)
                .build();

        // Should not throw exception, should handle gracefully
        assertDoesNotThrow(() -> {
            ChatMessageResponse response = chatMessageService.sendMessage(request);
            assertNotNull(response);
        });

        // Verify customer message was saved
        List<ChatMessage> messages = chatMessageRepository.findMessagesByChatId(testChat.getId());
        assertTrue(messages.stream()
                .anyMatch(m -> m.getContent().equals("Test message")));
    }

    @Test
    @Transactional
    void testAiServiceFailure_Timeout() {
        // Mock AI service to timeout
        when(aiServiceClient.chat(any(AiServiceClient.ChatRequest.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(15000); // Simulate timeout (longer than configured timeout)
                    return ApiResponse.<String>builder()
                            .status(200)
                            .data("Response")
                            .build();
                });

        ChatMessageRequest request = ChatMessageRequest.builder()
                .chatId(testChat.getId())
                .content("Test message")
                .type(ChatMessage.MessageType.TEXT)
                .build();

        // Should handle timeout gracefully
        // Note: Actual timeout behavior depends on Feign configuration
        assertDoesNotThrow(() -> {
            ChatMessageResponse response = chatMessageService.sendMessage(request);
            assertNotNull(response);
        });
    }
}

