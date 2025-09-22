package com.example.userservice.request;

import com.example.userservice.enums.WalletTransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private WalletTransactionType type;

    private String description;

    private String referenceId;

    @NotBlank(message = "Wallet ID is required")
    private String walletId;
}
