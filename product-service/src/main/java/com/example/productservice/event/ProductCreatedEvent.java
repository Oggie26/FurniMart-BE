package com.example.productservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEvent {
    private String productId;
    private String name;
    private String code;
    private Double price;
    private String description;
    private String categoryName;
//    private String thumbnailImage;
//    private Double weight;
//    private Double height;
//    private Double width;
//    private Double length;
//    private List<ProductColorDTO> productColors;
//    private LocalDateTime createdAt;
//    private List<MaterialResponse> materials;
}

