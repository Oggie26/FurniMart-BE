package com.example.userservice.service;

import com.example.userservice.entity.*;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.ChatParticipantRepository;
import com.example.userservice.repository.ChatRepository;
import com.example.userservice.repository.EmployeeRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.ChatRequest;
import com.example.userservice.response.ChatMessageResponse;
import com.example.userservice.response.ChatParticipantResponse;
import com.example.userservice.response.ChatResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.WebSocketMessage;
import com.example.userservice.service.inteface.ChatService;
import com.example.userservice.websocket.ChatWebSocketHandler;
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

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final ChatWebSocketHandler chatWebSocketHandler;

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
                .chatMode(Chat.ChatMode.AI) // Ensure new chats start in AI mode
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

        ChatResponse.ChatResponseBuilder builder = ChatResponse.builder()
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
                // New fields for AI chat to staff flow
                .chatMode(chat.getChatMode() != null ? chat.getChatMode() : Chat.ChatMode.AI)
                .assignedStaffId(chat.getAssignedStaffId())
                .staffRequestedAt(chat.getStaffRequestedAt())
                .staffChatEndedAt(chat.getStaffChatEndedAt());
        
        // Load assigned staff name if exists
        if (chat.getAssignedStaffId() != null) {
            User staff = userRepository.findById(chat.getAssignedStaffId()).orElse(null);
            if (staff != null) {
                builder.assignedStaffName(staff.getFullName());
            }
        }
        
        return builder.build();
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

    // ========== NEW METHODS FOR AI CHAT TO STAFF FLOW ==========

    @Override
    @Transactional
    public ChatResponse requestStaffConnection(String chatId) {
        String currentUserId = getCurrentUserId();
        User customer = userRepository.findByIdAndIsDeletedFalse(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_NOT_FOUND));

        // Verify customer owns the chat
        if (!chat.getCreatedBy().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Check if chat is in AI mode
        Chat.ChatMode currentMode = chat.getChatMode() != null ?
                chat.getChatMode() : Chat.ChatMode.AI;

        if (currentMode != Chat.ChatMode.AI) {
            throw new AppException(ErrorCode.INVALID_CHAT_STATE);
        }

        // Change chat mode to WAITING_STAFF
        chat.setChatMode(Chat.ChatMode.WAITING_STAFF);
        chat.setStaffRequestedAt(LocalDateTime.now());
        chatRepository.save(chat);

        // Get online staff and send notifications
        notifyAllOnlineStaff(chat, customer);

        log.info("Customer {} requested staff connection for chat {}", currentUserId, chatId);
        return toChatResponse(chat);
    }

    @Override
    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public ChatResponse acceptStaffConnection(String chatId) {
        String currentAccountId = getCurrentAccountId();
        Employee staff = employeeRepository.findByAccountIdAndIsDeletedFalse(currentAccountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Verify staff role
        if (staff.getAccount().getRole() != EnumRole.STAFF) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_NOT_FOUND));

        // Check if chat is in WAITING_STAFF mode
        if (chat.getChatMode() != Chat.ChatMode.WAITING_STAFF) {
            throw new AppException(ErrorCode.CHAT_ALREADY_ACCEPTED);
        }

        // Atomic update - only first staff wins
        chat.setChatMode(Chat.ChatMode.STAFF_CONNECTED);
        chat.setAssignedStaffId(staff.getId());
        chatRepository.save(chat);

        // Add staff to chat participants if not exists
        addStaffToChatParticipants(chat, staff);

        // Notify customer
        notifyCustomerStaffConnected(chat, staff);

        // Notify other staff that chat was accepted
        notifyOtherStaffChatAccepted(chat, staff.getId());

        log.info("Staff {} accepted chat connection for chat {}", staff.getId(), chatId);
        return toChatResponse(chat);
    }

    @Override
    @Transactional
    public ChatResponse endStaffChat(String chatId) {
        String currentUserId = getCurrentUserId();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_NOT_FOUND));

        // Verify user is customer or assigned staff
        String customerId = chat.getCreatedBy().getId();
        String assignedStaffId = chat.getAssignedStaffId();

        // Check if current user is customer (User ID)
        boolean isCustomer = currentUserId.equals(customerId);

        // Check if current user is assigned staff (need to check via Employee)
        boolean isAssignedStaff = false;
        if (assignedStaffId != null) {
            // Get current account ID
            String currentAccountId = getCurrentAccountId();
            Employee currentEmployee = employeeRepository.findByAccountIdAndIsDeletedFalse(currentAccountId)
                    .orElse(null);
            if (currentEmployee != null && currentEmployee.getId().equals(assignedStaffId)) {
                isAssignedStaff = true;
            }
        }

        if (!isCustomer && !isAssignedStaff) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Check if chat is in STAFF_CONNECTED mode
        if (chat.getChatMode() != Chat.ChatMode.STAFF_CONNECTED) {
            throw new AppException(ErrorCode.INVALID_CHAT_STATE);
        }

        // Change chat mode back to AI
        chat.setChatMode(Chat.ChatMode.AI);
        chat.setStaffChatEndedAt(LocalDateTime.now());
        chat.setAssignedStaffId(null); // Clear assignment
        chatRepository.save(chat);

        // Notify both customer and staff
        notifyChatEnded(chat);

        log.info("Chat {} ended by user {}", chatId, currentUserId);
        return toChatResponse(chat);
    }

    @Override
    public List<Employee> getOnlineStaff() {
        // Option 1: Use WebSocket sessions (if available)
        Set<String> onlineAccountIds = getOnlineAccountIdsFromWebSocket();

        // Option 2: Query from database (fallback - return all active staff)
        if (onlineAccountIds.isEmpty()) {
            log.warn("No online staff detected via WebSocket. Returning all active staff as fallback.");
            return employeeRepository.findEmployeesByRole(EnumRole.STAFF);
        }

        // Get employees by account IDs
        List<Employee> onlineStaff = new ArrayList<>();
        for (String accountId : onlineAccountIds) {
            employeeRepository.findByAccountIdAndIsDeletedFalse(accountId)
                    .filter(emp -> emp.getAccount().getRole() == EnumRole.STAFF)
                    .ifPresent(onlineStaff::add);
        }

        return onlineStaff;
    }

    @Override
    public boolean isStaffOnline(String staffId) {
        Employee staff = employeeRepository.findById(staffId).orElse(null);
        if (staff == null || staff.getAccount() == null) {
            return false;
        }

        String accountId = staff.getAccount().getId();
        Set<String> onlineAccountIds = getOnlineAccountIdsFromWebSocket();
        return onlineAccountIds.contains(accountId);
    }

    // ========== HELPER METHODS ==========

    private String getCurrentAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = authentication.getName();
        Account account = accountRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        return account.getId();
    }

    private Set<String> getOnlineAccountIdsFromWebSocket() {
        // Get from ChatWebSocketHandler
        // WebSocket handler tracks user IDs, we need to convert to account IDs
        try {
            Set<String> onlineUserIds = chatWebSocketHandler.getOnlineUserIds();
            Set<String> accountIds = new HashSet<>();
            
            // Convert user IDs to account IDs
            for (String userId : onlineUserIds) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.getAccount() != null) {
                    accountIds.add(user.getAccount().getId());
                }
            }
            
            return accountIds;
        } catch (Exception e) {
            log.warn("Error getting online account IDs from WebSocket: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    private void addStaffToChatParticipants(Chat chat, Employee staff) {
        // Check if staff is already a participant
        String staffUserId = getStaffUserId(staff);
        if (staffUserId == null) {
            log.warn("Cannot add staff {} to chat participants: Staff has no associated User", staff.getId());
            return;
        }

        Optional<ChatParticipant> existingParticipant = chatParticipantRepository
                .findActiveParticipantByChatIdAndUserId(chat.getId(), staffUserId);

        if (existingParticipant.isEmpty()) {
            // Create User for staff if needed (staff might not have User entity)
            // For now, we'll use a workaround: create a minimal participant
            // Note: This might need adjustment based on your schema
            User staffUser = getOrCreateUserForEmployee(staff);
            
            ChatParticipant staffParticipant = ChatParticipant.builder()
                    .chat(chat)
                    .user(staffUser)
                    .role(ChatParticipant.ParticipantRole.MEMBER)
                    .status(EnumStatus.ACTIVE)
                    .lastReadAt(LocalDateTime.now())
                    .build();
            chatParticipantRepository.save(staffParticipant);
            log.info("Added staff {} as participant to chat {}", staff.getId(), chat.getId());
        }
    }

    private String getStaffUserId(Employee staff) {
        // Staff might not have a User entity directly
        // We need to check if there's a User linked to the Account
        // For now, return null and handle in addStaffToChatParticipants
        Account account = staff.getAccount();
        if (account != null && account.getUser() != null) {
            return account.getUser().getId();
        }
        return null;
    }

    private User getOrCreateUserForEmployee(Employee employee) {
        // Check if User exists for this Employee's Account
        Account account = employee.getAccount();
        if (account != null && account.getUser() != null) {
            return account.getUser();
        }

        // Create a minimal User for staff (if needed)
        // Note: This is a workaround - in production, you might want to ensure
        // all Employees have associated Users, or adjust ChatParticipant to support Employee
        log.warn("Employee {} does not have associated User. Creating minimal User.", employee.getId());
        
        User staffUser = User.builder()
                .fullName(employee.getFullName())
                .phone(employee.getPhone())
                .status(EnumStatus.ACTIVE)
                .avatar(employee.getAvatar())
                .account(account)
                .build();
        
        return userRepository.save(staffUser);
    }

    private void notifyAllOnlineStaff(Chat chat, User customer) {
        List<Employee> onlineStaff = getOnlineStaff();
        
        if (onlineStaff.isEmpty()) {
            log.warn("No online staff found to notify for chat {}", chat.getId());
            return;
        }

        WebSocketMessage message = WebSocketMessage.builder()
                .type("STAFF_CHAT_REQUEST")
                .chatId(chat.getId())
                .senderId(customer.getId())
                .content("Khách hàng muốn kết nối trực tiếp")
                .data(Map.of(
                        "customerId", customer.getId(),
                        "customerName", customer.getFullName() != null ? customer.getFullName() : "Khách hàng",
                        "chatId", chat.getId()
                ))
                .timestamp(System.currentTimeMillis())
                .build();

        for (Employee staff : onlineStaff) {
            // Try to send via user ID
            // Note: WebSocket handler tracks by user ID, so we need to get user ID
            String userId = getStaffUserId(staff);
            if (userId != null) {
                chatWebSocketHandler.sendMessageToUser(userId, message);
            }
        }

        log.info("Notified {} online staff about chat request {}", onlineStaff.size(), chat.getId());
    }

    private void notifyCustomerStaffConnected(Chat chat, Employee staff) {
        WebSocketMessage message = WebSocketMessage.builder()
                .type("STAFF_CONNECTED")
                .chatId(chat.getId())
                .senderId(staff.getId())
                .content("Nhân viên đã kết nối. Bạn có thể chat trực tiếp.")
                .data(Map.of(
                        "staffId", staff.getId(),
                        "staffName", staff.getFullName() != null ? staff.getFullName() : "Nhân viên",
                        "chatId", chat.getId()
                ))
                .timestamp(System.currentTimeMillis())
                .build();

        String customerId = chat.getCreatedBy().getId();
        chatWebSocketHandler.sendMessageToUser(customerId, message);
        log.info("Notified customer {} that staff {} connected to chat {}", customerId, staff.getId(), chat.getId());
    }

    private void notifyOtherStaffChatAccepted(Chat chat, String acceptedStaffId) {
        List<Employee> onlineStaff = getOnlineStaff();
        
        WebSocketMessage message = WebSocketMessage.builder()
                .type("CHAT_ACCEPTED_BY_OTHER")
                .chatId(chat.getId())
                .content("Chat này đã được nhận bởi staff khác")
                .data(Map.of(
                        "acceptedByStaffId", acceptedStaffId,
                        "chatId", chat.getId()
                ))
                .timestamp(System.currentTimeMillis())
                .build();

        for (Employee staff : onlineStaff) {
            if (!staff.getId().equals(acceptedStaffId)) {
                String userId = getStaffUserId(staff);
                if (userId != null) {
                    chatWebSocketHandler.sendMessageToUser(userId, message);
                }
            }
        }

        log.info("Notified {} other staff that chat {} was accepted by {}", 
                onlineStaff.size() - 1, chat.getId(), acceptedStaffId);
    }

    private void notifyChatEnded(Chat chat) {
        WebSocketMessage message = WebSocketMessage.builder()
                .type("STAFF_CHAT_ENDED")
                .chatId(chat.getId())
                .content("Chat với staff đã kết thúc. Bạn có thể tiếp tục chat với AI.")
                .timestamp(System.currentTimeMillis())
                .build();

        // Notify customer
        String customerId = chat.getCreatedBy().getId();
        chatWebSocketHandler.sendMessageToUser(customerId, message);

        // Notify staff
        if (chat.getAssignedStaffId() != null) {
            Employee staff = employeeRepository.findById(chat.getAssignedStaffId()).orElse(null);
            if (staff != null) {
                String userId = getStaffUserId(staff);
                if (userId != null) {
                    chatWebSocketHandler.sendMessageToUser(userId, message);
                }
            }
        }

        log.info("Notified customer and staff that chat {} ended", chat.getId());
    }
}
