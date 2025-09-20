package com.example.orderservice.feign;

import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "http://localhost:8087")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ApiResponse<ProductResponse> getProductById(@PathVariable("id") String id);
}
