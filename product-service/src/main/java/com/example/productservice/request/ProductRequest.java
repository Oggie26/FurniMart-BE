package com.example.productservice.request;

import com.example.productservice.enums.EnumStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product code is required")
    @Size(max = 50, message = "Product code must be at most 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Product code can only contain letters, numbers, underscores, and dashes")
    private String code;

    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Cost price must be greater than 0")
    private Double costPrice;

    @NotNull(message = "Sell price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Sell price must be greater than 0")
    private Double sellPrice;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    private Double unitPrice;

    @NotNull(message = "Quantity per box is required")
    @Min(value = 1, message = "Quantity per box must be at least 1")
    private Integer quantityPerBox;

    @NotNull(message = "Status is required")
    private EnumStatus status;


    @NotBlank(message = "Thumbnail image URL is required")
    @Size(max = 255, message = "Thumbnail image URL must be at most 255 characters")
    private String thumbnailImage;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Brand ID is required")
    private String brandId;

}
