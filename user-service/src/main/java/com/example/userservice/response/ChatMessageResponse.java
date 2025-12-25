package com.example.userservice.response;

import com.example.userservice.entity.ChatMessage;
import com.example.userservice.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ChatMessageResponse {
    private String id;
    private String content;
    private ChatMessage.MessageType type;
    private EnumStatus status;
    private String chatId;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String replyToMessageId;
    private String replyToContent;
    private String attachmentUrl;
    private String attachmentType;
    private Boolean isEdited;
    private Boolean isDeleted;
    private Boolean isOwnMessage; // true if message is sent by current user
    private Date createdAt;
    private Date updatedAt;
}
