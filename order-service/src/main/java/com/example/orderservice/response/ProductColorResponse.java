package com.example.orderservice.response;

import com.example.orderservice.enums.EnumStatus;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductColorResponse implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private ProductResponse product;
    private ColorResponse color;
    private List<ImageResponse> images;
    private List<Image3DResponse> models3D;
    private EnumStatus status;
}
