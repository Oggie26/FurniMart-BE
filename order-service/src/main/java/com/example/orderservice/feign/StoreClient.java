package com.example.orderservice.feign;

import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.StoreDistance;
import com.example.orderservice.response.StoreResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@FeignClient(name = "user-service", contextId = "storeClient")
public interface StoreClient {

    @GetMapping("/api/stores/{id}")
    ApiResponse<StoreResponse> getStoreById(@PathVariable("id") String id);

    @GetMapping("/api/stores/nearest/list")
     ApiResponse<List<StoreDistance>> getNearestStores(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5") int limit);

    @GetMapping("/api/stores/count/active")
    ApiResponse<Long> getActiveStoresCount();

    @GetMapping("/api/stores")
    ApiResponse<List<StoreResponse>> getAllStores();
}
