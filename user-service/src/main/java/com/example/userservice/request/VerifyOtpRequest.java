package com.example.userservice.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyOtpRequest {
    
    @NotBlank(message = "OTP code must not be blank")
    @Size(min = 6, max = 6, message = "OTP code must be 6 digits")
    private String otpCode;
}

