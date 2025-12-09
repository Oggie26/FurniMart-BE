package com.example.aiservice.controller;

import com.example.aiservice.response.ApiResponse;
import com.example.aiservice.response.InteriorDesignResponse;
import com.example.aiservice.service.AiInteriorDesignService;
import com.example.aiservice.service.ChatBoxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatBoxController {

    private final ChatBoxService chatBoxService;
    private final AiInteriorDesignService aiInteriorDesignService;

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


    @PostMapping(value = "/analyze-room", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InteriorDesignResponse>> analyzeRoom(
            @RequestParam("image") MultipartFile image) {

        InteriorDesignResponse result = aiInteriorDesignService.analyzeRoom(image);

        return ResponseEntity.ok(
                ApiResponse.<InteriorDesignResponse>builder()
                        .status(200)
                        .message("Room analyzed successfully")
                        .data(result)
                        .build()
        );
    }


    public record ChatRequest(String message) {}
}
