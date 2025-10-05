package com.example.productservice.request;

import com.example.productservice.enums.EnumStatus;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductColorRequest {
    private String productId;
    private String colorId;

    private EnumStatus status;

    private List<ProductImageRequest> imageRequests;
    private List<ProductModel3DRequest> model3DRequests;
}
