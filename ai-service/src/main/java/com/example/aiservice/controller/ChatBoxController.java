package com.example.aiservice.controller;

import com.example.aiservice.response.ApiResponse;
import com.example.aiservice.service.ChatBoxService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Cho phép gọi từ web/mobile
public class ChatBoxController {

    private final ChatBoxService chatBoxService;


    @PostMapping
    public ApiResponse<String> chat(@RequestParam("message") String message) {
        String reply = chatBoxService.chatWithAI(message);
        return ApiResponse.<String>builder()
                .status(200)
                .message("AI response successfully")
                .data(reply)
                .build();
    }
}
