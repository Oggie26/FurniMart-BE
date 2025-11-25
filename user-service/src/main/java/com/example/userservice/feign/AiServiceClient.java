package com.example.userservice.feign;

import com.example.userservice.config.FeignClientInterceptor;
import com.example.userservice.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "ai-service",
    configuration = FeignClientInterceptor.class
)
public interface AiServiceClient {
    
    @PostMapping("/api/ai/chat")
    ApiResponse<String> chat(@RequestBody ChatRequest request);
    
    record ChatRequest(String message) {}
}

