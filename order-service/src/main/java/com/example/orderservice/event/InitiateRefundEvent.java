package com.example.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateRefundEvent {
    private Long orderId;
    private Long warrantyClaimId;
    private String userId; // Customer ID để tìm wallet
    private Double amount;
    private LocalDateTime initiatedAt;
}
