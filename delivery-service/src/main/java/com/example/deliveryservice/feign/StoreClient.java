package com.example.deliveryservice.feign;

import com.example.deliveryservice.response.ApiResponse;
import com.example.deliveryservice.response.StoreDistanceResponse;
import com.example.deliveryservice.response.StoreResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", contextId = "storeClient")
public interface StoreClient {

    @GetMapping("/api/stores/{id}")
    ApiResponse<StoreResponse> getStoreById(@PathVariable("id") String id);

    @GetMapping("/api/stores")
    ApiResponse<List<StoreResponse>> getAllStores();

    @GetMapping("/api/stores/nearest/list")
    ApiResponse<List<StoreDistanceResponse>> getNearestStores(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5") int limit);
}

