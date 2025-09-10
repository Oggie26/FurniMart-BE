package com.example.userservice.response;

import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String id;
    private String fullName;
    private Boolean gender;
    @Enumerated(EnumType.STRING)
    private EnumRole role;
    @Enumerated(EnumType.STRING)
    private String email;
    private EnumStatus status;
    private String password;
}

