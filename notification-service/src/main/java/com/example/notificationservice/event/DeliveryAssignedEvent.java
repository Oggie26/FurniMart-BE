package com.example.notificationservice.event;

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
public class DeliveryAssignedEvent {
    private Long orderId;
    private String email;
    private String fullName;
    private String deliveryStaffId;
    private LocalDateTime assignedAt;
    private LocalDateTime estimatedDeliveryDate;
    private Double totalAmount;
    private String storeId;
    private String storeName;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String productColorId;
        private String productName;
        private Double price;
        private int quantity;
        private String colorName;
    }
}
