package com.example.userservice.response;

import com.example.userservice.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class WebSocketMessage {
    private String type; // MESSAGE, TYPING, USER_JOINED, USER_LEFT, etc.
    private String chatId;
    private String senderId;
    private String content;
    private ChatMessage.MessageType messageType;
    private Long timestamp;
    private Object data; // Additional data for specific message types
}
