package com.example.productservice.request;

import com.example.productservice.enums.Enum3DFormat;
import com.example.productservice.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductModel3DRequest {

    private EnumStatus status;

    private String modelUrl;

    private Enum3DFormat format;

    private Double sizeInMb;

    private String previewImage;

    private String productId;

    private String colorId;
}
