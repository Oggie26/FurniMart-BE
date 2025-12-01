package com.example.orderservice.service;

import com.example.orderservice.entity.Cart;
import com.example.orderservice.entity.CartItem;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.*;
import com.example.orderservice.repository.CartItemRepository;
import com.example.orderservice.repository.CartRepository;
import com.example.orderservice.response.*;
import com.example.orderservice.service.inteface.CartService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
    private final InventoryClient inventoryClient;

    @Override
    @Transactional
    public void addProductToCart(String productColorId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new AppException(ErrorCode.INVALID_QUANTITY);
        }

        String userId = getUserId();
        Cart cart = getOrCreateCartEntity(userId);

        ProductColorResponse productColor = getProductColor(productColorId);
        int availableStock = getAvailableProduct(productColorId);

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductColorId().equals(productColorId))
                .findFirst();

        if (existingItem.isPresent()) {
            int totalQuantity = existingItem.get().getQuantity() + quantity;

            if (totalQuantity > availableStock) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            existingItem.get().setQuantity(totalQuantity);
        } else {
            if (quantity > availableStock) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }
            addNewItemToCart(cart, productColor, quantity);
        }

        cart.updateTotalPrice();
        cartRepository.save(cart);
    }


    @Override
    @Transactional
    public void deleteProductFromCart(String productColorId) {
        String userId = getUserId();
        Cart cart = getOrCreateCartEntity(userId);

        CartItem itemToRemove = cart.getItems().stream()
                .filter(cartItem -> cartItem.getProductColorId().equals(productColorId))
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
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String userId = getUserId();
        Cart cart = getOrCreateCartEntity(userId);

        List<CartItem> itemsToRemove = cart.getItems().stream()
                .filter(item -> cartItemIds.contains(item.getId()))
                .toList();

        if (itemsToRemove.isEmpty()) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        cartItemRepository.deleteAll(itemsToRemove);
        itemsToRemove.forEach(cart.getItems()::remove);
        cart.updateTotalPrice();
        cartRepository.save(cart);
    }


    @Override
    @Transactional
    public void clearCart() {
        Cart cart = cartRepository.findByUserId(getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        Set<CartItem> items = cart.getItems();

        if (items != null && !items.isEmpty()) {
            cartItemRepository.deleteAllInBatch(items);
            items.clear();
        }
    }


    @Override
    public CartResponse getCartById(Long id) {
        Cart cart = cartRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        String userId = getUserId();
        if (!cart.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        List<CartItemResponse> itemResponses = convertCartItemsToResponses(cart);
        Double totalPrice = calculateTotalPrice(cart);

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .totalPrice(totalPrice)
                .build();
    }

    @Override
    @Transactional
    public CartResponse getCart() {
        String userId = getUserId();
        Cart cart = getOrCreateCartEntity(userId);

        List<CartItemResponse> itemResponses = convertCartItemsToResponses(cart);
        Double totalPrice = calculateTotalPrice(cart);

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .totalPrice(totalPrice)
                .build();
    }

    @Override
    @Transactional
    public void updateProductQuantityInCart(String productColorId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new AppException(ErrorCode.INVALID_QUANTITY);
        }

        String userId = getUserId();
        Cart cart = getOrCreateCartEntity(userId);

        ProductColorResponse productColor = getProductColor(productColorId);
        int availableQuantity = getAvailableProduct(productColorId);

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductColorId().equals(productColorId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;

            if (newQuantity > availableQuantity) {
                throw new AppException(ErrorCode.INVALID_QUANTITY);
            }

            item.setQuantity(newQuantity);
        } else {
            if (quantity > availableQuantity) {
                throw new AppException(ErrorCode.INVALID_QUANTITY);
            }

            addNewItemToCart(cart, productColor, quantity);
        }

        cart.updateTotalPrice();
        cartRepository.save(cart);
    }


    private ProductColorResponse getProductColor(String productColorId) {
        ApiResponse<ProductColorResponse> response = productClient.getProductColor(productColorId);
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

    private void addNewItemToCart(Cart cart, ProductColorResponse productColor, Integer quantity) {
        CartItem newItem = CartItem.builder()
                    .productColorId(productColor.getId())
                .price(productColor.getProduct().getPrice())
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
        Map<String, ProductColorResponse> productColorCache = new HashMap<>();

        return cart.getItems().stream()
                .map(item -> {
                    ProductColorResponse productColor = productColorCache.computeIfAbsent(
                            item.getProductColorId(),
                            this::getProductColor
                    );

                    return CartItemResponse.builder()
                            .cartItemId(item.getId())
                            .productColorId(item.getProductColorId())
                            .productName(productColor.getProduct().getName())
                            .image(productColor.getImages().stream()
                                    .findFirst()
                                    .map(ImageResponse::getImage)
                                    .orElse(null))
                            .price(item.getPrice())
                            .quantity(item.getQuantity())
                            .colorName(productColor.getColor() != null ? productColor.getColor().getColorName() : "")
                            .totalItemPrice(item.getPrice() * item.getQuantity())
                            .build();
                })
                .collect(Collectors.toList());
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

        ApiResponse<UserResponse> userIdResponse = userClient.getUserByAccountId(response.getData().getId());
        if (userIdResponse == null || userIdResponse.getData() == null) {
            throw new AppException(ErrorCode.NOT_FOUND_USER);
        }

        return userIdResponse.getData().getId();
    }

    private Integer getAvailableProduct(String productColorId){
        ApiResponse<Integer> response = inventoryClient.getAvailableStockByProductColorId(productColorId);
        return Objects.requireNonNull(response.getData());
    }
}
