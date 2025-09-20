package com.example.productservice.request;

import com.example.productservice.enums.EnumStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaterialRequest {

    @NotBlank(message = "Material name must not be blank")
    @Size(min = 2, max = 100, message = "Material name must be between 2 and 100 characters")
    private String materialName;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @Enumerated(EnumType.STRING)
    private EnumStatus status;

    private String image;
}
