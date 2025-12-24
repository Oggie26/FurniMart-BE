package com.example.orderservice.event;

import com.example.orderservice.enums.WarrantyActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyClaimApprovedEvent {
    private Long claimId;
    private Long orderId;
    private String customerId;
    private Long addressId;
    private WarrantyActionType actionType;
    private Double refundAmount;
    private Double repairCost;
    private String exchangeProductColorId;
    private LocalDateTime approvedAt;
    private String approvedBy; // Admin/Manager ID
}
