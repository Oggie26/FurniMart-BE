package com.example.orderservice.repository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    void delete(CartItem cartItem);
}
