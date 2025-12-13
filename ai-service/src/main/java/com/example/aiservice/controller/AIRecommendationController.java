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
public class    AIRecommendationController {

    private final AIStoreRecommendationService aiService;

    /**
     * AI recommend store tốt nhất cho order
     * Tiêu chí: CÓ ĐỦ HÀNG + GẦN NHẤT
     */

}
