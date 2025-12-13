package com.example.orderservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ColorResponse implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String colorName;
    private String hexCode;
    private List<ImageResponse> images;
    private List<Image3DResponse> models3D;
}
