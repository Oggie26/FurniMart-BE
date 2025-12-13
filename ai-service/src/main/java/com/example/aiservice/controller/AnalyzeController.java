package com.example.aiservice.controller;

import com.example.aiservice.request.StoreRecommendationRequest;
import com.example.aiservice.response.ApiResponse;
import com.example.aiservice.response.InteriorDesignResponse;
import com.example.aiservice.response.StoreRecommendationResponse;
import com.example.aiservice.service.AIStoreRecommendationService;
import com.example.aiservice.service.AnalyzeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api/ai/analyze")
@Slf4j
@RestController
@RequiredArgsConstructor
public class AnalyzeController {

    private final AnalyzeService analyzeService;
    private final AIStoreRecommendationService aiService;

    @PostMapping(value = "/analyze-room", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InteriorDesignResponse>> analyzeRoom(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "note", required = false) String userNote
    ) {
        return ResponseEntity.ok(
                ApiResponse.<InteriorDesignResponse>builder()
                        .status(200)
                        .message("Room analyzed successfully")
                        .data(analyzeService.analyzeRoom(image, userNote))
                        .build());
    }

    @PostMapping("/store/recommend-store")
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


    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("AI Service is running")
                .data("OK")
                .build();
    }

}
