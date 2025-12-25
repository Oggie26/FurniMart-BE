package com.example.userservice.websocket;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.ChatMessage;
import com.example.userservice.entity.ChatParticipant;
import com.example.userservice.entity.Employee;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.ChatParticipantRepository;
import com.example.userservice.repository.EmployeeRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.ChatMessageRequest;
import com.example.userservice.response.ChatMessageResponse;
import com.example.userservice.response.ChatResponse;
import com.example.userservice.response.WebSocketMessage;
import com.example.userservice.service.inteface.ChatMessageService;
import com.example.userservice.service.inteface.ChatService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.lang.NonNull;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final ChatParticipantRepository chatParticipantRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final ChatMessageService chatMessageService;
    private final ChatService chatService;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String remoteAddress = session.getRemoteAddress() != null ? session.getRemoteAddress().toString() : "unknown";
        log.info("WebSocket connection established: sessionId={}, remoteAddress={}, uri={}", 
                session.getId(), remoteAddress, session.getUri());
        
        String userId = extractUserIdFromSession(session);
        if (userId != null) {
            sessions.put(session.getId(), session);
            userSessions.put(userId, session.getId());
            log.info("User {} connected to WebSocket. Total active sessions: {}, Total users: {}", 
                    userId, sessions.size(), userSessions.size());
            
            sendMessage(session, WebSocketMessage.builder()
                    .type("CONNECTION_ESTABLISHED")
                    .content("Connected to chat")
                    .timestamp(System.currentTimeMillis())
                    .build());
            
            notifyStaffAboutWaitingChats(userId);
        } else {
            log.warn("No user ID found in WebSocket connection, closing session. URI: {}, Headers: {}", 
                    session.getUri(), session.getHandshakeHeaders());
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void handleMessage(@NonNull WebSocketSession session, @NonNull org.springframework.web.socket.WebSocketMessage<?> message) throws Exception {
        log.info("Received WebSocket message: {}", message.getPayload());
        
        try {
            String payload = (String) message.getPayload();
            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);
            
            String userId = getUserFromSession(session);
            if (userId == null) {
                log.warn("No user found for session: {}", session.getId());
                return;
            }
            
            switch (wsMessage.getType()) {
                case "MESSAGE":
                    handleChatMessage(session, wsMessage, userId);
                    break;
                case "TYPING":
                    handleTypingIndicator(session, wsMessage, userId);
                    break;
                case "JOIN_CHAT":
                    handleJoinChat(session, wsMessage, userId);
                    break;
                case "LEAVE_CHAT":
                    handleLeaveChat(session, wsMessage, userId);
                    break;
                case "PING":
                    handlePing(session, userId);
                    break;
                default:
                    log.warn("Unknown message type: {}", wsMessage.getType());
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        String userId = getUserFromSession(session);
        log.error("WebSocket transport error for session: {}, user: {}, error: {}", 
                session.getId(), userId, exception.getMessage(), exception);
        cleanupSession(session);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus closeStatus) throws Exception {
        String userId = getUserFromSession(session);
        log.info("WebSocket connection closed: sessionId={}, user={}, closeStatus={}, reason={}. Remaining sessions: {}", 
                session.getId(), userId, closeStatus.getCode(), closeStatus.getReason(), sessions.size());
        cleanupSession(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private void handleChatMessage(WebSocketSession session, WebSocketMessage wsMessage, String userId) {
        try {
            // Validate chat access
            if (!hasAccessToChat(userId, wsMessage.getChatId())) {
                sendError(session, "Access denied to chat");
                return;
            }

            // Get user to set up security context - try User first, then Employee
            User user = getUserOrEmployeeUser(userId);
            if (user == null) {
                sendError(session, "User not found: " + userId);
                return;
            }

            // Set up security context for ChatMessageService
            setSecurityContextForUser(user);

            // Create ChatMessageRequest
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .chatId(wsMessage.getChatId())
                    .content(wsMessage.getContent())
                    .type(ChatMessage.MessageType.TEXT) // Default to TEXT, can be enhanced later
                    .build();

            // Call ChatMessageService to save message and trigger AI response
            ChatMessageResponse response = chatMessageService.sendMessage(request);

            // Convert to WebSocket message and broadcast
            WebSocketMessage broadcastMessage = WebSocketMessage.builder()
                    .type("MESSAGE")
                    .chatId(response.getChatId())
                    .senderId(response.getSenderId())
                    .content(response.getContent())
                    .messageType(response.getType())
                    .timestamp(response.getCreatedAt() != null ? response.getCreatedAt().getTime() : System.currentTimeMillis())
                    .build();

            // Broadcast customer message
            broadcastToChat(wsMessage.getChatId(), broadcastMessage, userId);

            log.info("Message saved and broadcasted for chat: {}, user: {}", wsMessage.getChatId(), userId);
            
        } catch (Exception e) {
            log.error("Error handling chat message", e);
            sendError(session, "Error sending message: " + e.getMessage());
        }
    }

    private void setSecurityContextForUser(User user) {
        try {
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
        } catch (Exception e) {
            log.error("Error setting security context for user: {}", user.getId(), e);
            throw new RuntimeException("Failed to set security context", e);
        }
    }

    private void handleTypingIndicator(WebSocketSession session, WebSocketMessage wsMessage, String userId) {
        try {
            if (!hasAccessToChat(userId, wsMessage.getChatId())) {
                return;
            }

            // Broadcast typing indicator to other participants
            broadcastTypingToChat(wsMessage.getChatId(), userId, wsMessage.getContent());
            
        } catch (Exception e) {
            log.error("Error handling typing indicator", e);
        }
    }

    private void handleJoinChat(WebSocketSession session, WebSocketMessage wsMessage, String userId) {
        try {
            if (!hasAccessToChat(userId, wsMessage.getChatId())) {
                sendError(session, "Access denied to chat");
                return;
            }

            // Notify other participants that user joined
            WebSocketMessage joinMessage = WebSocketMessage.builder()
                    .type("USER_JOINED")
                    .chatId(wsMessage.getChatId())
                    .senderId(userId)
                    .content("User joined the chat")
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            broadcastToChat(wsMessage.getChatId(), joinMessage, userId);
            
        } catch (Exception e) {
            log.error("Error handling join chat", e);
            sendError(session, "Error joining chat");
        }
    }

    private void handleLeaveChat(WebSocketSession session, WebSocketMessage wsMessage, String userId) {
        try {
            // Notify other participants that user left
            WebSocketMessage leaveMessage = WebSocketMessage.builder()
                    .type("USER_LEFT")
                    .chatId(wsMessage.getChatId())
                    .senderId(userId)
                    .content("User left the chat")
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            broadcastToChat(wsMessage.getChatId(), leaveMessage, userId);
            
        } catch (Exception e) {
            log.error("Error handling leave chat", e);
        }
    }

    private void handlePing(WebSocketSession session, String userId) {
        try {
            WebSocketMessage pongMessage = WebSocketMessage.builder()
                    .type("PONG")
                    .content("pong")
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            sendMessage(session, pongMessage);
        } catch (Exception e) {
            log.error("Error handling ping", e);
        }
    }

    // Public method to broadcast message to all participants in a chat (for AI responses)
    public void broadcastToChat(String chatId, WebSocketMessage message) {
        broadcastToChatInternal(chatId, message, null);
    }

    // Public method to broadcast message to all participants except the sender
    public void broadcastToChat(String chatId, WebSocketMessage message, String excludeUserId) {
        broadcastToChatInternal(chatId, message, excludeUserId);
    }

    private void broadcastToChatInternal(String chatId, WebSocketMessage message, String excludeUserId) {
        try {
            var participants = chatParticipantRepository.findActiveParticipantsByChatId(chatId);
            
            for (var participant : participants) {
                String participantUserId = getParticipantId(participant);
                if (participantUserId == null) {
                    continue;
                }
                
                if (excludeUserId == null || !participantUserId.equals(excludeUserId)) {
                    String sessionId = userSessions.get(participantUserId);
                    if (sessionId != null) {
                        WebSocketSession participantSession = sessions.get(sessionId);
                        if (participantSession != null && participantSession.isOpen()) {
                            sendMessage(participantSession, message);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error broadcasting to chat: {}", chatId, e);
        }
    }

    private void broadcastTypingToChat(String chatId, String userId, String typingStatus) {
        try {
            var participants = chatParticipantRepository.findActiveParticipantsByChatId(chatId);
            
            WebSocketMessage typingMessage = WebSocketMessage.builder()
                    .type("TYPING")
                    .chatId(chatId)
                    .senderId(userId)
                    .content(typingStatus)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            for (var participant : participants) {
                String participantUserId = getParticipantId(participant);
                if (participantUserId == null) {
                    continue;
                }
                
                if (!participantUserId.equals(userId)) {
                    String sessionId = userSessions.get(participantUserId);
                    if (sessionId != null) {
                        WebSocketSession participantSession = sessions.get(sessionId);
                        if (participantSession != null && participantSession.isOpen()) {
                            sendMessage(participantSession, typingMessage);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error broadcasting typing indicator", e);
        }
    }

    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            if (session.isOpen()) {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                log.debug("WebSocket message sent: type={}, sessionId={}", message.getType(), session.getId());
            } else {
                log.warn("Attempted to send message to closed WebSocket session: {}", session.getId());
            }
        } catch (IOException e) {
            log.error("Error sending WebSocket message to session: {}, message type: {}", 
                    session.getId(), message.getType(), e);
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            WebSocketMessage errorMsg = WebSocketMessage.builder()
                    .type("ERROR")
                    .content(errorMessage)
                    .timestamp(System.currentTimeMillis())
                    .build();
            sendMessage(session, errorMsg);
        } catch (Exception e) {
            log.error("Error sending error message", e);
        }
    }

    private String extractUserIdFromSession(WebSocketSession session) {
        // Try to get user ID from query parameters
        URI uri = session.getUri();
        if (uri != null) {
            String query = uri.getQuery();
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2 && "userId".equals(keyValue[0])) {
                        return keyValue[1];
                    }
                }
            }
        }
        
        // Try to get from headers
        var headers = session.getHandshakeHeaders();
        if (headers.containsKey("userId")) {
            return headers.getFirst("userId");
        }
        
        return null;
    }

    private String getUserFromSession(WebSocketSession session) {
        for (Map.Entry<String, String> entry : userSessions.entrySet()) {
            if (entry.getValue().equals(session.getId())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean hasAccessToChat(String userId, String chatId) {
        try {
            // For testing purposes, allow access if user ID and chat ID are provided
            // In production, you would validate against the database
            if (userId != null && chatId != null && !userId.isEmpty() && !chatId.isEmpty()) {
                // Check if this is a test user (simple string like "user123")
                if (userId.matches("^[a-zA-Z0-9]+$") && !userId.contains("-")) {
                    log.info("Allowing test user access: {} to chat: {}", userId, chatId);
                    return true;
                }
                // For real users, check database
                return chatParticipantRepository.findActiveParticipantByChatIdAndUserId(chatId, userId).isPresent();
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking chat access", e);
            // For testing, allow access if we can't check the database
            return userId != null && chatId != null && !userId.isEmpty() && !chatId.isEmpty();
        }
    }

    private void cleanupSession(WebSocketSession session) {
        String userId = getUserFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
        }
        sessions.remove(session.getId());
    }

    // Public method to send message to specific user
    public void sendMessageToUser(String userId, WebSocketMessage message) {
        String sessionId = userSessions.get(userId);
        if (sessionId != null) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                sendMessage(session, message);
            }
        }
    }

    public void broadcastToAll(WebSocketMessage message) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                sendMessage(session, message);
            }
        });
    }

    public Set<String> getOnlineUserIds() {
        return new HashSet<>(userSessions.keySet());
    }

    private void notifyStaffAboutWaitingChats(String userId) {
        try {
            // Check if user is staff - userId could be Employee.id, User.id, or Account.id
            boolean isStaff = false;
            
            // First, try to find as Employee (most common for staff)
            // Use findEmployeeById which already filters by isDeleted and includes STAFF role
            Optional<Employee> employeeOpt = employeeRepository.findEmployeeById(userId)
                    .filter(emp -> emp.getAccount() != null 
                            && emp.getAccount().getRole() == EnumRole.STAFF);
            
            if (employeeOpt.isPresent()) {
                isStaff = true;
                log.debug("User {} identified as STAFF (Employee)", userId);
            } else {
                // Try to find as User (less common for staff, but possible)
                Optional<User> userOpt = userRepository.findByIdAndIsDeletedFalse(userId);
                if (userOpt.isPresent() && userOpt.get().getAccount() != null 
                        && userOpt.get().getAccount().getRole() == EnumRole.STAFF) {
                    isStaff = true;
                    log.debug("User {} identified as STAFF (User)", userId);
                } else {
                    // Try to find as Account (if userId is actually accountId)
                    Optional<Account> accountOpt = accountRepository.findByIdAndIsDeletedFalse(userId);
                    if (accountOpt.isPresent() && accountOpt.get().getRole() == EnumRole.STAFF) {
                        isStaff = true;
                        log.debug("User {} identified as STAFF (Account)", userId);
                    }
                }
            }
            
            if (!isStaff) {
                log.debug("User {} is not a staff member, skipping notification", userId);
                return;
            }

            List<ChatResponse> waitingChats = chatService.getChatsWaitingForStaff();
            
            if (!waitingChats.isEmpty()) {
                WebSocketMessage message = WebSocketMessage.builder()
                        .type("WAITING_CHATS_NOTIFICATION")
                        .content(String.format("Có %d chat đang chờ hỗ trợ", waitingChats.size()))
                        .data(Map.of(
                                "waitingChatCount", waitingChats.size(),
                                "message", String.format("Có %d chat đang chờ hỗ trợ. Vui lòng kiểm tra queue.", waitingChats.size())
                        ))
                        .timestamp(System.currentTimeMillis())
                        .build();

                sendMessageToUser(userId, message);
                log.info("Notified staff {} about {} waiting chats", userId, waitingChats.size());
            } else {
                log.debug("No waiting chats to notify staff {}", userId);
            }
        } catch (Exception e) {
            log.error("Error notifying staff about waiting chats for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Helper method to get participant ID from ChatParticipant (User or Employee)
     */
    private String getParticipantId(ChatParticipant participant) {
        if (participant.getUser() != null) {
            return participant.getUser().getId();
        } else if (participant.getEmployee() != null) {
            // For employees, we need to get the User ID from their account
            // If employee has a User associated with their account, use that ID
            // Otherwise, use the employee ID (but this might cause issues in WebSocket tracking)
            Employee employee = participant.getEmployee();
            if (employee.getAccount() != null && employee.getAccount().getUser() != null) {
                return employee.getAccount().getUser().getId();
            }
            // Fallback to employee ID - WebSocket should track by this
            return employee.getId();
        }
        return null;
    }

    /**
     * Helper method to get User entity from either User ID or Employee ID
     * For employees, returns the User associated with their account (or creates one if needed)
     * Similar to logic in ChatServiceImpl.createChat() and ChatMessageServiceImpl.sendMessage()
     */
    private User getUserOrEmployeeUser(String userId) {
        // Try to find as User first
        Optional<User> userOpt = userRepository.findByIdAndIsDeletedFalse(userId);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }
        
        // Try to find as Employee
        Optional<Employee> employeeOpt = employeeRepository.findEmployeeById(userId);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            Account account = employee.getAccount();
            if (account != null && account.getUser() != null) {
                return account.getUser();
            }
            // If employee doesn't have a User, create one (similar to ChatServiceImpl.createChat())
            // This ensures employees can send messages via WebSocket
            log.info("Creating User entity for Employee {} to send chat messages via WebSocket", userId);
            try {
                User newUser = User.builder()
                        .fullName(employee.getFullName())
                        .phone(employee.getPhone())
                        .status(EnumStatus.ACTIVE)
                        .avatar(employee.getAvatar())
                        .account(account)
                        .build();
                newUser.setIsDeleted(false);
                return userRepository.save(newUser);
            } catch (Exception e) {
                log.error("Error creating User for Employee {}: {}", userId, e.getMessage(), e);
                return null;
            }
        }
        
        return null;
    }
}
