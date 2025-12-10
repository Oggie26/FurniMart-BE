package com.example.productservice.response;

import com.example.productservice.enums.EnumStatus;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private String description;
    private Double price;
    private String code;
    private String thumbnailImage;
    private Double weight;
    private Double height;
    private Double width;
    private Double length;
    private String categoryName;
    private String userId;
    private String fullName;
    private Long categoryId;
    private String slug;
    private EnumStatus status;
    private List<ProductColorDTO> productColors;
    private List<MaterialResponse> materials;
}
