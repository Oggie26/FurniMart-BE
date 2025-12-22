package com.example.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReturnedEvent {
    private Long orderId;
    private Long warrantyClaimId;
    private String storeId;
    private LocalDateTime returnedAt;
    private String confirmedBy; // Warehouse staff ID
    private List<ReturnedItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnedItem {
        private String productColorId;
        private Integer quantity;
        private String reason; // Warranty reason
    }
}
