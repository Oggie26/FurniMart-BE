package com.example.userservice.response;

import com.example.userservice.enums.WalletTransactionStatus;
import com.example.userservice.enums.WalletTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionAdminResponse {
    private String id;
    private String code;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private BigDecimal amount;
    private WalletTransactionStatus status;
    private WalletTransactionType type;
    private String description;
    private String referenceId;
    private String walletId;
    private String walletCode;
    private String userId;
    private String userName;
    private String email;
    private Date createdAt;
}
