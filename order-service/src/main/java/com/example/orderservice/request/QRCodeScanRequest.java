package com.example.orderservice.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRCodeScanRequest {
    
    @NotBlank(message = "QR code cannot be blank")
    private String qrCode;
    
    private String customerSignature; // Base64 encoded signature
}
