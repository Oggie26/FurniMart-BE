package com.example.aiservice.feign;

import com.example.aiservice.response.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "store-service")
public interface StoreClient {

    @GetMapping("/api/stores/nearest")
    ApiResponse<List<StoreDistance>> getNearestStores(
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam("limit") Integer limit);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class StoreDistance {
        private StoreInfo store;
        private Double distance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class StoreInfo {
        private String id;
        private String storeName;
        private String address;
        private Double latitude;
        private Double longitude;
    }
}
