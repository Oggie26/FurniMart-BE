package com.example.orderservice.feign;

import com.example.orderservice.response.AIStoreRecommendationResponse;
import com.example.orderservice.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "ai-service")
public interface AIClient {

    @PostMapping("/api/ai/analyze/store/recommend-store")
    ApiResponse<AIStoreRecommendationResponse> recommendStore(@RequestBody Map<String, Object> request);
}
