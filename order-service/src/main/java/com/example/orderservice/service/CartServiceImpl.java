package com.example.orderservice.service;

import com.example.orderservice.entity.Cart;
import com.example.orderservice.entity.CartItem;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.AuthClient;
import com.example.orderservice.feign.ProductClient;
import com.example.orderservice.feign.UserClient;
import com.example.orderservice.repository.CartItemRepository;
import com.example.orderservice.repository.CartRepository;
import com.example.orderservice.response.*;
import com.example.orderservice.service.inteface.CartService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AuthClient authClient;
    private final ProductClient productClient;
    private final UserClient userClient;

    @Override
    @Transactional
    public void addProductToCart(String productId, Integer quantity) {
        String userId = getUserId();
        Cart cart = getOrCreateCartEntity(userId);

        ProductResponse product = getProductById(productId);

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + quantity);
        } else {
            addNewItemToCart(cart, product, quantity);
        }

        cart.updateTotalPrice();
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void deleteProductFromCart(String productId) {
        String userId = getUserId();
        Cart cart = getOrCreateCartEntity(userId);

        CartItem itemToRemove = cart.getItems().stream()
                .filter(cartItem -> cartItem.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        cart.getItems().remove(itemToRemove);
        cartItemRepository.delete(itemToRemove);

        cart.updateTotalPrice();
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void removeProductFromCart(List<String> productIds) {
        String userId = getUserId();
        Cart cart = getOrCreateCartEntity(userId);

        List<CartItem> itemsToRemove = cart.getItems().stream()
                .filter(cartItem -> productIds.contains(cartItem.getProductId()))
                .toList();

        if (itemsToRemove.isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        cart.getItems().removeAll(itemsToRemove);
        cartItemRepository.deleteAll(itemsToRemove);

        cart.updateTotalPrice();
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public CartResponse getCart() {
        String userId = getUserId();
        Cart cart = getOrCreateCartEntity(userId);
        Double totalPrice = calculateTotalPrice(cart);
        List<CartItemResponse> itemResponses = convertCartItemsToResponses(cart);
        return createCartResponse(cart, itemResponses, totalPrice);
    }

    @Override
    @Transactional
    public void updateProductQuantityInCart(String productId, Integer quantity) {
        String userId = getUserId();
        Cart cart = getOrCreateCartEntity(userId);

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (quantity <= 0) {
            throw new AppException(ErrorCode.INVALID_QUANTITY);
        }

        cartItem.setQuantity(quantity);
        cart.updateTotalPrice();
        cartRepository.save(cart);
    }


    private ProductResponse getProductById(String productId) {
        ApiResponse<ProductResponse> response = productClient.getProductById(productId);
        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return response.getData();
    }

    private Cart getOrCreateCartEntity(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCart(userId));
    }

    private Cart createNewCart(String userId) {
        Cart newCart = Cart.builder()
                .userId(userId)
                .items(new HashSet<>())
                .build();
        return cartRepository.save(newCart);
    }

    private void addNewItemToCart(Cart cart, ProductResponse product, Integer quantity) {

        CartItem newItem = CartItem.builder()
                .productId(product.getId())
                .price(product.getPrice())
                .quantity(quantity)
                .cart(cart)
                .build();
        cart.getItems().add(newItem);
    }

    private Double calculateTotalPrice(Cart cart) {
        return cart.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }

    private List<CartItemResponse> convertCartItemsToResponses(Cart cart) {
        return cart.getItems().stream()
                .map(item -> CartItemResponse.builder()
                        .productId(item.getProductId())
                        .productName(getProductById(item.getProductId()).getName())
                        .thumbnail(getProductById(item.getProductId()).getThumbnailImage())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .totalItemPrice(item.getPrice() * item.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    private CartResponse createCartResponse(Cart cart, List<CartItemResponse> itemResponses, Double totalPrice) {
        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .totalPrice(totalPrice)
                .build();
    }

    private String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String username = authentication.getName();
        ApiResponse<AuthResponse> response = authClient.getUserByUsername(username);

        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.NOT_FOUND_USER);
        }
        ApiResponse<UserResponse> userId = userClient.getUserByAccountId(response.getData().getId());
        return userId.getData().getId() ;
    }


}
