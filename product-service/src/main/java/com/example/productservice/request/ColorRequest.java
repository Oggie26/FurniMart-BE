package com.example.productservice.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColorRequest {

    @NotBlank(message = "Color name must not be blank")
    @Size(min = 2, max = 50, message = "Color name must be between 2 and 50 characters")
    private String colorName;

    @Pattern(
            regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
            message = "Hex code must be a valid HEX color (e.g. #FFFFFF)"
    )
    private String hexCode;

    private List<ProductImageRequest> imageRequestList;

    private List<ProductModel3DRequest> model3DRequestList;
}
