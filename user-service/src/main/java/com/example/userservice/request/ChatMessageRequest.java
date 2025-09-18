package com.example.userservice.request;

import com.example.userservice.entity.ChatMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ChatMessageRequest {
    @NotBlank(message = "Message content is required")
    private String content;

    @NotNull(message = "Chat ID is required")
    private String chatId;

    @Builder.Default
    private ChatMessage.MessageType type = ChatMessage.MessageType.TEXT;

    private String replyToMessageId;

    private String attachmentUrl;

    private String attachmentType;
}
