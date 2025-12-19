package com.example.aiservice.response;

import com.example.aiservice.enums.EnumRole;
import com.example.aiservice.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private String id;
    private String fullName;
    private Boolean gender;
    private EnumRole role;
    private String email;
    private EnumStatus status;
    private String password;
}
