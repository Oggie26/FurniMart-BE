package com.example.orderservice.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartResponse {
    Long cartId;
    String username;
    List<CartItemResponse> items;
    Double totalPrice;
}
