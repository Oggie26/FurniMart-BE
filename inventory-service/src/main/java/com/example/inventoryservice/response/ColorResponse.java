package com.example.inventoryservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ColorResponse {
    private String id;
    private String colorName;
    private String hexCode;
    private List<ImageResponse> images;
    private List<Image3DResponse> models3D;
}
