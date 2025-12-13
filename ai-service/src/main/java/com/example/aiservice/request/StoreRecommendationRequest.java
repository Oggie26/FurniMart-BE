package com.example.aiservice.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreRecommendationRequest {

    private Long orderId;
    private List<String> rejectedStoreIds; // Stores đã reject
    private List<OrderItemDTO> orderItems; // Sản phẩm cần mua
    private CustomerAddressDTO customerAddress; // Địa chỉ khách hàng

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO {
        private String productColorId;
        private Integer quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerAddressDTO {
        private Long addressId;
        private Double latitude;
        private Double longitude;
        private String addressLine;
    }
}
