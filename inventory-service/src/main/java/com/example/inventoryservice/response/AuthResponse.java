package com.example.inventoryservice.response;

import com.example.inventoryservice.enums.EnumRole;
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
    private String email;
    private String password;
    private EnumRole role;
}
