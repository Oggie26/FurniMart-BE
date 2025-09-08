package com.example.userservice.event;

import com.example.userservice.enums.EnumRole;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCreatedEvent {
    private String authId;
    @Enumerated(EnumType.STRING)
    private EnumRole role;
}
