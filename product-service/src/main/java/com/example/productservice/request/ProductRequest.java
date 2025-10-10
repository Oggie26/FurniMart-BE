package com.example.productservice.request;

import com.example.productservice.enums.EnumStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private Double price;

    @NotBlank(message = "Thumbnail image is required")
    private String thumbnailImage;

    @Positive(message = "Weight must be > 0")
    private Double weight;

    @Positive(message = "Height must be > 0")
    private Double height;

    @Positive(message = "Width must be > 0")
    private Double width;

    @Positive(message = "Length must be > 0")
    private Double length;

    @NotNull(message = "CategoryId is required")
    private Long categoryId;

    @NotEmpty(message = "MaterialIds is required")
    private List<Long> materialIds;

    @Enumerated(EnumType.STRING)
    EnumStatus status; 
    
    private String slug; 

}
