package com.example.orderservice.service;

import com.example.orderservice.entity.*;
import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.enums.PaymentStatus;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.AuthClient;
import com.example.orderservice.feign.ProductClient;
import com.example.orderservice.feign.StoreClient;
import com.example.orderservice.feign.UserClient;
import com.example.orderservice.repository.*;
import com.example.orderservice.request.CancelOrderRequest;
import com.example.orderservice.response.*;
import com.example.orderservice.service.inteface.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final ProcessOrderRepository processOrderRepository;
    private final ProductClient productClient;
    private final CartRepository cartRepository;
    private final PaymentRepository paymentRepository;
    private final UserClient userClient;
    private final AuthClient authClient;
    private final StoreClient storeClient;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final AssignOrderServiceImpl assignOrderService;

    @Override
    @Transactional
    public OrderResponse createOrder(Long cartId, Long addressId, PaymentMethod paymentMethod, String voucherCode) {
        if (cartId == null) {
            throw new AppException(ErrorCode.CART_NOT_FOUND);
        }
        if (addressId == null) {
            throw new AppException(ErrorCode.INVALID_ADDRESS);
        }
        if (paymentMethod == null) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        if (cart.getItems().isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        log.info("Cart items class: {}", cart.getItems().getClass().getName());

        Order order = buildOrder(cart, addressId);
        List<OrderDetail> details = createOrderItemsFromCart(cart, order);
        order.setOrderDetails(details);
        order.setStatus(EnumProcessOrder.PENDING);
        ProcessOrder process = new ProcessOrder();
        process.setOrder(order);
        process.setStatus(EnumProcessOrder.PENDING);
        process.setCreatedAt(new Date());
        order.setProcessOrders(new ArrayList<>(List.of(process)));

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

        cart.getItems().clear();
        cart.setTotalPrice(0.0);
        cartRepository.save(cart);

        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse createPreOrder(Long cartId, Long addressId, String voucherCode) {
        if (cartId == null) {
            throw new AppException(ErrorCode.CART_NOT_FOUND);
        }
        if (addressId == null) {
            throw new AppException(ErrorCode.INVALID_ADDRESS);
        }

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        if (cart.getItems().isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        log.info("Creating pre-order for cart: {}", cartId);

        Order order = buildOrder(cart, addressId);
        List<OrderDetail> details = createOrderItemsFromCart(cart, order);
        order.setOrderDetails(details);
        order.setStatus(EnumProcessOrder.PRE_ORDER);
        
        ProcessOrder process = new ProcessOrder();
        process.setOrder(order);
        process.setStatus(EnumProcessOrder.PRE_ORDER);
        process.setCreatedAt(new Date());
        order.setProcessOrders(new ArrayList<>(List.of(process)));

        orderRepository.save(order);

        cart.getItems().clear();
        cart.setTotalPrice(0.0);
        cartRepository.save(cart);

        log.info("Pre-order created successfully with ID: {}", order.getId());
        return mapToResponse(order);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return mapToResponse(order);
    }

    @Override
    public void cancelOrder(CancelOrderRequest cancelOrderRequest) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(cancelOrderRequest.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        order.setStatus(EnumProcessOrder.CANCELLED);

        ProcessOrder process = new ProcessOrder();
        process.setOrder(order);
        process.setStatus(EnumProcessOrder.CANCELLED);
        process.setCreatedAt(new Date());
        order.setProcessOrders(new ArrayList<>(List.of(process)));

        List<OrderCreatedEvent.OrderItem> orderItems = order.getOrderDetails().stream()
                .map(detail -> OrderCreatedEvent.OrderItem.builder()
                        .productColorId(detail.getProductColorId())
                        .quantity(detail.getQuantity())
                        .productName(getProductColorResponse(detail.getProductColorId()).getProduct().getName())
                        .price(detail.getPrice())
                        .colorName(getProductColorResponse(detail.getProductColorId()).getColor().getColorName())
                        .build())
                .toList();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .email(safeGetUser(order.getUserId()).getEmail())
                .fullName(safeGetUser(order.getUserId()).getFullName())
                .orderDate(order.getOrderDate())
                .totalPrice(order.getTotal())
                .orderId(order.getId())
                .storeId(order.getStoreId())
                .addressLine(getAddress(order.getAddressId()))
                .paymentMethod(order.getPayment().getPaymentMethod())
                .items(orderItems)
                .build();

        try {
            kafkaTemplate.send("order-cancel-topic", event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                        } else {
                            log.info("Successfully sent order cancel event for: {}", event.getOrderId());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to send Kafka event {}, error: {}", event.getFullName(), e.getMessage());
        }

        processOrderRepository.save(process);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, EnumProcessOrder status) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        ProcessOrder process = new ProcessOrder();
        process.setOrder(order);
        process.setStatus(status);
        process.setCreatedAt(new Date());

        processOrderRepository.save(process);

        if (order.getProcessOrders() == null) {
            order.setProcessOrders(new ArrayList<>());
        }
        order.getProcessOrders().add(process);
        orderRepository.save(order);
        if(status.equals(EnumProcessOrder.PAYMENT)){
            List<OrderCreatedEvent.OrderItem> orderItems = order.getOrderDetails().stream()
                    .map(detail -> OrderCreatedEvent.OrderItem.builder()
                            .productColorId(detail.getProductColorId())
                            .quantity(detail.getQuantity())
                            .productName(getProductColorResponse(detail.getProductColorId()).getProduct().getName())
                            .price(detail.getPrice())
                            .colorName(getProductColorResponse(detail.getProductColorId()).getColor().getColorName())
                            .build())
                    .toList();
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
            if (payment.getPaymentMethod().equals(PaymentMethod.COD)){
                payment.setPaymentStatus(PaymentStatus.DEPOSITED);
            }else{
                payment.setPaymentStatus(PaymentStatus.PAID);
            }
            paymentRepository.save(payment);

            assignOrderService.assignOrderToStore(orderId);

            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .email(safeGetUser(order.getUserId()).getEmail())
                    .fullName(safeGetUser(order.getUserId()).getFullName())
                    .orderDate(order.getOrderDate())
                    .totalPrice(order.getTotal())
                    .orderId(order.getId())
                    .storeId(order.getStoreId())
                    .addressLine(getAddress(order.getAddressId()))
                    .paymentMethod(order.getPayment().getPaymentMethod())
                    .items(orderItems)
                    .build();

            try {
                kafkaTemplate.send("order-created-topic", event)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                            } else {
                                log.info("Successfully sent order creation event for: {}", event.getOrderId());
                            }
                        });
            } catch (Exception e) {
                log.error("Failed to send Kafka event {}, error: {}", event.getFullName(), e.getMessage());
            }
        }
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public void handlePaymentCOD(Long orderId){
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        assignOrderService.assignOrderToStore(orderId);

        List<OrderCreatedEvent.OrderItem> orderItems = order.getOrderDetails().stream()
                .map(detail -> OrderCreatedEvent.OrderItem.builder()
                        .productColorId(detail.getProductColorId())
                        .quantity(detail.getQuantity())
                        .productName(getProductColorResponse(detail.getProductColorId()).getProduct().getName())
                        .price(detail.getPrice())
                        .colorName(getProductColorResponse(detail.getProductColorId()).getColor().getColorName())
                        .build())
                .toList();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .email(safeGetUser(order.getUserId()).getEmail())
                .fullName(safeGetUser(order.getUserId()).getFullName())
                .orderDate(order.getOrderDate())
                .totalPrice(order.getTotal())
                .orderId(order.getId())
                .storeId(order.getStoreId())
                .addressLine(getAddress(order.getAddressId()))
                .paymentMethod(PaymentMethod.COD)
                .items(orderItems)
                .build();

        try {
            kafkaTemplate.send("order-created-topic", event)
                    .whenComplete((
                            result, ex) -> {
                        if (ex != null) {
                        } else {
                            log.info("Successfully sent order creation event for: {}", event.getOrderId());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to send Kafka event {}, error: {}", event.getFullName(), e.getMessage());
        }
    }

    @Override
    public PageResponse<OrderResponse> searchOrderByCustomer(String request, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String userId = getUserId();

        Page<Order> orders = orderRepository.searchByUserIdAndKeyword(userId, request, pageable);

        List<OrderResponse> responses = orders.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

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
        try {
            Pageable pageable = PageRequest.of(page, size);

            Page<Order> orders = orderRepository.searchByKeywordNative(request, pageable);

            List<OrderResponse> responses = orders.getContent()
                    .stream()
                    .map(order -> {
                        try {
                            return mapToResponse(order);
                        } catch (Exception e) {
                            log.error("Error mapping order {} to response: {}", order.getId(), e.getMessage(), e);
                            return OrderResponse.builder()
                                    .id(order.getId())
                                    .total(order.getTotal())
                                    .status(order.getStatus())
                                    .orderDate(order.getOrderDate())
                                    .note(order.getNote())
                                    .storeId(order.getStoreId())
                                    .orderDetails(Collections.emptyList())
                                    .processOrders(Collections.emptyList())
                                    .build();
                        }
                    })
                    .collect(Collectors.toList());

            return new PageResponse<>(
                    responses,
                    orders.getNumber(),
                    orders.getSize(),
                    orders.getTotalElements(),
                    orders.getTotalPages(),
                    orders.isFirst(),
                    orders.isLast()
            );
        } catch (AppException e) {
            log.error("Application error in searchOrder: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in searchOrder: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    public PageResponse<OrderResponse> searchOrderByStoreId(String request, int page, int size, String storeId) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            String id = getStoreById(storeId);

            Page<Order> orders = orderRepository.searchByStoreIdAndKeyword(id, request, pageable);

            List<OrderResponse> responses = orders.getContent()
                    .stream()
                    .map(order -> {
                        try {
                            return mapToResponse(order);
                        } catch (Exception e) {
                            log.error("Error mapping order {} to response: {}", order.getId(), e.getMessage(), e);
                            return OrderResponse.builder()
                                    .id(order.getId())
                                    .total(order.getTotal())
                                    .status(order.getStatus())
                                    .orderDate(order.getOrderDate())
                                    .note(order.getNote())
                                    .storeId(order.getStoreId())
                                    .orderDetails(Collections.emptyList())
                                    .processOrders(Collections.emptyList())
                                    .build();
                        }
                    })
                    .collect(Collectors.toList());

            return new PageResponse<>(
                    responses,
                    orders.getNumber(),
                    orders.getSize(),
                    orders.getTotalElements(),
                    orders.getTotalPages(),
                    orders.isFirst(),
                    orders.isLast()
            );
        } catch (AppException e) {
            log.error("Application error in searchOrderByStoreId: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in searchOrderByStoreId: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    public PageResponse<OrderResponse> getOrdersByStatus(EnumProcessOrder status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderRepository.findByStatusAndIsDeletedFalse(status, pageable);

        List<OrderResponse> responses = orders.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

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
                .status(order.getStatus())
                .depositPrice(order.getDepositPrice())
                .reason(order.getReason())
                .orderDate(order.getOrderDate())
                .orderDetails(
                        order.getOrderDetails() != null
                                ? order.getOrderDetails().stream()
                                .map(detail -> OrderDetailResponse.builder()
                                        .id(detail.getId())
                                        .productColorId(detail.getProductColorId())
                                        .quantity(detail.getQuantity())
                                        .price(detail.getPrice())
                                        .build())
                                .collect(Collectors.toList())
                                : Collections.emptyList()
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
                                : Collections.emptyList()
                )
                .payment(paymentResponse)
                .storeId(order.getStoreId())
                .qrCode(order.getQrCode())
                .qrCodeGeneratedAt(order.getQrCodeGeneratedAt())
                .build();
    }

    private Order buildOrder(Cart cart, Long addressId) {
        Double total = cart.getTotalPrice();
        if (total == null) {
            total = cart.getItems().stream()
                    .filter(item -> item.getPrice() != null && item.getQuantity() != null)
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum();
        }

        Order order = Order.builder()
                .total(total)
                .userId(getUserId())
                .addressId(getAddressById(addressId))
                .depositPrice(0.0)
                .orderDate(new Date())
                .build();
        return order;
    }

    private List<OrderDetail> createOrderItemsFromCart(Cart cart, Order order) {
        return cart.getItems().stream()
                .filter(item -> item.getPrice() != null && item.getQuantity() != null)
                .map(cartItem -> OrderDetail.builder()
                        .order(order)
                        .productColorId(cartItem.getProductColorId())
                        .quantity(cartItem.getQuantity())
                        .price(cartItem.getPrice())
                        .build())
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
        return "TXN-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 10000);
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

    private String getStoreById(String storeId) {
        ApiResponse<StoreResponse> response = storeClient.getStoreById(storeId);
        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }
        return response.getData().getId();
    }

    private String getAddress(Long addressId) {
        if (addressId == null) return null;
        ApiResponse<AddressResponse> resp = userClient.getAddressById(addressId);
        if (resp == null || resp.getData() == null) return null;
        return resp.getData().getAddressLine();
    }

    private ProductColorResponse getProductColorResponse(String id){
        ApiResponse<ProductColorResponse> response = productClient.getProductColor(id);
        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return response.getData();
    }

}