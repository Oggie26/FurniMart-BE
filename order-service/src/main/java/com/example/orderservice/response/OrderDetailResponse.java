package com.example.orderservice.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDetailResponse {
    private Long id;
    private String productColorId;
    private Integer quantity;
    private Double price;
    private ProductColorResponse productColor; // Enhanced: include product details
}
