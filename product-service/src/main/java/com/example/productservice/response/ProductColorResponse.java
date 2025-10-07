package com.example.productservice.response;

import com.example.productservice.enums.EnumStatus;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductColorResponse {
    private String id;
    private ProductResponse product;
    private ColorResponse color;
    private List<ImageResponse> images;
    private List<Image3DResponse> models3D;
    private EnumStatus status;
}
