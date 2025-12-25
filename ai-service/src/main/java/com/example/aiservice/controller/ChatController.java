package com.example.aiservice.controller;

import com.example.aiservice.request.ChatRequest;
import com.example.aiservice.response.ApiResponse;
import com.example.aiservice.response.ChatResponse;
import com.example.aiservice.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/chat")
@Tag(name = "AI Chat Controller")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "Chat with AI", description = "Gửi message và nhận phản hồi từ AI assistant")
    public ApiResponse<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("Received chat request for chatId: {}", request.getChatId());
        
        try {
            ChatResponse response = chatService.chat(request);
            
            return ApiResponse.<ChatResponse>builder()
                    .status(HttpStatus.OK.value())
                    .message("AI response generated successfully")
                    .data(response)
                    .build();
        } catch (Exception e) {
            log.error("Error in chat endpoint", e);
            return ApiResponse.<ChatResponse>builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Error processing chat request: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
}

