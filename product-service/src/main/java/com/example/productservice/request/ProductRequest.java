package com.example.productservice.request;

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

    private List<ColorRequest> colorRequests;


}
