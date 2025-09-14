package com.example.inventoryservice.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse {
    private String id;
    private String code;
    private String name;
    private Double unitPrice;
    private String thumbnailImage;
    private String categoryName;
    private String brandName;
}
