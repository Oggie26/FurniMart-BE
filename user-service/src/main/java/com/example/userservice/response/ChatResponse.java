package com.example.userservice.response;

import com.example.userservice.entity.Chat;
import com.example.userservice.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ChatResponse {
    private String id;
    private String name;
    private String description;
    private Chat.ChatType type;
    private EnumStatus status;
    private String createdById;
    private String createdByName;
    private Date createdAt;
    private Date updatedAt;
    private List<ChatParticipantResponse> participants;
    private ChatMessageResponse lastMessage;
    private Long unreadCount;
    private Boolean isMuted;
    private Boolean isPinned;
    
    // New fields for AI chat to staff flow
    private Chat.ChatMode chatMode;
    private String assignedStaffId;
    private String assignedStaffName;
    private LocalDateTime staffRequestedAt;
    private LocalDateTime staffChatEndedAt;
}
