package com.example.userservice.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequest {
    
    @NotBlank(message = "Token must not be blank")
    private String token;
    
    @NotBlank(message = "New password must not be blank")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String newPassword;
}

