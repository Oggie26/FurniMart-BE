package com.example.orderservice.feign;

import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.StoreResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "user-service", url = "http://localhost:8086", contextId = "storeClient")
public interface StoreClient {

    @GetMapping("/api/stores/{id}")
    ApiResponse<StoreResponse> getStoreById(@PathVariable("id") String id);
}
