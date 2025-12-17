package com.example.orderservice.response;

import com.example.orderservice.enums.WalletStatus;
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
public class WalletResponse {

    private String id;
    private String code;
    private BigDecimal balance;
    private WalletStatus status;
    private String userId;
    private String userFullName;
    private Date createdAt;
    private Date updatedAt;
}
