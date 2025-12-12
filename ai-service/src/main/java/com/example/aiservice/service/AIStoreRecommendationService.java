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

    private List<StoreClient.StoreDistance> getNearbyStores(Double lat, Double lon, int limit) {
        try {
            ApiResponse<List<StoreClient.StoreDistance>> response = storeClient.getNearestStores(lat, lon, limit);
            return response != null ? response.getData() : null;
        } catch (Exception e) {
            log.error("Error getting nearby stores: {}", e.getMessage());
            return null;
        }
    }

}
