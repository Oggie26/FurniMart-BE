package com.example.deliveryservice.response;

import com.example.deliveryservice.enums.DeliveryStatus;
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
    private String storeId;
    private String storeName;
    private String deliveryStaffId;
    private String assignedBy;
    private LocalDateTime assignedAt;
    private LocalDateTime estimatedDeliveryDate;
    private DeliveryStatus status;
    private String notes;
    private Boolean productsPrepared;
    private LocalDateTime productsPreparedAt;
    private String rejectReason;
    private LocalDateTime rejectedAt;
    private String rejectedBy;
    private OrderResponse order;
}

