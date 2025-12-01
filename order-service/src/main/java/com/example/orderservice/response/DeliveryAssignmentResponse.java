package com.example.orderservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignmentResponse {
    private Long id;
    private Long orderId;
    private String storeId;
    private String deliveryStaffId;
    private String assignedBy;
    private LocalDateTime assignedAt;
    private LocalDateTime estimatedDeliveryDate;
    private String status; // DeliveryStatus enum as string
    private String notes;
    private Boolean invoiceGenerated;
    private LocalDateTime invoiceGeneratedAt;
    private Boolean productsPrepared;
    private LocalDateTime productsPreparedAt;
    private String rejectReason;
    private LocalDateTime rejectedAt;
    private String rejectedBy;
}

