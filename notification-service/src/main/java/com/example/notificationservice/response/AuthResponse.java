package com.example.notificationservice.response;

import com.example.notificationservice.enums.EnumRole;
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
    private EnumRole role;
}
