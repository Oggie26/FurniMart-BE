package com.example.userservice.response;

import com.example.userservice.entity.ChatParticipant;
import com.example.userservice.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ChatParticipantResponse {
    private String id;
    private String chatId;
    private String userId;
    private String userName;
    private String userAvatar;
    private ChatParticipant.ParticipantRole role;
    private EnumStatus status;
    private LocalDateTime lastReadAt;
    private Boolean isMuted;
    private Boolean isPinned;
}
