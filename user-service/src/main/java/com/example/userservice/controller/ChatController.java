package com.example.userservice.controller;

import com.example.userservice.request.ChatRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.ChatResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.service.inteface.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@Tag(name = "Chat Controller")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ChatController {
    private final ChatService chatService;

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

    @GetMapping("/{id}")
    @Operation(summary = "Get chat by ID")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<ChatResponse> getChatById(@PathVariable String id) {
        return ApiResponse.<ChatResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Chat retrieved successfully")
                .data(chatService.getChatById(id))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get user's chats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
    public ApiResponse<List<ChatResponse>> getUserChats() {
        return ApiResponse.<List<ChatResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Chats retrieved successfully")
                .data(chatService.getUserChats())
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
}
