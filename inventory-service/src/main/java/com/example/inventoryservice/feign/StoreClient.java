package com.example.inventoryservice.feign;

import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.StoreResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "user-service", contextId = "storeClient")
public interface StoreClient {

    @GetMapping("/api/stores/{id}")
    ApiResponse<StoreResponse> getStoreById(@PathVariable("id") String id);
}
