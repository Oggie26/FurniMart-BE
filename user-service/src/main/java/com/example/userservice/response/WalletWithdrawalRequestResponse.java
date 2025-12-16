package com.example.userservice.response;

import com.example.userservice.enums.WithdrawalRequestStatus;
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
public class WalletWithdrawalRequestResponse {

    private String id;
    private String code;
    private String walletId;
    private String walletCode;
    private String userId;
    private String userFullName;
    private BigDecimal amount;
    private String bankAccountNumber;
    private String bankName;
    private String accountHolderName;
    private String description;
    private WithdrawalRequestStatus status;
    private String rejectionReason;
    private String approvedBy;
    private Date approvedAt;
    private String transactionId;
    private String referenceId;
    private Date createdAt;
    private Date updatedAt;
}
