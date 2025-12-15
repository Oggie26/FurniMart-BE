package com.example.productservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductColorQuickResponse {
    private String id; // productColorId
    private String colorName;
    private String imageUrl;
    private Integer currentStock;
}
