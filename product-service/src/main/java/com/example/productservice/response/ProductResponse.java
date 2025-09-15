package com.example.productservice.response;

import com.example.productservice.entity.Category;
import com.example.productservice.enums.EnumStatus;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private String description;
    private Double price;
    private String code;
    private String thumbnailImage;
    private String slug;
    private Double weight;
    private Double height;
    private EnumStatus status;
    private Double width;
    private Double length;
    private String categoryName;
    private List<ColorResponse> color;
    private String materialName;
    private List<ImageResponse> images;
    private List<Image3DResponse> images3d;

}
