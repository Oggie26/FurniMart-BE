package com.example.orderservice.feign;

import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service",  contextId = "colorClient")
public interface ColorClient {

    @GetMapping("/api/products/{productId}/color/{colorId}")
    ApiResponse<ProductResponse> getProductByColorId(@PathVariable String productId, @PathVariable String colorId);
}
