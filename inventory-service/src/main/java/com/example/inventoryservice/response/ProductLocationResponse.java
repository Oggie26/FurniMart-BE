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

    @Getter @Setter @Builder
    public static class LocationInfo {
        private String warehouseId;
        private String warehouseName;

        private String zoneId;
        private String zoneName;

        private String locationItemId;
        private String locationCode;

        private Integer totalQuantity;
        private Integer reserved;

        public Integer getAvailable() {
            return totalQuantity - reserved;
        }
    }
}
