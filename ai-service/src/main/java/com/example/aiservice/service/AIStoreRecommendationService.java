package com.example.aiservice.service;

import com.example.aiservice.feign.InventoryClient;
import com.example.aiservice.feign.StoreClient;
import com.example.aiservice.request.StoreRecommendationRequest;
import com.example.aiservice.response.ApiResponse;
import com.example.aiservice.response.StoreRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIStoreRecommendationService {

    private final StoreClient storeClient;
    private final InventoryClient inventoryClient;

    /**
     * AI Algorithm: Tìm store tốt nhất dựa trên:
     * 1. Khoảng cách (gần nhất)
     * 2. Inventory availability (có đủ hàng)
     * 3. Loại bỏ stores đã reject
     */
    public StoreRecommendationResponse recommendStore(StoreRecommendationRequest request) {
        log.info("🤖 AI analyzing order {} to find best store", request.getOrderId());

        // 1. Lấy danh sách stores gần
        List<StoreClient.StoreDistance> nearbyStores = getNearbyStores(
                request.getCustomerAddress().getLatitude(),
                request.getCustomerAddress().getLongitude(),
                20 // Top 20 stores
        );

        if (nearbyStores == null || nearbyStores.isEmpty()) {
            log.warn("❌ Không tìm thấy store nào gần khách hàng");
            return null;
        }

        // 2. Filter: Loại bỏ stores đã reject
        List<StoreClient.StoreDistance> candidates = nearbyStores.stream()
                .filter(sd -> !request.getRejectedStoreIds().contains(sd.getStore().getId()))
                .collect(Collectors.toList());

        log.info("📊 Found {} candidate stores (after filtering {} rejected)",
                candidates.size(), request.getRejectedStoreIds().size());

        // 3. Score từng store
        List<ScoredStore> scoredStores = new ArrayList<>();

        for (StoreClient.StoreDistance candidate : candidates) {
            ScoredStore scored = scoreStore(candidate, request.getOrderItems());
            if (scored != null) {
                scoredStores.add(scored);
            }
        }

        if (scoredStores.isEmpty()) {
            log.warn("❌ Không có store nào đủ điều kiện");
            return null;
        }

        // 4. Sắp xếp theo score (cao nhất trước)
        scoredStores.sort(Comparator.comparingInt(ScoredStore::getScore).reversed());

        // 5. Chọn store tốt nhất
        ScoredStore best = scoredStores.get(0);

        log.info("✅ AI recommend: {} (score: {}, distance: {}km, stock: {}%)",
                best.getStoreId(), best.getScore(), best.getDistance(),
                best.getStockAvailability() * 100);

        // 6. Build response
        return StoreRecommendationResponse.builder()
                .recommendedStoreId(best.getStoreId())
                .storeName(best.getStoreName())
                .distance(best.getDistance())
                .stockAvailability(best.getStockAvailability())
                .confidence(calculateConfidence(best.getScore()))
                .score(best.getScore())
                .reason(generateReason(best))
                .productDetails(best.getProductDetails())
                .alternatives(buildAlternatives(scoredStores.subList(1, Math.min(4, scoredStores.size()))))
                .build();
    }

    /**
     * Score 1 store dựa trên nhiều tiêu chí
     */
    private ScoredStore scoreStore(StoreClient.StoreDistance candidate,
            List<StoreRecommendationRequest.OrderItemDTO> orderItems) {
        String storeId = candidate.getStore().getId();
        double distance = candidate.getDistance();

        int score = 0;
        List<StoreRecommendationResponse.ProductAvailability> productDetails = new ArrayList<>();
        int availableCount = 0;

        // Check inventory cho từng sản phẩm
        for (StoreRecommendationRequest.OrderItemDTO item : orderItems) {
            try {
                ApiResponse<Boolean> stockCheck = inventoryClient.checkStockAtStore(
                        item.getProductColorId(),
                        storeId,
                        item.getQuantity());

                boolean available = stockCheck != null &&
                        stockCheck.getData() != null &&
                        stockCheck.getData();

                productDetails.add(StoreRecommendationResponse.ProductAvailability.builder()
                        .productColorId(item.getProductColorId())
                        .available(available)
                        .build());

                if (available) {
                    availableCount++;
                }
            } catch (Exception e) {
                log.warn("Error checking stock for {} at {}: {}",
                        item.getProductColorId(), storeId, e.getMessage());
                return null; // Skip store nếu không check được inventory
            }
        }

        double stockAvailability = (double) availableCount / orderItems.size();

        // Chỉ xét stores có >= 80% hàng
        if (stockAvailability < 0.8) {
            return null;
        }

        // === SCORING ALGORITHM ===

        // 1. Stock availability (0-50 điểm)
        score += (int) (stockAvailability * 50);

        // 2. Distance (0-30 điểm) - Càng gần càng cao
        if (distance < 5) {
            score += 30;
        } else if (distance < 10) {
            score += 20;
        } else if (distance < 20) {
            score += 10;
        }

        // 3. Bonus: 100% stock (20 điểm)
        if (stockAvailability == 1.0) {
            score += 20;
        }

        return ScoredStore.builder()
                .storeId(storeId)
                .storeName(candidate.getStore().getStoreName())
                .distance(distance)
                .stockAvailability(stockAvailability)
                .score(score)
                .productDetails(productDetails)
                .build();
    }

    private List<StoreClient.StoreDistance> getNearbyStores(Double lat, Double lon, int limit) {
        try {
            ApiResponse<List<StoreClient.StoreDistance>> response = storeClient.getNearestStores(lat, lon, limit);
            return response != null ? response.getData() : null;
        } catch (Exception e) {
            log.error("Error getting nearby stores: {}", e.getMessage());
            return null;
        }
    }

    private double calculateConfidence(int score) {
        // Score max = 100 → confidence = 1.0
        return Math.min(1.0, score / 100.0);
    }

    private String generateReason(ScoredStore store) {
        StringBuilder reason = new StringBuilder();

        if (store.getStockAvailability() == 1.0) {
            reason.append("✅ Có đủ 100% hàng. ");
        } else {
            reason.append(String.format("✅ Có %.0f%% hàng. ", store.getStockAvailability() * 100));
        }

        if (store.getDistance() < 5) {
            reason.append("📍 Rất gần khách hàng (< 5km). ");
        } else if (store.getDistance() < 10) {
            reason.append("📍 Gần khách hàng (< 10km). ");
        }

        reason.append(String.format("🎯 Điểm tổng hợp: %d/100", store.getScore()));

        return reason.toString();
    }

    private List<StoreRecommendationResponse.AlternativeStore> buildAlternatives(List<ScoredStore> alternatives) {
        return alternatives.stream()
                .map(alt -> StoreRecommendationResponse.AlternativeStore.builder()
                        .storeId(alt.getStoreId())
                        .storeName(alt.getStoreName())
                        .distance(alt.getDistance())
                        .stockAvailability(alt.getStockAvailability())
                        .score(alt.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    // Helper class
    @lombok.Data
    @lombok.Builder
    private static class ScoredStore {
        private String storeId;
        private String storeName;
        private Double distance;
        private Double stockAvailability;
        private Integer score;
        private List<StoreRecommendationResponse.ProductAvailability> productDetails;
    }
}
