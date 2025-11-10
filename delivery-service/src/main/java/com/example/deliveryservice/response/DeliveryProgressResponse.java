package com.example.deliveryservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryProgressResponse {
    private String storeId;
    private String storeName;
    private Long totalAssignments;
    private Long assignedCount;
    private Long preparingCount;
    private Long readyCount;
    private Long inTransitCount;
    private Long deliveredCount;
    private List<DeliveryAssignmentResponse> assignments;
}

