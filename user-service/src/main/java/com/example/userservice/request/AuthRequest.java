package com.example.userservice.request;

import com.example.userservice.enums.EnumRole;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthRequest {
    private String username;
    private String password;
}
