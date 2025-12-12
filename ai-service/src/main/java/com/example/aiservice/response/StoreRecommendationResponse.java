package com.example.aiservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreRecommendationResponse {

    private String recommendedStoreId; // Store được AI recommend
    private String storeName; // Tên store
    private Double distance; // Khoảng cách (km)
    private Double stockAvailability; // % hàng có sẵn (0.0 - 1.0)
    private Double confidence; // Độ tin cậy (0.0 - 1.0)
    private String reason; // Lý do AI chọn store này
    private Integer score; // Điểm tổng hợp

    // Chi tiết inventory
    private List<ProductAvailability> productDetails;

    // Alternative stores
    private List<AlternativeStore> alternatives;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductAvailability {
        private String productColorId;
        private Boolean available;
        private Integer availableQuantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlternativeStore {
        private String storeId;
        private String storeName;
        private Double distance;
        private Double stockAvailability;
        private Integer score;
    }
}
