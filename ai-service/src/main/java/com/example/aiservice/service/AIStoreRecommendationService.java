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
    private final GeminiAIService geminiAIService;

    public StoreRecommendationResponse recommendStore(StoreRecommendationRequest request) {
        log.info("🤖 AI-POWERED analyzing order {} to find best store", request.getOrderId());

        List<StoreClient.StoreDistance> nearbyStores = getNearbyStores(
                request.getCustomerAddress().getLatitude(),
                request.getCustomerAddress().getLongitude(),
                10 // Top 10 stores - let AI decide
        );

        if (nearbyStores == null || nearbyStores.isEmpty()) {
            log.warn("❌ Không tìm thấy store nào gần khách hàng");
            return null;
        }

        List<StoreClient.StoreDistance> candidates = nearbyStores.stream()
                .filter(sd -> !request.getRejectedStoreIds().contains(sd.getStore().getId()))
                .collect(Collectors.toList());

        log.info("📊 Found {} candidate stores (after filtering {} rejected)",
                candidates.size(), request.getRejectedStoreIds().size());

        if (candidates.isEmpty()) {
            log.warn("❌ Không có store nào sau khi filter rejected");
            return null;
        }

        // ✨ GEMINI AI: Analyze ALL candidates & decide
        List<GeminiAIService.StoreCandidate> aiCandidates = new ArrayList<>();

        for (StoreClient.StoreDistance candidate : candidates) {
            // Check inventory for this store
            int availableCount = 0;
            for (StoreRecommendationRequest.OrderItemDTO item : request.getOrderItems()) {
                try {
                    ApiResponse<Boolean> stockCheck = inventoryClient.checkStockAtStore(
                            item.getProductColorId(),
                            candidate.getStore().getId(),
                            item.getQuantity());
                    if (stockCheck != null && stockCheck.getData() != null && stockCheck.getData()) {
                        availableCount++;
                    }
                } catch (Exception e) {
                    log.warn("Error checking stock at {}: {}",
                            candidate.getStore().getId(), e.getMessage());
                }
            }

            double stockAvailability = (double) availableCount / request.getOrderItems().size();

            // Add to AI candidates (AI sẽ tự quyết định threshold)
            aiCandidates.add(GeminiAIService.StoreCandidate.builder()
                    .storeId(candidate.getStore().getId())
                    .storeName(candidate.getStore().getStoreName())
                    .distance(candidate.getDistance())
                    .stockAvailability(stockAvailability)
                    .score(0) // AI không cần score từ rule-based
                    .availableProductCount(availableCount)
                    .build());
        }

        if (aiCandidates.isEmpty()) {
            log.warn("❌ Không có candidate nào có data đầy đủ");
            return null;
        }

        // Prepare context cho AI
        GeminiAIService.OrderContext context = GeminiAIService.OrderContext.builder()
                .productNames(request.getOrderItems().stream()
                        .map(StoreRecommendationRequest.OrderItemDTO::getProductColorId)
                        .collect(Collectors.joining(", ")))
                .customerAddress(request.getCustomerAddress().getAddressLine())
                .totalItems(request.getOrderItems().size())
                .build();

        // ✨ GEMINI AI - Make the decision
        log.info("🤖 Asking Gemini AI to analyze {} stores...", aiCandidates.size());
        GeminiAIService.GeminiDecision geminiDecision = geminiAIService.askGemini(aiCandidates, context);

        if (geminiDecision == null) {
            log.warn("❌ Gemini AI không thể đưa ra quyết định");
            return null;
        }

        // Find the chosen store
        GeminiAIService.StoreCandidate chosenStore = aiCandidates.stream()
                .filter(s -> s.getStoreId().equals(geminiDecision.getRecommendedStoreId()))
                .findFirst()
                .orElse(null);

        if (chosenStore == null) {
            log.warn("❌ Gemini chọn store không hợp lệ: {}", geminiDecision.getRecommendedStoreId());
            return null;
        }

        log.info("✅ Gemini AI chose: {} - {}",
                chosenStore.getStoreId(), geminiDecision.getAiReasoning());

        // Build alternatives (những store AI không chọn)
        List<StoreRecommendationResponse.AlternativeStore> alternatives = aiCandidates.stream()
                .filter(s -> !s.getStoreId().equals(chosenStore.getStoreId()))
                .limit(3)
                .map(alt -> StoreRecommendationResponse.AlternativeStore.builder()
                        .storeId(alt.getStoreId())
                        .storeName(alt.getStoreName())
                        .distance(alt.getDistance())
                        .stockAvailability(alt.getStockAvailability())
                        .score(0)
                        .build())
                .collect(Collectors.toList());

        return StoreRecommendationResponse.builder()
                .recommendedStoreId(chosenStore.getStoreId())
                .storeName(chosenStore.getStoreName())
                .distance(chosenStore.getDistance())
                .stockAvailability(chosenStore.getStockAvailability())
                .confidence(0.95) // Gemini AI high confidence
                .score(100) // Symbolic - AI made the decision
                .reason("🤖 Gemini AI Decision: " + geminiDecision.getAiReasoning())
                .productDetails(null) // Not needed with AI
                .alternatives(alternatives)
                .build();
    }

    private ScoredStore scoreStore(StoreClient.StoreDistance candidate,
            List<StoreRecommendationRequest.OrderItemDTO> orderItems) {
        String storeId = candidate.getStore().getId();
        double distance = candidate.getDistance();

        int score = 0;
        List<StoreRecommendationResponse.ProductAvailability> productDetails = new ArrayList<>();
        int availableCount = 0;

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

        if (stockAvailability < 0.8) {
            return null;
        }

        score += (int) (stockAvailability * 50);

        if (distance < 5) {
            score += 30;
        } else if (distance < 10) {
            score += 20;
        } else if (distance < 20) {
            score += 10;
        }

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
