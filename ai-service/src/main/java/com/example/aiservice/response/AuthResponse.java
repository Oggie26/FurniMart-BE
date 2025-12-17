package com.example.aiservice.response;

import com.example.aiservice.enums.EnumRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String id;
    private String email;
    private String password;
    private EnumRole role;
}

