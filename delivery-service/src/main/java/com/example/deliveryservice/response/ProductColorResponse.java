package com.example.deliveryservice.response;

import com.example.deliveryservice.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductColorResponse {
    private String id;
    private ProductResponse product;
    private ColorResponse color;
    private List<ImageResponse> images;
    private EnumStatus status;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductResponse {
        private String id;
        private String name;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColorResponse {
        private String id;
        private String colorName;
    }
}

