package com.example.orderservice.service.inteface;

import com.example.orderservice.response.CartResponse;

import java.util.List;

public interface CartService {
    void addProductToCart(String productId, Integer quantity);
    void deleteProductFromCart(String productId);
    void removeProductFromCart(List<String> productId);
    CartResponse getCart();
    void updateProductQuantityInCart(String productId,  Integer quantity);
}
