
package com.example.aiservice.response;

import com.example.aiservice.enums.Enum3DFormat;
import com.example.aiservice.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Image3DResponse {
    private String image3d;
    private EnumStatus status;
    private String modelUrl;
    private Enum3DFormat format;
    private Double sizeInMb;
    private String previewImage;
}
