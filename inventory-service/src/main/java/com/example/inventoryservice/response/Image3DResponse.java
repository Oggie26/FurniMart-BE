package com.example.inventoryservice.response;

import com.example.inventoryservice.enums.Enum3DFormat;
import com.example.inventoryservice.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Image3DResponse implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String image3d;
    private EnumStatus status;
    private String modelUrl;
    private Enum3DFormat format;
    private Double sizeInMb;
    private String previewImage;
}
