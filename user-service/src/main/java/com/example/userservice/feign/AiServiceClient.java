package com.example.userservice.feign;

import com.example.userservice.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "ai-service")
public interface AiServiceClient {

    @PostMapping("/api/ai/chat")
    ApiResponse<ChatResponse> chat(@RequestBody ChatRequest request);

    // Inner classes for request/response
    class ChatRequest {
        private String chatId;
        private String message;
        private List<MessageHistoryItem> messageHistory;

        public ChatRequest() {}

        public ChatRequest(String chatId, String message, List<MessageHistoryItem> messageHistory) {
            this.chatId = chatId;
            this.message = message;
            this.messageHistory = messageHistory;
        }

        public String getChatId() {
            return chatId;
        }

        public void setChatId(String chatId) {
            this.chatId = chatId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<MessageHistoryItem> getMessageHistory() {
            return messageHistory;
        }

        public void setMessageHistory(List<MessageHistoryItem> messageHistory) {
            this.messageHistory = messageHistory;
        }

        public static class MessageHistoryItem {
            private String role;
            private String content;

            public MessageHistoryItem() {}

            public MessageHistoryItem(String role, String content) {
                this.role = role;
                this.content = content;
            }

            public String getRole() {
                return role;
            }

            public void setRole(String role) {
                this.role = role;
            }

            public String getContent() {
                return content;
            }

            public void setContent(String content) {
                this.content = content;
            }
        }
    }

    class ChatResponse {
        private String response;

        public ChatResponse() {}

        public ChatResponse(String response) {
            this.response = response;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }
    }
}

