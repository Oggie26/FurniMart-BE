package com.example.aiservice.controller;

import com.example.aiservice.response.ApiResponse;
import com.example.aiservice.service.ChatBoxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatBoxController {

    private final ChatBoxService chatBoxService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> chat(@RequestBody ChatRequest request) {
        String reply = chatBoxService.chatWithAI(request.message());

        ApiResponse<String> response = ApiResponse.<String>builder()
                .status(200)
                .message("AI phản hồi thành công")
                .data(reply)
                .build();

        return ResponseEntity.ok(response);
    }

    public record ChatRequest(String message) {}
}
