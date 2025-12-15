package com.example.productservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductQuickLookupResponse {
    private String id;
    private String name;
    private Double price;
    private String thumbnailImage;
    private List<ProductColorQuickResponse> colors;
}
