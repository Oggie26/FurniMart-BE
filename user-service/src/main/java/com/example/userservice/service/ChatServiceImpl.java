package com.example.userservice.service;

import com.example.userservice.entity.*;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.ChatParticipantRepository;
import com.example.userservice.repository.ChatRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.ChatRequest;
import com.example.userservice.response.ChatMessageResponse;
import com.example.userservice.response.ChatParticipantResponse;
import com.example.userservice.response.ChatResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.service.inteface.ChatService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ChatResponse createChat(ChatRequest chatRequest) {
        String currentUserId = getCurrentUserId();
        User currentUser = userRepository.findByIdAndIsDeletedFalse(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Chat chat = Chat.builder()
                .name(chatRequest.getName())
                .description(chatRequest.getDescription())
                .type(chatRequest.getType())
                .status(EnumStatus.ACTIVE)
                .createdBy(currentUser)
                .build();

        Chat savedChat = chatRepository.save(chat);

        // Add creator as admin
        ChatParticipant creatorParticipant = ChatParticipant.builder()
                .chat(savedChat)
                .user(currentUser)
                .role(ChatParticipant.ParticipantRole.ADMIN)
                .status(EnumStatus.ACTIVE)
                .lastReadAt(LocalDateTime.now())
                .build();
        chatParticipantRepository.save(creatorParticipant);

        // Add other participants
        if (chatRequest.getParticipantIds() != null && !chatRequest.getParticipantIds().isEmpty()) {
            for (String participantId : chatRequest.getParticipantIds()) {
                if (!participantId.equals(currentUserId)) {
                    User participant = userRepository.findByIdAndIsDeletedFalse(participantId)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

                    ChatParticipant chatParticipant = ChatParticipant.builder()
                            .chat(savedChat)
                            .user(participant)
                            .role(ChatParticipant.ParticipantRole.MEMBER)
                            .status(EnumStatus.ACTIVE)
                            .lastReadAt(LocalDateTime.now())
                            .build();
                    chatParticipantRepository.save(chatParticipant);
                }
            }
        }

        return toChatResponse(savedChat);
    }

    @Override
    public ChatResponse getChatById(String chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_NOT_FOUND));

        // Check if current user is a participant
        String currentUserId = getCurrentUserId();
        chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        return toChatResponse(chat);
    }

    @Override
    public List<ChatResponse> getUserChats() {
        String currentUserId = getCurrentUserId();
        List<Chat> chats = chatRepository.findChatsByUserId(currentUserId);
        return chats.stream()
                .map(this::toChatResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<ChatResponse> getUserChatsWithPagination(int page, int size) {
        String currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Chat> chatPage = chatRepository.findChatsByUserId(currentUserId, pageable);

        List<ChatResponse> chatResponses = chatPage.getContent().stream()
                .map(this::toChatResponse)
                .collect(Collectors.toList());

        return PageResponse.<ChatResponse>builder()
                .content(chatResponses)
                .totalElements(chatPage.getTotalElements())
                .totalPages(chatPage.getTotalPages())
                .size(chatPage.getSize())
                .number(chatPage.getNumber())
                .first(chatPage.isFirst())
                .last(chatPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public ChatResponse updateChat(String chatId, ChatRequest chatRequest) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_NOT_FOUND));

        String currentUserId = getCurrentUserId();
        ChatParticipant participant = chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        // Only admin can update chat
        if (participant.getRole() != ChatParticipant.ParticipantRole.ADMIN) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (chatRequest.getName() != null) {
            chat.setName(chatRequest.getName());
        }
        if (chatRequest.getDescription() != null) {
            chat.setDescription(chatRequest.getDescription());
        }

        Chat updatedChat = chatRepository.save(chat);
        return toChatResponse(updatedChat);
    }

    @Override
    @Transactional
    public void deleteChat(String chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_NOT_FOUND));

        String currentUserId = getCurrentUserId();
        ChatParticipant participant = chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        // Only admin can delete chat
        if (participant.getRole() != ChatParticipant.ParticipantRole.ADMIN) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        chat.setStatus(EnumStatus.DELETED);
        chat.setIsDeleted(true);
        chatRepository.save(chat);
    }

    @Override
    @Transactional
    public ChatResponse addParticipant(String chatId, String userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_NOT_FOUND));

        String currentUserId = getCurrentUserId();
        ChatParticipant currentParticipant = chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        // Only admin and moderator can add participants
        if (currentParticipant.getRole() == ChatParticipant.ParticipantRole.MEMBER) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        User newParticipant = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check if user is already a participant
        chatParticipantRepository.findByChatIdAndUserId(chatId, userId)
                .ifPresent(existing -> {
                    if (existing.getStatus() == EnumStatus.ACTIVE) {
                        throw new AppException(ErrorCode.USER_ALREADY_PARTICIPANT);
                    } else {
                        existing.setStatus(EnumStatus.ACTIVE);
                        chatParticipantRepository.save(existing);
                    }
                });

        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chat(chat)
                .user(newParticipant)
                .role(ChatParticipant.ParticipantRole.MEMBER)
                .status(EnumStatus.ACTIVE)
                .lastReadAt(LocalDateTime.now())
                .build();
        chatParticipantRepository.save(chatParticipant);

        return toChatResponse(chat);
    }

    @Override
    @Transactional
    public ChatResponse removeParticipant(String chatId, String userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_NOT_FOUND));

        String currentUserId = getCurrentUserId();
        ChatParticipant currentParticipant = chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        // Only admin and moderator can remove participants
        if (currentParticipant.getRole() == ChatParticipant.ParticipantRole.MEMBER) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        ChatParticipant participantToRemove = chatParticipantRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_PARTICIPANT));

        participantToRemove.setStatus(EnumStatus.INACTIVE);
        chatParticipantRepository.save(participantToRemove);

        return toChatResponse(chat);
    }

    @Override
    @Transactional
    public ChatResponse updateParticipantRole(String chatId, String userId, String role) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_NOT_FOUND));

        String currentUserId = getCurrentUserId();
        ChatParticipant currentParticipant = chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        // Only admin can update roles
        if (currentParticipant.getRole() != ChatParticipant.ParticipantRole.ADMIN) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        ChatParticipant participant = chatParticipantRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_PARTICIPANT));

        participant.setRole(ChatParticipant.ParticipantRole.valueOf(role.toUpperCase()));
        chatParticipantRepository.save(participant);

        return toChatResponse(chat);
    }

    @Override
    public List<ChatResponse> searchChats(String searchTerm) {
        String currentUserId = getCurrentUserId();
        List<Chat> allChats = chatRepository.findChatsByUserId(currentUserId);
        
        return allChats.stream()
                .filter(chat -> chat.getName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                               (chat.getDescription() != null && chat.getDescription().toLowerCase().contains(searchTerm.toLowerCase())))
                .map(this::toChatResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChatResponse getOrCreatePrivateChat(String otherUserId) {
        String currentUserId = getCurrentUserId();
        
        // Check if private chat already exists
        return chatRepository.findPrivateChatBetweenUsers(currentUserId, otherUserId)
                .map(this::toChatResponse)
                .orElseGet(() -> {
                    // Create new private chat
                    ChatRequest chatRequest = ChatRequest.builder()
                            .name("Private Chat")
                            .type(Chat.ChatType.PRIVATE)
                            .participantIds(List.of(otherUserId))
                            .build();
                    return createChat(chatRequest);
                });
    }

    @Override
    @Transactional
    public void markChatAsRead(String chatId) {
        String currentUserId = getCurrentUserId();
        ChatParticipant participant = chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        participant.setLastReadAt(LocalDateTime.now());
        chatParticipantRepository.save(participant);
    }

    @Override
    @Transactional
    public ChatResponse muteChat(String chatId, boolean muted) {
        String currentUserId = getCurrentUserId();
        ChatParticipant participant = chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        participant.setIsMuted(muted);
        chatParticipantRepository.save(participant);

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_NOT_FOUND));
        return toChatResponse(chat);
    }

    @Override
    @Transactional
    public ChatResponse pinChat(String chatId, boolean pinned) {
        String currentUserId = getCurrentUserId();
        ChatParticipant participant = chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));

        participant.setIsPinned(pinned);
        chatParticipantRepository.save(participant);

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_NOT_FOUND));
        return toChatResponse(chat);
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

    private ChatResponse toChatResponse(Chat chat) {
        List<ChatParticipantResponse> participants = chat.getParticipants() != null ?
                chat.getParticipants().stream()
                        .filter(p -> p.getStatus() == EnumStatus.ACTIVE)
                        .map(this::toChatParticipantResponse)
                        .collect(Collectors.toList()) : List.of();

        // Get last message (simplified - you might want to optimize this)
        ChatMessageResponse lastMessage = null;
        if (chat.getMessages() != null && !chat.getMessages().isEmpty()) {
            ChatMessage lastMsg = chat.getMessages().stream()
                    .filter(m -> m.getStatus() == EnumStatus.ACTIVE && !m.getIsDeleted())
                    .max((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                    .orElse(null);
            if (lastMsg != null) {
                lastMessage = toChatMessageResponse(lastMsg);
            }
        }

        return ChatResponse.builder()
                .id(chat.getId())
                .name(chat.getName())
                .description(chat.getDescription())
                .type(chat.getType())
                .status(chat.getStatus())
                .createdById(chat.getCreatedBy().getId())
                .createdByName(chat.getCreatedBy().getFullName())
                .createdAt(chat.getCreatedAt())
                .updatedAt(chat.getUpdatedAt())
                .participants(participants)
                .lastMessage(lastMessage)
                .unreadCount(0L) // TODO: Calculate unread count
                .isMuted(false) // TODO: Get from participant
                .isPinned(false) // TODO: Get from participant
                .build();
    }

    private ChatParticipantResponse toChatParticipantResponse(ChatParticipant participant) {
        return ChatParticipantResponse.builder()
                .id(participant.getId())
                .chatId(participant.getChat().getId())
                .userId(participant.getUser().getId())
                .userName(participant.getUser().getFullName())
                .userAvatar(participant.getUser().getAvatar())
                .role(participant.getRole())
                .status(participant.getStatus())
                .lastReadAt(participant.getLastReadAt())
                .isMuted(participant.getIsMuted())
                .isPinned(participant.getIsPinned())
                .build();
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
