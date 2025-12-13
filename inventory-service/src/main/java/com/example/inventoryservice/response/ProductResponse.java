package com.example.inventoryservice.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String code;
    private String name;
    private Double unitPrice;
    private String thumbnailImage;
    private String categoryName;
    private String brandName;
}
