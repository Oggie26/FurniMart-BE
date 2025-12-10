package com.example.orderservice.repository;

import com.example.orderservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    void delete(@NonNull CartItem cartItem);
    List<CartItem> findByCartId(Long id);
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") Long cartId);

}
