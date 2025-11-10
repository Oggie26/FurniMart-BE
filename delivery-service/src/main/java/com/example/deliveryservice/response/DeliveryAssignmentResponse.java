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
    private Long orderId;
    private String storeId;
    private String deliveryStaffId;
    private String assignedBy;
    private LocalDateTime assignedAt;
    private LocalDateTime estimatedDeliveryDate;
    private DeliveryStatus status;
    private String notes;
    private Boolean invoiceGenerated;
    private LocalDateTime invoiceGeneratedAt;
    private Boolean productsPrepared;
    private LocalDateTime productsPreparedAt;
    private OrderResponse order;
    private StoreResponse store;
}

