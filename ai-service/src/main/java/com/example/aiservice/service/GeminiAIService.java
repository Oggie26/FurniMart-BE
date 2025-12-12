package com.example.aiservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAIService {

    private final ChatModel chatModel;

    public GeminiDecision askGemini(List<StoreCandidate> candidates, OrderContext context) {
        if (candidates.isEmpty()) {
            return null;
        }

        String prompt = buildPrompt(candidates, context);

        log.info("🤖 Calling Gemini AI with {} candidates", candidates.size());
        log.debug("Prompt: {}", prompt);

        try {
            String geminiResponse = chatModel.call(prompt);

            log.info("✨ Gemini response: {}", geminiResponse);

            return parseGeminiResponse(geminiResponse, candidates);

        } catch (Exception e) {
            log.error("❌ Gemini AI error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Build prompt cho Gemini
     */
    private String buildPrompt(List<StoreCandidate> candidates, OrderContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Bạn là AI assistant chuyên về logistics và quản lý chuỗi cung ứng.\n\n");

        prompt.append("📦 THÔNG TIN ĐƠN HÀNG:\n");
        prompt.append(String.format("- Sản phẩm: %s\n", context.getProductNames()));
        prompt.append(String.format("- Địa chỉ giao hàng: %s\n", context.getCustomerAddress()));
        prompt.append(String.format("- Số lượng sản phẩm: %d items\n\n", context.getTotalItems()));

        prompt.append("🏪 CÁC CỬA HÀNG KHẢ DỤNG:\n\n");
        for (int i = 0; i < candidates.size(); i++) {
            StoreCandidate store = candidates.get(i);
            prompt.append(String.format("%d. %s\n", i + 1, store.getStoreName()));
            prompt.append(String.format("   - ID: %s\n", store.getStoreId()));
            prompt.append(String.format("   - Khoảng cách: %.1f km\n", store.getDistance()));
            prompt.append(String.format("   - Có đủ hàng: %.0f%%\n", store.getStockAvailability() * 100));
            prompt.append(String.format("   - Điểm tổng hợp: %d/100\n", store.getScore()));
            prompt.append(String.format("   - Sản phẩm có sẵn: %d/%d\n\n",
                    store.getAvailableProductCount(),
                    context.getTotalItems()));
        }

        prompt.append("🎯 YÊU CẦU:\n");
        prompt.append("Phân tích các yếu tố:\n");
        prompt.append("1. Độ khả dụng hàng hóa (cao nhất ưu tiên)\n");
        prompt.append("2. Khoảng cách giao hàng (gần nhất tốt hơn)\n");
        prompt.append("3. Tổng hợp điểm đánh giá\n");
        prompt.append("4. Trải nghiệm khách hàng (giao nhanh vs đầy đủ)\n\n");

        prompt.append("Hãy chọn CỬA HÀNG TỐT NHẤT và giải thích ngắn gọn lý do.\n\n");

        prompt.append("📝 FORMAT TRẢ LỜI (CHÍNH XÁC):\n");
        prompt.append("STORE_ID: [ID cửa hàng bạn chọn]\n");
        prompt.append("REASON: [Lý do ngắn gọn, 1-2 câu]\n");

        return prompt.toString();
    }

    private GeminiDecision parseGeminiResponse(String response, List<StoreCandidate> candidates) {
        try {
            String storeId = null;
            String reason = "";

            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.startsWith("STORE_ID:")) {
                    storeId = line.replace("STORE_ID:", "").trim();
                } else if (line.startsWith("REASON:")) {
                    reason = line.replace("REASON:", "").trim();
                }
            }

            if (storeId != null) {
                String finalStoreId = storeId;
                boolean validStore = candidates.stream()
                        .anyMatch(c -> c.getStoreId().equals(finalStoreId));

                if (validStore) {
                    return new GeminiDecision(storeId, reason);
                }
            }

            log.warn("⚠️ Gemini response không hợp lệ, fallback về top candidate");
            return new GeminiDecision(
                    candidates.get(0).getStoreId(),
                    "AI fallback: Chọn store điểm cao nhất");

        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            return null;
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    public static class StoreCandidate {
        private String storeId;
        private String storeName;
        private Double distance;
        private Double stockAvailability;
        private Integer score;
        private Integer availableProductCount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    public static class OrderContext {
        private String productNames;
        private String customerAddress;
        private Integer totalItems;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class GeminiDecision {
        private String recommendedStoreId;
        private String aiReasoning;
    }
}
