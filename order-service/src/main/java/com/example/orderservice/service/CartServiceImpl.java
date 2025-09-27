package com.example.orderservice.service;

import com.example.orderservice.entity.Cart;
import com.example.orderservice.entity.CartItem;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.AuthClient;
import com.example.orderservice.feign.ColorClient;
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
import org.springframework.web.bind.annotation.PathVariable;

import java.util.*;
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
    private final ColorClient colorClient;

    @Override
    @Transactional
    public void addProductToCart(String productId, Integer quantity, String colorId) {
        if (quantity <= 0) {
            throw new AppException(ErrorCode.INVALID_QUANTITY);
        }

        String userId = getUserId();
        Cart cart = getOrCreateCartEntity(userId);

        ProductResponse product = getProductById(productId);

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId)
                        && Objects.equals(item.getColorId(), colorId))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + quantity);
        } else {
            addNewItemToCart(cart, product, quantity, colorId);
        }

        cart.updateTotalPrice();
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void deleteProductFromCart(String productId, String colorId) {
        String userId = getUserId();
        Cart cart = getOrCreateCartEntity(userId);

        CartItem itemToRemove = cart.getItems().stream()
                .filter(cartItem -> cartItem.getProductId().equals(productId)
                        && Objects.equals(cartItem.getColorId(), colorId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        cart.getItems().remove(itemToRemove);
        cartItemRepository.delete(itemToRemove);

        cart.updateTotalPrice();
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void removeProductFromCart(List<String> cartItemIds) {
        String userId = getUserId();
        Cart cart = getOrCreateCartEntity(userId);

        List<CartItem> itemsToRemove = cart.getItems().stream()
                .filter(cartItem -> cartItemIds.contains(cartItem.getId()))
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
    public void updateProductQuantityInCart(String productId, String colorId, Integer quantity) {
        if (quantity <= 0) {
            throw new AppException(ErrorCode.INVALID_QUANTITY);
        }

        String userId = getUserId();
        Cart cart = getOrCreateCartEntity(userId);

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId)
                        && Objects.equals(item.getColorId(), colorId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

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

    private void addNewItemToCart(Cart cart, ProductResponse product, Integer quantity, String colorId) {
        CartItem newItem = CartItem.builder()
                .productId(product.getId())
                .price(product.getPrice())
                .quantity(quantity)
                .colorId(colorId)
                .cart(cart)
                .build();
        cart.getItems().add(newItem);
    }

    private Double calculateTotalPrice(Cart cart) {
        return cart.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())                .sum();
    }

    private List<CartItemResponse> convertCartItemsToResponses(Cart cart) {
        Map<String, ProductResponse> productCache = new HashMap<>();

        return cart.getItems().stream()
                .map(item -> {
                    ProductResponse product = productCache.computeIfAbsent(
                            item.getProductId(),
                            this::getProductById
                    );

                    return CartItemResponse.builder()
                            .cartItemId(item.getId())
                            .productId(item.getProductId())
                            .productName(product.getName())
                            .image(getImage(item.getProductId(),item.getColorId()))
                            .price(item.getPrice())
                            .quantity(item.getQuantity())
                            .colorName(getColorName(item.getColorId()))
                            .colorId(item.getColorId())
                            .totalItemPrice(item.getPrice() * item.getQuantity())
                            .build();
                })
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
        return userId.getData().getId();
    }


    private String getImage(String productId, String colorId) {
        ApiResponse<ProductResponse> response = colorClient.getProductByColorId(productId, colorId);
        ProductResponse product = response.getData();

        if (product == null || product.getColor() == null) {
            throw new AppException(ErrorCode.COLOR_NOT_FOUND);
        }

        return product.getColor().stream()
                .filter(c -> c.getId().equals(colorId))
                .findFirst()
                .flatMap(c -> c.getImages().stream().findFirst())
                .map(ImageResponse::getImage)
                .orElseThrow(() -> new AppException(ErrorCode.COLOR_NOT_FOUND));
    }

    private String getColorName(String colorId) {
        ApiResponse<ColorResponse> response = colorClient.getColorById(colorId);

        if (response == null || response.getData() == null ) {
            throw new AppException(ErrorCode.COLOR_NOT_FOUND);
        }
        return response.getData().getColorName();
    }
}
