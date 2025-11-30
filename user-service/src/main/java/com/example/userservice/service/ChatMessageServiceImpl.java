package com.example.userservice.service;

import com.example.userservice.entity.*;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.ChatMessageRepository;
import com.example.userservice.repository.ChatParticipantRepository;
import com.example.userservice.repository.ChatRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.ChatMessageRequest;
import com.example.userservice.response.ChatMessageResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.service.inteface.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final UserRepository userRepository;

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
        
        // Note: AI chat is now handled directly via API Gateway -> AI Service
        // No need to call AI service from here anymore
        
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

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
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
        // Find the user by email to get the actual user ID
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return user.getId();
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
