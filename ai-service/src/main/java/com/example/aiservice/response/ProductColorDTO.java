package com.example.aiservice.response;


import com.example.aiservice.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductColorDTO {
    private String id;
    private ColorResponse color;
    private List<ImageResponse> images;
    private List<Image3DResponse> models3D;
    private EnumStatus status;
}

