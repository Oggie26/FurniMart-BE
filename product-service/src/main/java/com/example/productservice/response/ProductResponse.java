package com.example.productservice.response;

import com.example.productservice.enums.EnumStatus;
import com.example.productservice.enums.EnumUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private Double costPrice;
    private Double sellPrice;
    private Double unitPrice;
    private Integer quantityPerBox;
    private EnumStatus status;
    private EnumUnit unit;
    private List<ImageProductResponse> images;
    private String slug;
    private String thumbnailImage;
    private String categoryName;
    private String brandName;
}
