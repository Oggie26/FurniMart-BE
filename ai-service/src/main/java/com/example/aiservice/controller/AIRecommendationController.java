package com.example.aiservice.controller;

import com.example.aiservice.request.StoreRecommendationRequest;
import com.example.aiservice.response.ApiResponse;
import com.example.aiservice.response.StoreRecommendationResponse;
import com.example.aiservice.service.AIStoreRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Recommendation Controller")
@RequiredArgsConstructor
@Slf4j
public class AIRecommendationController {

    private final AIStoreRecommendationService aiService;

    /**
     * AI recommend store tốt nhất cho order
     * Tiêu chí: CÓ ĐỦ HÀNG + GẦN NHẤT
     */
    @PostMapping("/recommend-store")
    @Operation(summary = "AI recommend best store for order", description = "Tìm store có đủ hàng và gần khách hàng nhất")
    public ApiResponse<StoreRecommendationResponse> recommendStore(
            @RequestBody StoreRecommendationRequest request) {

        log.info("📥 Received AI recommendation request for order {}", request.getOrderId());

        try {
            StoreRecommendationResponse recommendation = aiService.recommendStore(request);

            if (recommendation == null) {
                return ApiResponse.<StoreRecommendationResponse>builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .message("Không tìm thấy store phù hợp")
                        .data(null)
                        .build();
            }

            return ApiResponse.<StoreRecommendationResponse>builder()
                    .status(HttpStatus.OK.value())
                    .message("AI recommendation thành công")
                    .data(recommendation)
                    .build();

        } catch (Exception e) {
            log.error("❌ AI recommendation failed: {}", e.getMessage(), e);

            return ApiResponse.<StoreRecommendationResponse>builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Lỗi AI service: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }

    /**
     * Health check cho AI service
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("AI Service is running")
                .data("OK")
                .build();
    }
}
