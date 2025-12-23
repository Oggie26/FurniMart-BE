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
public class WarrantyClaimRejectedEvent {
    private Long claimId;
    private Long orderId;
    private String customerId;
    private String rejectionReason; // adminResponse hoặc resolutionNotes
    private LocalDateTime rejectedAt;
    private String rejectedBy; // Admin/Manager ID
}
