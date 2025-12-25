package com.example.aiservice.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String chatId;
    private String message;
    private List<MessageHistoryItem> messageHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageHistoryItem {
        private String role; // "user" or "assistant"
        private String content;
    }
}

