package com.example.orderservice.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItemResponse {
    Long cartItemId;
    String productColorId;
    String productName;
    String image;
    Double price;
    String colorId;
    String colorName;
    int quantity;
    Double totalItemPrice;
}
