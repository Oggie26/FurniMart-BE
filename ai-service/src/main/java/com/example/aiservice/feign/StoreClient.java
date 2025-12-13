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

@FeignClient(name = "user-service")
public interface StoreClient {

    @GetMapping("/api/stores/nearest/list")
    ApiResponse<List<StoreDistance>> getNearestStores(
            @RequestParam("lat") Double latitude,
            @RequestParam("lon") Double longitude,
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
        @com.fasterxml.jackson.annotation.JsonProperty("name")
        private String storeName;
        @com.fasterxml.jackson.annotation.JsonProperty("addressLine")
        private String address;
        private Double latitude;
        private Double longitude;
    }
}
