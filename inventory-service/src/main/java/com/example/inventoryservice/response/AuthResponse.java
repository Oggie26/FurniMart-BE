package com.example.inventoryservice.response;

import com.example.inventoryservice.enums.EnumRole;
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
    private String email;
    private String password;
    @Enumerated(EnumType.STRING)
    private EnumRole role;
}

