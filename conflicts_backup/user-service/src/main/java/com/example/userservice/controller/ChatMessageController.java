package com.example.userservice.controller;

import com.example.userservice.request.ChatMessageRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.ChatMessageResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.service.inteface.ChatMessageService;
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
@RequestMapping("/api/chat-messages")
@Tag(name = "Chat Message Controller")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ChatMessageController {
    private final ChatMessageService chatMessageService;

    @PostMapping
    @Operation(summary = "Send message")
    @ResponseStatus(HttpStatus.CREATED)
<<<<<<< HEAD
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
=======
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('STAFF')")
>>>>>>> branch/phong
    public ApiResponse<ChatMessageResponse> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        return ApiResponse.<ChatMessageResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Message sent successfully")
                .data(chatMessageService.sendMessage(request))
                .build();
    }

    @GetMapping("/chat/{chatId}")
    @Operation(summary = "Get chat messages")
<<<<<<< HEAD
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
=======
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('STAFF')")
>>>>>>> branch/phong
    public ApiResponse<List<ChatMessageResponse>> getChatMessages(@PathVariable String chatId) {
        return ApiResponse.<List<ChatMessageResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Messages retrieved successfully")
                .data(chatMessageService.getChatMessages(chatId))
                .build();
    }

    @GetMapping("/chat/{chatId}/paginated")
    @Operation(summary = "Get chat messages with pagination")
<<<<<<< HEAD
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
=======
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('STAFF')")
>>>>>>> branch/phong
    public ApiResponse<PageResponse<ChatMessageResponse>> getChatMessagesWithPagination(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<ChatMessageResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Messages retrieved successfully with pagination")
                .data(chatMessageService.getChatMessagesWithPagination(chatId, page, size))
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get message by ID")
<<<<<<< HEAD
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
=======
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('STAFF')")
>>>>>>> branch/phong
    public ApiResponse<ChatMessageResponse> getMessageById(@PathVariable String id) {
        return ApiResponse.<ChatMessageResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Message retrieved successfully")
                .data(chatMessageService.getMessageById(id))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit message")
<<<<<<< HEAD
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
=======
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('STAFF')")
>>>>>>> branch/phong
    public ApiResponse<ChatMessageResponse> editMessage(@PathVariable String id, @RequestParam String content) {
        return ApiResponse.<ChatMessageResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Message edited successfully")
                .data(chatMessageService.editMessage(id, content))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete message")
<<<<<<< HEAD
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
=======
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('STAFF')")
>>>>>>> branch/phong
    public ApiResponse<Void> deleteMessage(@PathVariable String id) {
        chatMessageService.deleteMessage(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Message deleted successfully")
                .build();
    }

    @GetMapping("/chat/{chatId}/search")
    @Operation(summary = "Search messages in chat")
<<<<<<< HEAD
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
=======
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('STAFF')")
>>>>>>> branch/phong
    public ApiResponse<List<ChatMessageResponse>> searchMessagesInChat(
            @PathVariable String chatId, 
            @RequestParam String searchTerm) {
        return ApiResponse.<List<ChatMessageResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Search completed successfully")
                .data(chatMessageService.searchMessagesInChat(chatId, searchTerm))
                .build();
    }

    @GetMapping("/chat/{chatId}/search/paginated")
    @Operation(summary = "Search messages in chat with pagination")
<<<<<<< HEAD
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
=======
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('STAFF')")
>>>>>>> branch/phong
    public ApiResponse<PageResponse<ChatMessageResponse>> searchMessagesInChatWithPagination(
            @PathVariable String chatId,
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<ChatMessageResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Search completed successfully")
                .data(chatMessageService.searchMessagesInChatWithPagination(chatId, searchTerm, page, size))
                .build();
    }

    @GetMapping("/{id}/replies")
    @Operation(summary = "Get message replies")
<<<<<<< HEAD
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
=======
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('STAFF')")
>>>>>>> branch/phong
    public ApiResponse<List<ChatMessageResponse>> getMessageReplies(@PathVariable String id) {
        return ApiResponse.<List<ChatMessageResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Replies retrieved successfully")
                .data(chatMessageService.getMessageReplies(id))
                .build();
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark message as read")
<<<<<<< HEAD
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
=======
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('STAFF')")
>>>>>>> branch/phong
    public ApiResponse<Void> markMessageAsRead(@PathVariable String id) {
        chatMessageService.markMessageAsRead(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Message marked as read")
                .build();
    }

    @GetMapping("/chat/{chatId}/unread")
    @Operation(summary = "Get unread messages in chat")
<<<<<<< HEAD
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF')")
=======
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('STAFF')")
>>>>>>> branch/phong
    public ApiResponse<List<ChatMessageResponse>> getUnreadMessages(@PathVariable String chatId) {
        return ApiResponse.<List<ChatMessageResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Unread messages retrieved successfully")
                .data(chatMessageService.getUnreadMessages(chatId))
                .build();
    }
}
