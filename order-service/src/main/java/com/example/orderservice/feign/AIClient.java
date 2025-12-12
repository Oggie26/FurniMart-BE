package com.example.orderservice.feign;

import com.example.orderservice.response.AIStoreRecommendationResponse;
import com.example.orderservice.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "ai-service", url = "${ai.service.url}")
public interface AIClient {

    /**
     * Gọi AI Service để tìm store tốt nhất
     * Input: orderId, rejectedStoreId, orderDetails, customerAddress
     * Output: Recommended store (có đủ hàng + gần nhất)
     */
    @PostMapping("/api/ai/recommend-store")
    ApiResponse<AIStoreRecommendationResponse> recommendStore(@RequestBody Map<String, Object> request);
}
