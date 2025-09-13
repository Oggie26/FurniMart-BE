package com.example.orderservice.response;

import com.example.orderservice.enums.EnumRole;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String id;
    private String email;
    private String password;
    @Enumerated(EnumType.STRING)
    private EnumRole role;
}
