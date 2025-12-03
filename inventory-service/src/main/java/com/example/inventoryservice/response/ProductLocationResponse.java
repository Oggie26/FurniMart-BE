package com.example.inventoryservice.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ProductLocationResponse {
    private String productColorId;
    private List<LocationInfo> locations;
    private String storeId;

    @Getter @Setter @Builder
    public static class LocationInfo {
        private String warehouseId;
        private String warehouseName;
        private String storeId;

        private String zoneId;
        private String zoneName;

        private String locationItemId;
        private String locationCode;

        @Builder.Default
        private int totalQuantity = 0;

        @Builder.Default
        private int reserved = 0;

        public int getAvailable() {
            return Math.max(0, totalQuantity - reserved);
        }
    }
}