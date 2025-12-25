package com.example.userservice.service;

import com.example.userservice.entity.*;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.feign.AiServiceClient;
import com.example.userservice.repository.ChatMessageRepository;
import com.example.userservice.repository.ChatParticipantRepository;
import com.example.userservice.repository.ChatRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.request.ChatMessageRequest;
import com.example.userservice.response.ChatMessageResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.WebSocketMessage;
import com.example.userservice.service.inteface.ChatMessageService;
import com.example.userservice.websocket.ChatWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AiServiceClient aiServiceClient;
    private final ChatWebSocketHandler chatWebSocketHandler;

    private static final String AI_ASSISTANT_EMAIL = "ai-assistant@furnimart.com";

    public ChatMessageServiceImpl(
            ChatMessageRepository chatMessageRepository,
            ChatRepository chatRepository,
            ChatParticipantRepository chatParticipantRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            AiServiceClient aiServiceClient,
            @Lazy ChatWebSocketHandler chatWebSocketHandler) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatRepository = chatRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.aiServiceClient = aiServiceClient;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest messageRequest) {
        String currentUserId = getCurrentUserId();
        User sender = userRepository.findByIdAndIsDeletedFalse(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Chat chat = chatRepository.findById(messageRequest.getChatId())
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_NOT_FOUND));

        // Check if user is a participant
        chatParticipantRepository.findActiveParticipantByChatIdAndUserId(messageRequest.getChatId(), currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        // Validate chatMode - block customer from sending messages in WAITING_STAFF mode
        Chat.ChatMode chatMode = chat.getChatMode() != null ? chat.getChatMode() : Chat.ChatMode.AI;
        if (chatMode == Chat.ChatMode.WAITING_STAFF) {
            // Only staff can send messages in WAITING_STAFF mode
            Account senderAccount = sender.getAccount();
            if (senderAccount == null || senderAccount.getRole() != EnumRole.STAFF) {
                throw new AppException(ErrorCode.INVALID_CHAT_STATE);
            }
        }

        ChatMessage replyTo = null;
        if (messageRequest.getReplyToMessageId() != null) {
            replyTo = chatMessageRepository.findById(messageRequest.getReplyToMessageId())
                    .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
        }

        ChatMessage message = ChatMessage.builder()
                .content(messageRequest.getContent())
                .type(messageRequest.getType())
                .status(EnumStatus.ACTIVE)
                .chat(chat)
                .sender(sender)
                .replyTo(replyTo)
                .attachmentUrl(messageRequest.getAttachmentUrl())
                .attachmentType(messageRequest.getAttachmentType())
                .isEdited(false)
                .isDeleted(false)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // If chat mode is AI, trigger async AI response
        if (chatMode == Chat.ChatMode.AI) {
            try {
                processAIResponse(chat.getId(), savedMessage.getId(), chat);
            } catch (Exception e) {
                log.error("Error triggering AI response for chat: {}", chat.getId(), e);
                // Don't throw - user message is already saved
            }
        }
        
        return toChatMessageResponse(savedMessage);
    }
    

    @Override
    public List<ChatMessageResponse> getChatMessages(String chatId) {
        // Check if user is a participant
        String currentUserId = getCurrentUserId();
        chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        List<ChatMessage> messages = chatMessageRepository.findMessagesByChatId(chatId);
        return messages.stream()
                .map(this::toChatMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<ChatMessageResponse> getChatMessagesWithPagination(String chatId, int page, int size) {
        // Check if user is a participant
        String currentUserId = getCurrentUserId();
        chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<ChatMessage> messagePage = chatMessageRepository.findMessagesByChatId(chatId, pageable);

        List<ChatMessageResponse> messageResponses = messagePage.getContent().stream()
                .map(this::toChatMessageResponse)
                .collect(Collectors.toList());

        return PageResponse.<ChatMessageResponse>builder()
                .content(messageResponses)
                .totalElements(messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .size(messagePage.getSize())
                .number(messagePage.getNumber())
                .first(messagePage.isFirst())
                .last(messagePage.isLast())
                .build();
    }

    @Override
    public ChatMessageResponse getMessageById(String messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // Check if user is a participant in the chat
        String currentUserId = getCurrentUserId();
        chatParticipantRepository.findActiveParticipantByChatIdAndUserId(message.getChat().getId(), currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        return toChatMessageResponse(message);
    }

    @Override
    @Transactional
    public ChatMessageResponse editMessage(String messageId, String newContent) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        String currentUserId = getCurrentUserId();
        
        // Only sender can edit their message
        if (!message.getSender().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        message.setContent(newContent);
        message.setIsEdited(true);
        
        ChatMessage updatedMessage = chatMessageRepository.save(message);
        return toChatMessageResponse(updatedMessage);
    }

    @Override
    @Transactional
    public void deleteMessage(String messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        String currentUserId = getCurrentUserId();
        
        // Only sender can delete their message
        if (!message.getSender().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        message.setIsDeleted(true);
        chatMessageRepository.save(message);
    }

    @Override
    public List<ChatMessageResponse> searchMessagesInChat(String chatId, String searchTerm) {
        // Check if user is a participant
        String currentUserId = getCurrentUserId();
        chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        List<ChatMessage> messages = chatMessageRepository.searchMessagesInChat(chatId, searchTerm, PageRequest.of(0, 100)).getContent();
        return messages.stream()
                .map(this::toChatMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<ChatMessageResponse> searchMessagesInChatWithPagination(String chatId, String searchTerm, int page, int size) {
        // Check if user is a participant
        String currentUserId = getCurrentUserId();
        chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatMessage> messagePage = chatMessageRepository.searchMessagesInChat(chatId, searchTerm, pageable);

        List<ChatMessageResponse> messageResponses = messagePage.getContent().stream()
                .map(this::toChatMessageResponse)
                .collect(Collectors.toList());

        return PageResponse.<ChatMessageResponse>builder()
                .content(messageResponses)
                .totalElements(messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .size(messagePage.getSize())
                .number(messagePage.getNumber())
                .first(messagePage.isFirst())
                .last(messagePage.isLast())
                .build();
    }

    @Override
    public List<ChatMessageResponse> getMessageReplies(String messageId) {
        ChatMessage parentMessage = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // Check if user is a participant in the chat
        String currentUserId = getCurrentUserId();
        chatParticipantRepository.findActiveParticipantByChatIdAndUserId(parentMessage.getChat().getId(), currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        List<ChatMessage> replies = chatMessageRepository.findRepliesToMessage(messageId);
        return replies.stream()
                .map(this::toChatMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markMessageAsRead(String messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        String currentUserId = getCurrentUserId();
        ChatParticipant participant = chatParticipantRepository.findActiveParticipantByChatIdAndUserId(message.getChat().getId(), currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        // Update last read time
        participant.setLastReadAt(java.time.LocalDateTime.now());
        chatParticipantRepository.save(participant);
    }

    @Override
    public List<ChatMessageResponse> getUnreadMessages(String chatId) {
        String currentUserId = getCurrentUserId();
        ChatParticipant participant = chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        List<ChatMessage> unreadMessages = chatMessageRepository.findNewMessagesSince(chatId, participant.getLastReadAt());
        return unreadMessages.stream()
                .map(this::toChatMessageResponse)
                .collect(Collectors.toList());
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        String email = authentication.getName(); // This returns the email
        // Find the account by email
        Account account = accountRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        
        // Check if account has User (Customer)
        if (account.getUser() != null) {
            return account.getUser().getId();
        }
        
        // Check if account has Employee (Staff/Admin)
        if (account.getEmployee() != null) {
            return account.getEmployee().getId();
        }
        
        // If neither exists, throw exception
        throw new AppException(ErrorCode.USER_NOT_FOUND);
    }

    @Async
    public void processAIResponse(String chatId, String userMessageId, Chat chat) {
        try {
            log.info("Processing AI response for chat: {}, message: {}", chatId, userMessageId);

            // Get the user message
            ChatMessage userMessage = chatMessageRepository.findById(userMessageId)
                    .orElseThrow(() -> new RuntimeException("User message not found: " + userMessageId));

            // Get recent message history (5-10 messages, excluding the current one)
            List<ChatMessage> recentMessages = getRecentChatMessages(chatId, 10, userMessageId);

            // Build message history for AI
            List<AiServiceClient.ChatRequest.MessageHistoryItem> messageHistory = recentMessages.stream()
                    .map(msg -> {
                        String role = isAIMessage(msg) ? "assistant" : "user";
                        return new AiServiceClient.ChatRequest.MessageHistoryItem(role, msg.getContent());
                    })
                    .collect(Collectors.toList());

            // Call AI service
            AiServiceClient.ChatRequest aiRequest = new AiServiceClient.ChatRequest(
                    chatId,
                    userMessage.getContent(),
                    messageHistory
            );

            com.example.userservice.response.ApiResponse<AiServiceClient.ChatResponse> aiResponse = 
                    aiServiceClient.chat(aiRequest);

            if (aiResponse != null && aiResponse.getData() != null && aiResponse.getData().getResponse() != null) {
                String aiResponseText = aiResponse.getData().getResponse();

                // Create and save AI message
                ChatMessage aiMessage = createAIMessage(chatId, aiResponseText, chat);
                ChatMessage savedAIMessage = chatMessageRepository.save(aiMessage);

                log.info("AI response saved for chat: {}, message: {}", chatId, savedAIMessage.getId());

                // Broadcast AI response via WebSocket
                try {
                    WebSocketMessage wsMessage = WebSocketMessage.builder()
                            .type("MESSAGE")
                            .chatId(chatId)
                            .senderId(savedAIMessage.getSender().getId())
                            .content(savedAIMessage.getContent())
                            .messageType(savedAIMessage.getType())
                            .timestamp(savedAIMessage.getCreatedAt() != null ? 
                                    savedAIMessage.getCreatedAt().getTime() : System.currentTimeMillis())
                            .build();

                    chatWebSocketHandler.broadcastToChat(chatId, wsMessage);
                } catch (Exception e) {
                    log.error("Error broadcasting AI response via WebSocket", e);
                }
            } else {
                log.warn("AI service returned null or empty response for chat: {}", chatId);
            }

        } catch (Exception e) {
            log.error("Error processing AI response for chat: {}", chatId, e);
            // Don't throw - this is async, we don't want to affect the main flow
        }
    }

    private List<ChatMessage> getRecentChatMessages(String chatId, int limit, String excludeMessageId) {
        Pageable pageable = PageRequest.of(0, limit + 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatMessage> messagePage = chatMessageRepository.findMessagesByChatId(chatId, pageable);
        
        return messagePage.getContent().stream()
                .filter(msg -> !msg.getId().equals(excludeMessageId))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private ChatMessage createAIMessage(String chatId, String content, Chat chat) {
        User aiUser = getOrCreateAIUser();

        return ChatMessage.builder()
                .content(content)
                .type(ChatMessage.MessageType.TEXT)
                .status(EnumStatus.ACTIVE)
                .chat(chat)
                .sender(aiUser)
                .replyTo(null)
                .attachmentUrl(null)
                .attachmentType(null)
                .isEdited(false)
                .isDeleted(false)
                .build();
    }

    private User getOrCreateAIUser() {
        // Try to find existing AI user by email
        Account aiAccount = accountRepository.findByEmailAndIsDeletedFalse(AI_ASSISTANT_EMAIL).orElse(null);

        if (aiAccount != null) {
            User aiUser = userRepository.findByAccountIdAndIsDeletedFalse(aiAccount.getId()).orElse(null);
            if (aiUser != null) {
                return aiUser;
            }
        }

        // Create AI user if not exists
        log.info("Creating AI Assistant user");
        
        Account newAccount = Account.builder()
                .email(AI_ASSISTANT_EMAIL)
                .password("$2a$10$dummy") // Dummy password, AI user won't login
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .isDeleted(false)
                .build();
        Account savedAccount = accountRepository.save(newAccount);

        User newUser = User.builder()
                .fullName("AI Assistant")
                .phone("0000000000")
                .status(EnumStatus.ACTIVE)
                .account(savedAccount)
                .build();
        newUser.setIsDeleted(false);
        
        return userRepository.save(newUser);
    }

    private boolean isAIMessage(ChatMessage message) {
        if (message.getSender() == null || message.getSender().getAccount() == null) {
            return false;
        }
        return AI_ASSISTANT_EMAIL.equals(message.getSender().getAccount().getEmail());
    }

    private ChatMessageResponse toChatMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .type(message.getType())
                .status(message.getStatus())
                .chatId(message.getChat().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .senderAvatar(message.getSender().getAvatar())
                .replyToMessageId(message.getReplyTo() != null ? message.getReplyTo().getId() : null)
                .replyToContent(message.getReplyTo() != null ? message.getReplyTo().getContent() : null)
                .attachmentUrl(message.getAttachmentUrl())
                .attachmentType(message.getAttachmentType())
                .isEdited(message.getIsEdited())
                .isDeleted(message.getIsDeleted())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}
