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
    private final PDFService pdfService;

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

        // Generate PDF for the order
        try {
            UserResponse user = safeGetUser(order.getUserId());
            AddressResponse address = safeGetAddress(order.getAddressId());
            String pdfPath = pdfService.generateOrderPDF(order, user, address);
            order.setPdfFilePath(pdfPath);
            orderRepository.save(order);
            log.info("PDF generated for order {}: {}", order.getId(), pdfPath);
        } catch (Exception e) {
            log.error("Failed to generate PDF for order {}: {}", order.getId(), e.getMessage());
            // Continue even if PDF generation fails
        }

        // Clear cart after creating pre-order
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
        assignOrderService.assignOrderToStore(orderId);
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
        Pageable pageable = PageRequest.of(page, size);

        Page<Order> orders = orderRepository.searchByKeywordNative(request, pageable);

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
    public PageResponse<OrderResponse> searchOrderByStoreId(String request, int page, int size, String storeId) {
        log.info("searchOrderByStoreId called with storeId: {}, keyword: {}, page: {}, size: {}", storeId, request, page, size);
        
        // Validate storeId
        if (storeId == null || storeId.trim().isEmpty()) {
            log.error("storeId is null or empty");
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }
        
        // Check if storeId contains query parameters (indicates parsing issue)
        if (storeId.contains("=") || storeId.contains("&")) {
            log.error("storeId appears to contain query parameters: {}", storeId);
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }
        
        Pageable pageable = PageRequest.of(page, size);
        log.debug("Calling getStoreById with storeId: {}", storeId);
        String id = getStoreById(storeId);
        log.debug("Validated storeId: {}", id);

        Page<Order> orders = orderRepository.searchByStoreIdAndKeyword(id, request, pageable);

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

    @Override
    public PageResponse<OrderResponse> getOrdersByStoreId(String storeId, EnumProcessOrder status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Validate store exists
        String validatedStoreId = getStoreById(storeId);

        Page<Order> orders;
        if (status != null) {
            // Lọc theo storeId và status
            orders = orderRepository.findByStoreIdAndStatusAndIsDeletedFalse(validatedStoreId, status, pageable);
        } else {
            // Lấy tất cả orders của store (không filter status)
            orders = orderRepository.findByStoreIdAndIsDeletedFalse(validatedStoreId, pageable);
        }

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
    public List<ProcessOrderResponse> getOrderStatusHistory(Long orderId) {
        // Kiểm tra đơn hàng có tồn tại không
        orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Lấy lịch sử status, sắp xếp theo thời gian (cũ nhất trước)
        List<ProcessOrder> processOrders = processOrderRepository.findByOrderIdOrderByCreatedAtAsc(orderId);

        return processOrders.stream()
                .map(process -> ProcessOrderResponse.builder()
                        .id(process.getId())
                        .status(process.getStatus())
                        .createdAt(process.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<OrderResponse> getStoreOrdersWithInvoice(String storeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Validate store exists - catch exception to return proper error
        String validatedStoreId;
        try {
            validatedStoreId = getStoreById(storeId);
        } catch (AppException e) {
            // Re-throw AppException to let GlobalExceptionHandler handle it
            // This ensures proper HTTP status code (404 for STORE_NOT_FOUND)
            throw e;
        } catch (Exception e) {
            // Handle any other exceptions (e.g., Feign client errors)
            log.error("Error validating store {}: {}", storeId, e.getMessage());
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        // Lấy orders của store đã có hóa đơn (có pdfFilePath)
        Page<Order> orders = orderRepository.findByStoreIdWithInvoice(validatedStoreId, pageable);

        List<OrderResponse> responses = orders.getContent()
                .stream()
                .map(order -> {
                    OrderResponse response = mapToResponse(order);
                    // Kiểm tra file PDF có tồn tại không
                    boolean hasPdfFile = false;
                    if (order.getPdfFilePath() != null && !order.getPdfFilePath().isEmpty()) {
                        try {
                            java.io.File pdfFile = new java.io.File(order.getPdfFilePath());
                            hasPdfFile = pdfFile.exists();
                        } catch (Exception e) {
                            log.warn("Error checking PDF file existence for order {}: {}", order.getId(), e.getMessage());
                        }
                    }
                    // Set hasPdfFile vào response
                    response.setHasPdfFile(hasPdfFile);
                    return response;
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

        // Kiểm tra file PDF có tồn tại không
        boolean hasPdfFile = false;
        if (order.getPdfFilePath() != null && !order.getPdfFilePath().isEmpty()) {
            try {
                java.io.File pdfFile = new java.io.File(order.getPdfFilePath());
                hasPdfFile = pdfFile.exists();
            } catch (Exception e) {
                log.warn("Error checking PDF file existence for order {}: {}", order.getId(), e.getMessage());
            }
        }

        return OrderResponse.builder()
                .id(order.getId())
                .user(safeGetUser(order.getUserId()))
                .address(safeGetAddress(order.getAddressId()))
                .total(order.getTotal())
                .note(order.getNote())
                .status(order.getStatus())
                .reason(order.getReason())
                .orderDate(order.getOrderDate())
                .orderDetails(
                        order.getOrderDetails() != null
                                ? order.getOrderDetails().stream()
                                .map(detail -> {
                                    ProductColorResponse productColor = null;
                                    try {
                                        productColor = getProductColorResponse(detail.getProductColorId());
                                    } catch (Exception e) {
                                        log.warn("Failed to get product color for {}: {}", detail.getProductColorId(), e.getMessage());
                                    }
                                    return OrderDetailResponse.builder()
                                            .id(detail.getId())
                                            .productColorId(detail.getProductColorId())
                                            .quantity(detail.getQuantity())
                                            .price(detail.getPrice())
                                            .productColor(productColor)
                                            .build();
                                })
                                .collect(Collectors.toList())
                                : Collections.emptyList()
                )
                .processOrders(
                        order.getProcessOrders() != null
                                ? order.getProcessOrders().stream()
                                .sorted((p1, p2) -> {
                                    if (p1.getCreatedAt() == null && p2.getCreatedAt() == null) return 0;
                                    if (p1.getCreatedAt() == null) return 1;
                                    if (p2.getCreatedAt() == null) return -1;
                                    return p1.getCreatedAt().compareTo(p2.getCreatedAt()); // Sắp xếp cũ nhất trước
                                })
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
                .pdfFilePath(order.getPdfFilePath())
                .hasPdfFile(hasPdfFile)
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
        
        try {
            ApiResponse<UserResponse> userId = userClient.getUserByAccountId(response.getData().getId());
            if (userId == null || userId.getData() == null) {
                throw new AppException(ErrorCode.NOT_FOUND_USER);
            }
            return userId.getData().getId();
        } catch (feign.FeignException.NotFound e) {
            log.warn("User not found via Feign client for accountId: {}", response.getData().getId());
            throw new AppException(ErrorCode.NOT_FOUND_USER);
        } catch (feign.FeignException e) {
            log.error("Feign error when getting user by accountId {}: {}", response.getData().getId(), e.getMessage());
            if (e.status() == 404) {
                throw new AppException(ErrorCode.NOT_FOUND_USER);
            }
            throw new AppException(ErrorCode.NOT_FOUND_USER);
        } catch (Exception e) {
            log.error("Unexpected error when getting user by accountId {}: {}", response.getData().getId(), e.getMessage());
            throw new AppException(ErrorCode.NOT_FOUND_USER);
        }
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
        try {
            ApiResponse<StoreResponse> response = storeClient.getStoreById(storeId);
            if (response == null || response.getData() == null) {
                throw new AppException(ErrorCode.STORE_NOT_FOUND);
            }
            return response.getData().getId();
        } catch (feign.FeignException.NotFound e) {
            // Feign returns 404 when store not found
            log.warn("Store not found via Feign client: {}", storeId);
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        } catch (feign.FeignException e) {
            // Other Feign exceptions (4xx, 5xx)
            log.error("Feign error when getting store {}: {}", storeId, e.getMessage());
            if (e.status() == 404) {
                throw new AppException(ErrorCode.STORE_NOT_FOUND);
            }
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        } catch (Exception e) {
            // Any other exception
            log.error("Unexpected error when getting store {}: {}", storeId, e.getMessage());
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }
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