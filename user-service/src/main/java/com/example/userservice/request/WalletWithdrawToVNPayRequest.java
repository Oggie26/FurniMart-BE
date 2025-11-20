package com.example.userservice.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletWithdrawToVNPayRequest {
    
    @NotBlank(message = "Wallet ID is required")
    private String walletId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10000", message = "Minimum withdrawal amount is 10,000 VND")
    @DecimalMax(value = "100000000", message = "Maximum withdrawal amount is 100,000,000 VND")
    private Double amount;
    
    @NotBlank(message = "Bank account number is required")
    @Pattern(regexp = "^[0-9]{8,20}$", message = "Bank account number must be 8-20 digits")
    private String bankAccountNumber;
    
    @NotBlank(message = "Bank name is required")
    private String bankName;
    
    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}

