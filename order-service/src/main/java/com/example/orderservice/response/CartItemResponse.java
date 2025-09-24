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
    String productId;
    String productName;
    String thumbnail;
    Double price;
    String colorId;
    int quantity;
    Double totalItemPrice;
}
