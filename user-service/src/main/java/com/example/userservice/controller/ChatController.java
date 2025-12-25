package com.example.userservice.controller;

import com.example.userservice.request.ChatRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.ChatResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.StaffOnlineStatusResponse;
import com.example.userservice.service.inteface.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@Tag(name = "Chat Controller")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ChatController {
    private final ChatService chatService;
    
    // Track last request time per user for polling frequency monitoring
    private final java.util.Map<String, Long> lastRequestTime = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping
    @Operation(summary = "Create new chat")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<ChatResponse> createChat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Chat created successfully")
                .data(chatService.createChat(request))
                .build();
    }

    @PostMapping("/quick-create")
    @Operation(
            summary = "Quick create chat for customer",
            description = "Tạo chat nhanh cho khách hàng - không cần nhập tên hay chọn participants. " +
                          "Tên chat sẽ được tự động tạo với timestamp. Chat sẽ ở chế độ AI. " +
                          "Customer có thể chat với AI, hoặc bấm nút 'Gặp nhân viên' để yêu cầu gặp staff."
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<ChatResponse> quickCreateChat() {
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Chat created successfully")
                .data(chatService.quickCreateChatForCustomer())
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get chat by ID")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<ChatResponse> getChatById(@PathVariable String id, HttpServletResponse response) {
        String userIdentifier = getUserIdentifier();
        logPollingFrequency("/api/chats/" + id, userIdentifier);
        
        // Add cache headers: no-cache for polling endpoints (client should use WebSocket)
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader(HttpHeaders.EXPIRES, "0");
        
        ChatResponse chat = chatService.getChatById(id);
        
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Chat retrieved successfully. Consider using WebSocket for real-time updates.")
                .data(chat)
                .build();
    }

    @GetMapping
    @Operation(summary = "Get user's chats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<List<ChatResponse>> getUserChats(HttpServletResponse response) {
        String userIdentifier = getUserIdentifier();
        logPollingFrequency("/api/chats", userIdentifier);
        
        // Add cache headers: no-cache for polling endpoints (client should use WebSocket)
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader(HttpHeaders.EXPIRES, "0");
        
        List<ChatResponse> chats = chatService.getUserChats();
        
        return ApiResponse.<List<ChatResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Chats retrieved successfully. Consider using WebSocket for real-time updates.")
                .data(chats)
                .build();
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get user's chats with pagination")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<PageResponse<ChatResponse>> getUserChatsWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<ChatResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Chats retrieved successfully with pagination")
                .data(chatService.getUserChatsWithPagination(page, size))
                .build();
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest chats with unread priority (unread first, then read)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<List<ChatResponse>> getLatestChatsWithUnreadPriority(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.<List<ChatResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Latest chats retrieved successfully with unread priority")
                .data(chatService.getLatestChatsWithUnreadPriority(limit))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update chat")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<ChatResponse> updateChat(@PathVariable String id, @Valid @RequestBody ChatRequest request) {
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Chat updated successfully")
                .data(chatService.updateChat(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete chat")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<Void> deleteChat(@PathVariable String id) {
        chatService.deleteChat(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Chat deleted successfully")
                .build();
    }

    @PostMapping("/{id}/participants/{userId}")
    @Operation(summary = "Add participant to chat")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<ChatResponse> addParticipant(@PathVariable String id, @PathVariable String userId) {
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Participant added successfully")
                .data(chatService.addParticipant(id, userId))
                .build();
    }

    @DeleteMapping("/{id}/participants/{userId}")
    @Operation(summary = "Remove participant from chat")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<ChatResponse> removeParticipant(@PathVariable String id, @PathVariable String userId) {
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Participant removed successfully")
                .data(chatService.removeParticipant(id, userId))
                .build();
    }

    @PutMapping("/{id}/participants/{userId}/role")
    @Operation(summary = "Update participant role")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<ChatResponse> updateParticipantRole(
            @PathVariable String id, 
            @PathVariable String userId, 
            @RequestParam String role) {
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Participant role updated successfully")
                .data(chatService.updateParticipantRole(id, userId, role))
                .build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search chats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<List<ChatResponse>> searchChats(@RequestParam String searchTerm) {
        return ApiResponse.<List<ChatResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Search completed successfully")
                .data(chatService.searchChats(searchTerm))
                .build();
    }

    @PostMapping("/private/{userId}")
    @Operation(summary = "Get or create private chat with user")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<ChatResponse> getOrCreatePrivateChat(@PathVariable String userId) {
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Private chat retrieved/created successfully")
                .data(chatService.getOrCreatePrivateChat(userId))
                .build();
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark chat as read")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<Void> markChatAsRead(@PathVariable String id) {
        chatService.markChatAsRead(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Chat marked as read")
                .build();
    }

    @PatchMapping("/{id}/mute")
    @Operation(summary = "Mute/unmute chat")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<ChatResponse> muteChat(@PathVariable String id, @RequestParam boolean muted) {
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.OK.value())
                .message(muted ? "Chat muted" : "Chat unmuted")
                .data(chatService.muteChat(id, muted))
                .build();
    }

    @PatchMapping("/{id}/pin")
    @Operation(summary = "Pin/unpin chat")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<ChatResponse> pinChat(@PathVariable String id, @RequestParam boolean pinned) {
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.OK.value())
                .message(pinned ? "Chat pinned" : "Chat unpinned")
                .data(chatService.pinChat(id, pinned))
                .build();
    }

    @PostMapping("/{id}/request-staff")
    @Operation(summary = "Request staff connection")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<ChatResponse> requestStaffConnection(@PathVariable String id) {
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Staff connection requested")
                .data(chatService.requestStaffConnection(id))
                .build();
    }

    @GetMapping("/waiting-staff")
    @Operation(summary = "Get chats waiting for staff (Staff only)")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ApiResponse<List<ChatResponse>> getChatsWaitingForStaff(HttpServletResponse response) {
        String userIdentifier = getUserIdentifier();
        logPollingFrequency("/api/chats/waiting-staff", userIdentifier);
        
        // Add cache headers: no-cache for polling endpoints (client should use WebSocket)
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader(HttpHeaders.EXPIRES, "0");
        
        List<ChatResponse> waitingChats = chatService.getChatsWaitingForStaff();
        
        return ApiResponse.<List<ChatResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Waiting chats retrieved successfully. Consider using WebSocket for real-time updates.")
                .data(waitingChats)
                .build();
    }

    @PostMapping("/{id}/accept-staff")
    @Operation(summary = "Accept staff connection (Staff only)")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ApiResponse<ChatResponse> acceptStaffConnection(@PathVariable String id) {
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Staff connection accepted")
                .data(chatService.acceptStaffConnection(id))
                .build();
    }

    @PostMapping("/{id}/end-staff-chat")
    @Operation(summary = "End staff chat")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('ADMIN')")
    public ApiResponse<ChatResponse> endStaffChat(@PathVariable String id) {
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Staff chat ended")
                .data(chatService.endStaffChat(id))
                .build();
    }

    @GetMapping("/staff/online-count")
    @Operation(
            summary = "Get online staff count and estimated wait time",
            description = "Lấy số lượng staff đang online và thời gian ước tính chờ. " +
                        "Dành cho customer để biết tình trạng hỗ trợ."
    )
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('ADMIN')")
    public ApiResponse<StaffOnlineStatusResponse> getStaffOnlineStatus() {
        int onlineCount = chatService.getOnlineStaffCount();
        boolean hasOnlineStaff = chatService.hasOnlineStaff();
        String estimatedWaitTime = chatService.getEstimatedWaitTime();

        StaffOnlineStatusResponse response = StaffOnlineStatusResponse.builder()
                .onlineCount(onlineCount)
                .hasOnlineStaff(hasOnlineStaff)
                .estimatedWaitTime(estimatedWaitTime)
                .build();

        return ApiResponse.<StaffOnlineStatusResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Staff online status retrieved successfully")
                .data(response)
                .build();
    }

    @GetMapping("/websocket-status")
    @Operation(summary = "Check WebSocket connection status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<WebSocketStatusResponse> getWebSocketStatus() {
        // This endpoint helps FE determine if WebSocket is available
        // FE should check this before falling back to polling
        return ApiResponse.<WebSocketStatusResponse>builder()
                .status(HttpStatus.OK.value())
                .message("WebSocket status retrieved successfully")
                .data(WebSocketStatusResponse.builder()
                        .websocketAvailable(true)
                        .websocketEndpoint("/ws/chat")
                        .recommendedUsage("Use WebSocket for real-time chat updates instead of polling")
                        .pollingFallbackInterval(5000) // 5 seconds minimum if polling is used
                        .build())
                .build();
    }

    /**
     * Log polling frequency to monitor excessive polling
     */
    private void logPollingFrequency(String endpoint, String userIdentifier) {
        long currentTime = System.currentTimeMillis();
        String key = userIdentifier + ":" + endpoint;
        
        Long lastTime = lastRequestTime.get(key);
        if (lastTime != null) {
            long timeSinceLastRequest = currentTime - lastTime;
            if (timeSinceLastRequest < 2000) { // Less than 2 seconds
                log.warn("High polling frequency detected: User {} polling {} every {}ms. Consider using WebSocket.", 
                        userIdentifier, endpoint, timeSinceLastRequest);
            } else if (timeSinceLastRequest < 5000) { // Less than 5 seconds
                log.debug("Frequent polling: User {} polling {} every {}ms", 
                        userIdentifier, endpoint, timeSinceLastRequest);
            }
        }
        lastRequestTime.put(key, currentTime);
    }

    /**
     * Get user identifier for logging (email if authenticated, otherwise "anonymous")
     */
    private String getUserIdentifier() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName(); // Returns email
            }
        } catch (Exception e) {
            log.debug("Unable to get user identifier: {}", e.getMessage());
        }
        return "anonymous";
    }

    /**
     * Response DTO for WebSocket status
     */
    @Data
    @Builder
    public static class WebSocketStatusResponse {
        private boolean websocketAvailable;
        private String websocketEndpoint;
        private String recommendedUsage;
        private long pollingFallbackInterval; // in milliseconds
    }
}
