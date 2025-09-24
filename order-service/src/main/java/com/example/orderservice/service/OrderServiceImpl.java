package com.example.orderservice.service;

import com.example.orderservice.entity.*;
import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.enums.PaymentStatus;
import com.example.orderservice.event.OrderPlacedEvent;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.AuthClient;
import com.example.orderservice.feign.StoreClient;
import com.example.orderservice.feign.UserClient;
import com.example.orderservice.repository.*;
import com.example.orderservice.response.*;
import com.example.orderservice.service.inteface.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProcessOrderRepository processOrderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final PaymentRepository paymentRepository;
    private final UserClient userClient;
    private final AuthClient authClient;
    private final StoreClient storeClient;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Override
    @Transactional
    public OrderResponse createOrder(Long cartId, Long addressId, PaymentMethod paymentMethod, String voucherCode) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        if (paymentMethod == null) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }

        Order order = buildOrder(cart, addressId);
        order = orderRepository.save(order);

        List<OrderDetail> details = createOrderItemsFromCart(cart, order);
        orderDetailRepository.saveAll(details);

        order.setOrderDetails(details);
        orderRepository.save(order);

        ProcessOrder process = new ProcessOrder();
        process.setOrder(order);
        process.setStatus(EnumProcessOrder.PENDING);
        process.setCreatedAt(new Date());
        processOrderRepository.save(process);

        order.setProcessOrders(List.of(process));
        orderRepository.save(order);

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PENDING)
                .date(new Date())
                .total(order.getTotal())
                .userId(order.getUserId())
                .transactionCode(generateTransactionCode())
                .build();
        paymentRepository.save(payment);

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (!cartItems.isEmpty()) {
            cartItemRepository.deleteAll(cartItems);
        }
        cart.getItems().clear();
        cart.setTotalPrice(0.0);
        cartRepository.save(cart);
//
//        try {
//            OrderPlacedEvent event = new OrderPlacedEvent(order.getId(), order.getUserId(), order.getTotal());
//            kafkaTemplate.send("order-placed", event);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }

        return mapToResponse(order);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->  new AppException(ErrorCode.ORDER_NOT_FOUND));
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, EnumProcessOrder status) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() ->  new AppException(ErrorCode.ORDER_NOT_FOUND));

        ProcessOrder process = new ProcessOrder();
        process.setOrder(order);
        process.setStatus(status);
        process.setCreatedAt(new Date());

        processOrderRepository.save(process);

         if (order.getProcessOrders() == null) {
            order.setProcessOrders(List.of(process));
        } else {
            order.getProcessOrders().add(process);
        }
        orderRepository.save(order);

        return mapToResponse(order);
    }

    @Override
    public PageResponse<OrderResponse> searchOrderByCustomer(String request, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String userId = getUserId();

        Page<Order> orders = orderRepository.searchByUserIdAndKeyword(userId, request, pageable);

        List<OrderResponse> responses = orders.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                responses,
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages(),
                orders.isFirst(),
                orders.isLast()
        );
    }

    @Override
    public PageResponse<OrderResponse> searchOrder(String request, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Order> orders = orderRepository.searchByKeywordNative(request, pageable);

        List<OrderResponse> responses = orders.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                responses,
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages(),
                orders.isFirst(),
                orders.isLast()
        );
    }

    @Override
    public PageResponse<OrderResponse> searchOrderByStoreId(String request, int page, int size, String storeId) {
        Pageable pageable = PageRequest.of(page, size);
        String id = getStoreById(storeId);

        Page<Order> orders = orderRepository.searchByStoreIdAndKeyword(id, request, pageable);

        List<OrderResponse> responses = orders.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                responses,
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages(),
                orders.isFirst(),
                orders.isLast()
        );
    }

    private OrderResponse mapToResponse(Order order) {
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);

        PaymentResponse paymentResponse = null;
        if (payment != null) {
            paymentResponse = PaymentResponse.builder()
                    .id(payment.getId())
                    .transactionCode(payment.getTransactionCode())
                    .total(payment.getTotal())
                    .paymentMethod(payment.getPaymentMethod())
                    .paymentStatus(payment.getPaymentStatus())
                    .date(payment.getDate())
                    .build();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .user(safeGetUser(order.getUserId()))
                .address(safeGetAddress(order.getAddressId()))
                .total(order.getTotal())
                .note(order.getNote())
                .orderDate(order.getOrderDate())
                .orderDetails(
                        order.getOrderDetails() != null
                                ? order.getOrderDetails().stream()
                                .map(detail -> OrderDetailResponse.builder()
                                        .id(detail.getId())
                                        .productId(detail.getProductId())
                                        .quantity(detail.getQuantity())
                                        .price(detail.getPrice())
                                        .build())
                                .collect(Collectors.toList())
                                : List.of()
                )
                .processOrders(
                        order.getProcessOrders() != null
                                ? order.getProcessOrders().stream()
                                .map(process -> ProcessOrderResponse.builder()
                                        .id(process.getId())
                                        .status(process.getStatus())
                                        .createdAt(process.getCreatedAt())
                                        .build())
                                .collect(Collectors.toList())
                                : List.of()
                )
                .payment(paymentResponse)
                .build();
    }

    private Order buildOrder(Cart cart, Long addressId) {
        Double total = cart.getTotalPrice();
        if (total == null) {
            total = cart.getItems().stream()
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum();
        }

        Order order = Order.builder()
                .total(total)
                .userId(getUserId())
                .addressId(getAddressById(addressId))
                .orderDate(new Date())
                .build();
        return order;
    }

    private List<OrderDetail> createOrderItemsFromCart(Cart cart, Order order) {
        return cart.getItems().stream()
                .map(cartItem -> {
                    OrderDetail detail = OrderDetail.builder()
                            .order(order)
                            .productId(cartItem.getProductId())
                            .quantity(cartItem.getQuantity())
                            .price(cartItem.getPrice())
                            .build();

                    return detail;
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
        ApiResponse<UserResponse> userId = userClient.getUserByAccountId(response.getData().getId());
        if (userId == null || userId.getData() == null) {
            throw new AppException(ErrorCode.NOT_FOUND_USER);
        }
        return userId.getData().getId();
    }

    private Long getAddressById(Long addressId) {
        ApiResponse<AddressResponse> response = userClient.getAddressById(addressId);
        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        return response.getData().getId();
    }

    private String generateTransactionCode() {
        return "TXN-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }

    private UserResponse safeGetUser(String userId) {
        if (userId == null) return null;
        ApiResponse<UserResponse> resp = userClient.getUserById(userId);
        if (resp == null || resp.getData() == null) return null;
        return resp.getData();
    }

    private AddressResponse safeGetAddress(Long addressId) {
        if (addressId == null) return null;
        ApiResponse<AddressResponse> resp = userClient.getAddressById(addressId);
        if (resp == null || resp.getData() == null) return null;
        return resp.getData();
    }

    private String getStoreById(String storeId){
        ApiResponse<StoreResponse> response = storeClient.getStoreById(storeId);
        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }
        return response.getData().getId();
    }

}
