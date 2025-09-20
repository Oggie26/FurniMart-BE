package com.example.userservice.request;

import com.example.userservice.enums.WalletStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotNull(message = "Balance is required")
    @PositiveOrZero(message = "Balance must be positive or zero")
    private BigDecimal balance;

    @NotNull(message = "Status is required")
    private WalletStatus status;

    @NotBlank(message = "User ID is required")
    private String userId;
}
