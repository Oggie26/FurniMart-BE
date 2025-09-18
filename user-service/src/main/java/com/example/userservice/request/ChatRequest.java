package com.example.userservice.request;

import com.example.userservice.entity.Chat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class ChatRequest {
    @NotBlank(message = "Chat name is required")
    private String name;

    private String description;

    @NotNull(message = "Chat type is required")
    private Chat.ChatType type;

    @NotNull(message = "Participant IDs are required")
    private List<String> participantIds;
}
