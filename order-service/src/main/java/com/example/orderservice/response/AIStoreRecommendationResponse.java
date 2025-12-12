package com.example.orderservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIStoreRecommendationResponse {

    private String recommendedStoreId; // Store ID được AI recommend
    private String storeName; // Tên store
    private Double distance; // Khoảng cách (km)
    private Double stockAvailability; // % hàng có sẵn (0.0 - 1.0)
    private Double confidence; // Độ tin cậy của AI (0.0 - 1.0)
    private String reason; // Lý do AI chọn store này
    private List<String> availableProducts; // Danh sách sản phẩm có trong kho
    private List<String> unavailableProducts; // Danh sách sản phẩm KHÔNG có

    // Alternative stores nếu store chính không OK
    private List<AlternativeStore> alternatives;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlternativeStore {
        private String storeId;
        private String storeName;
        private Double distance;
        private Double stockAvailability;
    }
}
