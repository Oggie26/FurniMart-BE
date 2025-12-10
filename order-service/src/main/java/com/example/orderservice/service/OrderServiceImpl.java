package com.example.orderservice.service;

import com.example.orderservice.entity.*;
import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.enums.PaymentStatus;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.*;
import com.example.orderservice.repository.*;
import com.example.orderservice.request.StaffCreateOrderRequest;
import com.example.orderservice.request.CancelOrderRequest;
import com.example.orderservice.response.*;
import com.example.orderservice.service.inteface.CartService;
import com.example.orderservice.service.inteface.OrderService;
import com.example.orderservice.service.inteface.WarrantyService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final QRCodeService qrCodeService;
    private final CartService cartService;
    private final DeliveryClient deliveryClient;
    @Lazy
    private final WarrantyService warrantyService;

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

        try {
            UserResponse user = safeGetUser(order.getUserId());
            AddressResponse address = safeGetAddress(order.getAddressId());
            String pdfPath = pdfService.generateOrderPDF(order, user, address);
            order.setPdfFilePath(pdfPath);
            orderRepository.save(order);
        } catch (Exception e) {
            log.error("Failed to generate PDF for order {}: {}", order.getId(), e.getMessage());
        }

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

        try {
            UserResponse user = safeGetUser(order.getUserId());
            AddressResponse address = safeGetAddress(order.getAddressId());
            String pdfPath = pdfService.generateOrderPDF(order, user, address);
            order.setPdfFilePath(pdfPath);
            orderRepository.save(order);
            log.info("PDF generated for order {}: {}", order.getId(), pdfPath);
        } catch (Exception e) {
            log.error("Failed to generate PDF for order {}: {}", order.getId(), e.getMessage());
        }

        // Clear cart after creating pre-order
        cart.getItems().clear();
        cart.setTotalPrice(0.0);
        cartRepository.save(cart);

        log.info("Pre-order created successfully with ID: {}", order.getId());
        return mapToResponse(order);
    }

    // @Override
    // @Transactional
    // public OrderResponse createOrderForStaff(StaffCreateOrderRequest request) {
    //
    // ApiResponse<StoreResponse> storeResponse =
    // storeClient.getStoreById(request.getStoreId());
    // if (storeResponse == null || storeResponse.getData() == null) {
    // throw new AppException(ErrorCode.STORE_NOT_FOUND);
    // }
    //
    // ApiResponse<UserResponse> userResponse =
    // userClient.getUserById(request.getUserId());
    // if (userResponse == null || userResponse.getData() == null) {
    // throw new AppException(ErrorCode.NOT_FOUND_USER);
    // }
    //
    // ApiResponse<AddressResponse> addressResponse =
    // userClient.getAddressById(request.getAddressId());
    // if (addressResponse == null || addressResponse.getData() == null) {
    // throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
    // }
    //
    // Double total = request.getOrderDetails().stream()
    // .filter(detail -> detail.getPrice() != null && detail.getQuantity() != null)
    // .mapToDouble(detail -> detail.getPrice() * detail.getQuantity())
    // .sum();
    //
    //
    // if (total <= 0) {
    // throw new AppException(ErrorCode.INVALID_ORDER_TOTAL);
    // }
    //
    // Order order = Order.builder()
    // .storeId(request.getStoreId())
    // .total(total)
    // .status(EnumProcessOrder.MANAGER_ACCEPT)
    // .note(request.getNote())
    // .userId(request.getUserId())
    // .addressId(request.getAddressId())
    // .reason(request.getReason())
    // .orderDate(new Date())
    // .build();
    //
    // List<OrderDetail> orderDetails = request.getOrderDetails().stream()
    // .filter(detail -> detail.getPrice() != null && detail.getQuantity() != null)
    // .map(detail -> OrderDetail.builder()
    // .order(order)
    // .productColorId(detail.getProductColorId())
    // .quantity(detail.getQuantity())
    // .price(detail.getPrice())
    // .build())
    // .collect(Collectors.toList());
    //
    // if (orderDetails.isEmpty()) {
    // throw new AppException(ErrorCode.CART_EMPTY);
    // }
    //
    // order.setOrderDetails(orderDetails);
    //
    // Order savedOrder = orderRepository.save(order);
    //
    // QRCodeService.QRCodeResult qrCodeResult =
    // qrCodeService.generateQRCode(savedOrder.getId());
    // savedOrder.setQrCode(qrCodeResult.getQrCodeString());
    // savedOrder.setQrCodeGeneratedAt(new Date());
    //
    // ProcessOrder process = new ProcessOrder();
    // process.setOrder(savedOrder);
    // process.setStatus(EnumProcessOrder.MANAGER_ACCEPT);
    // process.setCreatedAt(new Date());
    // processOrderRepository.save(process);
    //
    // savedOrder.setProcessOrders(new ArrayList<>(List.of(process)));
    //
    // savedOrder = orderRepository.save(savedOrder);
    // if (request.getPaymentMethod().equals(PaymentMethod.COD)){
    // Payment payment = Payment.builder()
    // .order(savedOrder)
    // .paymentMethod(request.getPaymentMethod())
    // .paymentStatus(PaymentStatus.PENDING)
    // .date(new Date())
    // .total(savedOrder.getTotal())
    // .userId(savedOrder.getUserId())
    // .transactionCode(generateTransactionCode())
    // .build();
    // paymentRepository.save(payment);
    // }else{
    // Payment payment = Payment.builder()
    // .order(savedOrder)
    // .paymentMethod(request.getPaymentMethod())
    // .paymentStatus(PaymentStatus.PAID)
    // .date(new Date())
    // .total(savedOrder.getTotal())
    // .userId(savedOrder.getUserId())
    // .transactionCode(generateTransactionCode())
    // .build();
    // paymentRepository.save(payment);
    // }
    //
    // OrderCreatedEvent event = OrderCreatedEvent.builder()
    // .email(safeGetUser(order.getUserId()).getEmail())
    // .fullName(safeGetUser(order.getUserId()).getFullName())
    // .orderDate(order.getOrderDate())
    // .totalPrice(order.getTotal())
    // .orderId(order.getId())
    // .storeId(order.getStoreId())
    // .addressLine(getAddress(order.getAddressId()))
    // .paymentMethod(order.getPayment().getPaymentMethod())
    // .items(order.getOrderDetails())
    // .build();
    //
    // try {
    // kafkaTemplate.send("order-created-topic", event)
    // .whenComplete((result, ex) -> {
    // if (ex != null) {
    // log.error("Kafka send failed: {}", ex.getMessage());
    // } else {
    // log.info("Successfully sent order creation event for: {}",
    // event.getOrderId());
    // }
    // });
    // } catch (Exception e) {
    // log.error("Failed to send Kafka event {}, error: {}", event.getFullName(),
    // e.getMessage());
    // }
    // return mapToResponse(savedOrder);
    // }

    @Override
    @Transactional
    public OrderResponse createOrderForStaff(StaffCreateOrderRequest request) {

        ApiResponse<StoreResponse> storeResponse = storeClient.getStoreById(request.getStoreId());
        if (storeResponse == null || storeResponse.getData() == null) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        ApiResponse<UserResponse> userResponse = userClient.getUserById(request.getUserId());
        if (userResponse == null || userResponse.getData() == null) {
            throw new AppException(ErrorCode.NOT_FOUND_USER);
        }
        UserResponse userData = userResponse.getData();

        ApiResponse<AddressResponse> addressResponse = userClient.getAddressById(request.getAddressId());
        if (addressResponse == null || addressResponse.getData() == null) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        Double total = request.getOrderDetails().stream()
                .filter(detail -> detail.getPrice() != null && detail.getQuantity() != null)
                .mapToDouble(detail -> detail.getPrice() * detail.getQuantity())
                .sum();

        if (total <= 0) {
            throw new AppException(ErrorCode.INVALID_ORDER_TOTAL);
        }

        Order order = Order.builder()
                .storeId(request.getStoreId())
                .total(total)
                .status(EnumProcessOrder.MANAGER_ACCEPT)
                .note(request.getNote())
                .userId(request.getUserId())
                .addressId(request.getAddressId())
                .reason(request.getReason())
                .orderDate(new Date())
                .build();

        List<OrderDetail> orderDetails = request.getOrderDetails().stream()
                .filter(detail -> detail.getPrice() != null && detail.getQuantity() != null)
                .map(detail -> OrderDetail.builder()
                        .order(order)
                        .productColorId(detail.getProductColorId())
                        .quantity(detail.getQuantity())
                        .price(detail.getPrice())
                        .build())
                .collect(Collectors.toList());

        if (orderDetails.isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        order.setOrderDetails(orderDetails);

        Order savedOrder = orderRepository.save(order);

        QRCodeService.QRCodeResult qrCodeResult = qrCodeService.generateQRCode(savedOrder.getId());
        savedOrder.setQrCode(qrCodeResult.getQrCodeString());
        savedOrder.setQrCodeGeneratedAt(new Date());

        ProcessOrder process = new ProcessOrder();
        process.setOrder(savedOrder);
        process.setStatus(EnumProcessOrder.MANAGER_ACCEPT);
        process.setCreatedAt(new Date());
        processOrderRepository.save(process);

        savedOrder.setProcessOrders(new ArrayList<>(List.of(process)));

        savedOrder = orderRepository.save(savedOrder);

        PaymentStatus paymentStatus = request.getPaymentMethod().equals(PaymentMethod.COD)
                ? PaymentStatus.PENDING
                : PaymentStatus.PAID;

        Payment payment = Payment.builder()
                .order(savedOrder)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(paymentStatus)
                .date(new Date())
                .total(savedOrder.getTotal())
                .userId(savedOrder.getUserId())
                .transactionCode(generateTransactionCode())
                .build();

        paymentRepository.save(payment);

        savedOrder.setPayment(payment);

        List<OrderCreatedEvent.OrderItem> eventItems = savedOrder.getOrderDetails().stream()
                .map(detail -> OrderCreatedEvent.OrderItem.builder()
                        .productColorId(detail.getProductColorId())
                        .quantity(detail.getQuantity())
                        .price(detail.getPrice())
                        .productName(detail.getProductColorId())
                        .colorName("")
                        .build())
                .collect(Collectors.toList());

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .email(userData.getEmail())
                .fullName(userData.getFullName())
                .orderDate(savedOrder.getOrderDate())
                .totalPrice(savedOrder.getTotal())
                .orderId(savedOrder.getId())
                .storeId(savedOrder.getStoreId())
                .addressLine(safeGetAddress(order.getAddressId()).getAddressLine())
                .paymentMethod(request.getPaymentMethod())
                .items(eventItems)
                .build();

        try {
            kafkaTemplate.send("order-created-topic", event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Kafka send failed: {}", ex.getMessage());
                        } else {
                            log.info("Successfully sent order creation event for orderId: {}", event.getOrderId());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to send Kafka event for user {}, error: {}", userData.getFullName(), e.getMessage());
        }

        return mapToResponse(savedOrder);
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

    // @Override
    // @Transactional
    // public void handlePaymentCOD(Long orderId){
    // Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
    // .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
    //
    // assignOrderService.assignOrderToStore(orderId);
    //
    //
    // List<OrderCreatedEvent.OrderItem> orderItems =
    // order.getOrderDetails().stream()
    // .map(detail -> OrderCreatedEvent.OrderItem.builder()
    // .productColorId(detail.getProductColorId())
    // .quantity(detail.getQuantity())
    // .productName(getProductColorResponse(detail.getProductColorId()).getProduct().getName())
    // .price(detail.getPrice())
    // .colorName(getProductColorResponse(detail.getProductColorId()).getColor().getColorName())
    // .build())
    // .toList();
    //
    //
    // OrderCreatedEvent event = OrderCreatedEvent.builder()
    // .email(safeGetUser(order.getUserId()).getEmail())
    // .fullName(safeGetUser(order.getUserId()).getFullName())
    // .orderDate(order.getOrderDate())
    // .totalPrice(order.getTotal())
    // .orderId(order.getId())
    // .storeId(order.getStoreId())
    // .addressLine(getAddress(order.getAddressId()))
    // .paymentMethod(PaymentMethod.COD)
    // .items(orderItems)
    // .build();
    //
    // for (OrderDetail detail : order.getOrderDetails()) {
    // cartService.removeProductFromCart(Collections.singletonList(detail.getProductColorId()));
    // }
    //
    // try {
    // kafkaTemplate.send("order-created-topic", event)
    // .whenComplete((
    // result, ex) -> {
    // if (ex != null) {
    // log.info("Failed to send Kafka event {}, error: {}", event.getFullName(),
    // ex.getMessage());
    // } else {
    // log.info("Successfully sent order creation event for: {}",
    // event.getOrderId());
    // }
    // });
    // } catch (Exception e) {
    // log.error("Failed to send Kafka event {}, error: {}", event.getFullName(),
    // e.getMessage());
    // }
    // }

    // OrderServiceImpl.java

    @Override
    @Transactional
    public void handlePaymentCOD(Long orderId) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        assignOrderService.assignOrderToStore(orderId);

        List<OrderCreatedEvent.OrderItem> orderItems = new ArrayList<>();
        List<String> productIdsToRemove = new ArrayList<>();

        for (OrderDetail detail : order.getOrderDetails()) {
            ProductColorResponse productInfo = getProductColorResponse(detail.getProductColorId());

            orderItems.add(OrderCreatedEvent.OrderItem.builder()
                    .productColorId(detail.getProductColorId())
                    .quantity(detail.getQuantity())
                    .productName(productInfo.getProduct().getName())
                    .colorName(productInfo.getColor().getColorName())
                    .price(detail.getPrice())
                    .build());

            productIdsToRemove.add(detail.getProductColorId());
        }

        try {
            if (!productIdsToRemove.isEmpty()) {
                cartService.removeProductFromCart(productIdsToRemove);
            }
        } catch (Exception e) {
            log.error("Lỗi xóa giỏ hàng (Ignore): {}", e.getMessage());
        }

        // 3. Gửi Kafka
        // var userInfo = safeGetUser(order.getUserId());
        // OrderCreatedEvent event = OrderCreatedEvent.builder()
        // .email(userInfo.getEmail())
        // .fullName(userInfo.getFullName())
        // .orderDate(order.getOrderDate())
        // .totalPrice(order.getTotal())
        // .orderId(order.getId())
        // .storeId(order.getStoreId())
        // .addressLine(getAddress(order.getAddressId()))
        // .paymentMethod(PaymentMethod.COD)
        // .items(orderItems)
        // .build();
        //
        // try {
        // kafkaTemplate.send("order-created-topic", event)
        // .whenComplete((result, ex) -> {
        // if (ex != null) {
        // log.warn("Kafka send failed: {}", ex.getMessage());
        // } else {
        // log.info("Kafka sent success: {}", event.getOrderId());
        // }
        // });
        // } catch (Exception e) {
        // log.error("Kafka error: {}", e.getMessage());
        // }
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
        order.setStatus(status);
        orderRepository.save(order);

        if (status.equals(EnumProcessOrder.DELIVERED) || status.equals(EnumProcessOrder.FINISHED)) {
            try {
                warrantyService.createWarrantiesForOrder(orderId);
                log.info("Warranties created for order: {}", orderId);
            } catch (Exception e) {
                log.error("Failed to create warranties for order: {}", orderId, e);
            }
        }

        if (status.equals(EnumProcessOrder.PAYMENT)) {
            assignOrderService.assignOrderToStore(orderId);

            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

            if (payment.getPaymentMethod().equals(PaymentMethod.VNPAY)) {
                payment.setPaymentStatus(PaymentStatus.PAID);
                paymentRepository.save(payment);
            } else {
                payment.setPaymentStatus(PaymentStatus.DEPOSITED);
                paymentRepository.save(payment);
            }

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
                                log.error("Kafka send failed: {}", ex.getMessage());
                            } else {
                                log.info("Successfully sent order creation event for: {}", event.getOrderId());
                            }
                        });
            } catch (Exception e) {
                log.error("Failed to send Kafka event {}, error: {}", event.getFullName(), e.getMessage());
            }

        }
        if (status.equals(EnumProcessOrder.FINISHED)) {
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

            if (payment.getPaymentMethod() == PaymentMethod.COD
                    && payment.getPaymentStatus() == PaymentStatus.DEPOSITED) {
                payment.setPaymentStatus(PaymentStatus.PAID);
                paymentRepository.save(payment);
            }

            var user = safeGetUser(order.getUserId());

            List<OrderCreatedEvent.OrderItem> orderItems = order.getOrderDetails().stream()
                    .map(detail -> {
                        var pc = getProductColorResponse(detail.getProductColorId());
                        return OrderCreatedEvent.OrderItem.builder()
                                .productColorId(detail.getProductColorId())
                                .quantity(detail.getQuantity())
                                .productName(pc.getProduct().getName())
                                .price(detail.getPrice())
                                .colorName(pc.getColor().getColorName())
                                .build();
                    })
                    .toList();

            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .orderDate(order.getOrderDate())
                    .totalPrice(order.getTotal())
                    .orderId(order.getId())
                    .storeId(order.getStoreId())
                    .addressLine(getAddress(order.getAddressId()))
                    .paymentMethod(order.getPayment().getPaymentMethod())
                    .items(orderItems)
                    .build();

            kafkaTemplate.send("order-created-topic", event)
                    .whenComplete((result, ex) -> {
                        if (ex != null)
                            log.error("Kafka send failed: {}", ex.getMessage());
                        else
                            log.info("Successfully sent order creation event for: {}", event.getOrderId());
                    });
        }

        return mapToResponse(order);

    }

    @Override
    public PageResponse<OrderResponse> searchOrderByCustomer(String request, int page, int size) {
        // Sanitize search keyword to prevent injection
        if (request != null && !request.trim().isEmpty()) {
            String trimmed = request.trim();
            trimmed = trimmed.replaceAll("[<>\"'%;()&+]", "");
            request = trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
        } else {
            request = "";
        }

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
                orders.isLast());
    }

    @Override
    public PageResponse<OrderResponse> searchOrder(String request, int page, int size) {
        // Sanitize search keyword to prevent injection
        if (request != null && !request.trim().isEmpty()) {
            String trimmed = request.trim();
            trimmed = trimmed.replaceAll("[<>\"'%;()&+]", "");
            request = trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
        } else {
            request = "";
        }

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
                orders.isLast());
    }

    @Override
    public PageResponse<OrderResponse> searchOrderByStoreId(String request, int page, int size, String storeId) {
        Pageable pageable = PageRequest.of(page, size);
        String id = getStoreById(storeId);

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
                orders.isLast());
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
                orders.isLast());
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
                orders.isLast());
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

        String validatedStoreId;
        try {
            validatedStoreId = getStoreById(storeId);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating store {}: {}", storeId, e.getMessage());
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        Page<Order> orders = orderRepository.findByStoreIdWithInvoice(validatedStoreId, pageable);

        List<OrderResponse> responses = orders.getContent()
                .stream()
                .map(order -> {
                    OrderResponse response = mapToResponse(order);
                    boolean hasPdfFile = false;
                    if (order.getPdfFilePath() != null && !order.getPdfFilePath().isEmpty()) {
                        try {
                            java.io.File pdfFile = new java.io.File(order.getPdfFilePath());
                            hasPdfFile = pdfFile.exists();
                        } catch (Exception e) {
                            log.warn("Error checking PDF file existence for order {}: {}", order.getId(),
                                    e.getMessage());
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
                orders.isLast());
    }

    @Override
    @Transactional
    public boolean handleConfirmPayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!PaymentMethod.COD.equals(payment.getPaymentMethod())) {
            return false;
        }
        payment.setPaymentStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);
        return true;
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
                .user(safeGetUser(order.getUserId()) != null ? safeGetUser(order.getUserId()) : null)
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
                                                log.warn("Failed to get product color for {}: {}",
                                                        detail.getProductColorId(), e.getMessage());
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
                                : Collections.emptyList())
                .processOrders(
                        order.getProcessOrders() != null
                                ? order.getProcessOrders().stream()
                                        .sorted((p1, p2) -> {
                                            if (p1.getCreatedAt() == null && p2.getCreatedAt() == null)
                                                return 0;
                                            if (p1.getCreatedAt() == null)
                                                return 1;
                                            if (p2.getCreatedAt() == null)
                                                return -1;
                                            return p1.getCreatedAt().compareTo(p2.getCreatedAt()); // Sắp xếp cũ nhất
                                                                                                   // trước
                                        })
                                        .map(process -> ProcessOrderResponse.builder()
                                                .id(process.getId())
                                                .status(process.getStatus())
                                                .createdAt(process.getCreatedAt())
                                                .build())
                                        .collect(Collectors.toList())
                                : Collections.emptyList())
                .payment(paymentResponse)
                .storeId(order.getStoreId())
                .qrCode(order.getQrCode())
                .qrCodeGeneratedAt(order.getQrCodeGeneratedAt())
                .pdfFilePath(order.getPdfFilePath())
                .deliveryConfirmationResponse(getDeliveryConfirmationResponse(order.getId()))
                .hasPdfFile(hasPdfFile)
                .depositPrice(order.getDepositPrice())
                .orderType(order.getOrderType())
                .warrantyClaimId(order.getWarrantyClaimId())
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

    public UserResponse safeGetUser(String userId) {
        try {
            return userClient.getUserById(userId).getData();
        } catch (FeignException.NotFound e) {
            return null;
        } catch (Exception e) {
            log.error("Error fetching user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private AddressResponse safeGetAddress(Long addressId) {
        if (addressId == null)
            return null;
        ApiResponse<AddressResponse> resp = userClient.getAddressById(addressId);
        if (resp == null || resp.getData() == null)
            return null;
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
            log.warn("Store not found via Feign client: {}", storeId);
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        } catch (feign.FeignException e) {
            log.error("Feign error when getting store {}: {}", storeId, e.getMessage());
            if (e.status() == 404) {
                throw new AppException(ErrorCode.STORE_NOT_FOUND);
            }
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        } catch (Exception e) {
            log.error("Unexpected error when getting store {}: {}", storeId, e.getMessage());
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }
    }

    private String getAddress(Long addressId) {
        if (addressId == null)
            return null;
        ApiResponse<AddressResponse> resp = userClient.getAddressById(addressId);
        if (resp == null || resp.getData() == null)
            return null;
        return resp.getData().getAddressLine();
    }

    private ProductColorResponse getProductColorResponse(String id) {
        ApiResponse<ProductColorResponse> response = productClient.getProductColor(id);
        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return response.getData();
    }

    private DeliveryConfirmationResponse getDeliveryConfirmationResponse(Long orderId) {
        try {
            ApiResponse<DeliveryConfirmationResponse> response = deliveryClient.getDeliveryConfirmation(orderId);

            if (response == null || response.getData() == null) {
                return null; // Không có thông tin thì trả về null nhẹ nhàng
            }
            return response.getData();

        } catch (FeignException.NotFound e) {
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch delivery info for order {}: {}", orderId, e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public OrderResponse requestReturn(Long orderId, String reason, String note) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Validation: Order must be DELIVERED or FINISHED
        if (!EnumProcessOrder.DELIVERED.equals(order.getStatus())
                && !EnumProcessOrder.FINISHED.equals(order.getStatus())) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // Validation: Return window (e.g. 7 days from last status update/created date
        // of status)
        // Assuming implementation simply checks status for now or simple date check if
        // strictly required.
        // For MVP/Demo: Allow request if status is correct.

        ProcessOrder process = new ProcessOrder();
        process.setOrder(order);
        process.setStatus(EnumProcessOrder.RETURN_REQUESTED);
        process.setCreatedAt(new Date());
        processOrderRepository.save(process);

        if (order.getProcessOrders() == null) {
            order.setProcessOrders(new ArrayList<>());
        }
        order.getProcessOrders().add(process);
        order.setStatus(EnumProcessOrder.RETURN_REQUESTED);
        order.setReason(reason);
        order.setNote(note); // Append or set note? user request note.

        return mapToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse acceptReturn(Long orderId) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!EnumProcessOrder.RETURN_REQUESTED.equals(order.getStatus())) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        ProcessOrder process = new ProcessOrder();
        process.setOrder(order);
        process.setStatus(EnumProcessOrder.RETURN_ACCEPTED);
        process.setCreatedAt(new Date());
        processOrderRepository.save(process);

        order.getProcessOrders().add(process);
        order.setStatus(EnumProcessOrder.RETURN_ACCEPTED);

        return mapToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse rejectReturn(Long orderId, String reason) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!EnumProcessOrder.RETURN_REQUESTED.equals(order.getStatus())) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        ProcessOrder process = new ProcessOrder();
        process.setOrder(order);
        process.setStatus(EnumProcessOrder.RETURN_REJECTED);
        process.setCreatedAt(new Date());
        processOrderRepository.save(process);

        order.getProcessOrders().add(process);
        order.setStatus(EnumProcessOrder.RETURN_REJECTED);
        // Maybe append reason to order note or somewhere specific?
        // Current requirement doesn't specify where to store rejection reason aside
        // from log or overwriting reason/note.
        // I will append it to note for visibility.
        String existingNote = order.getNote() != null ? order.getNote() : "";
        order.setNote(existingNote + " | REJECT REASON: " + reason);

        return mapToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse confirmReturn(Long orderId) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!EnumProcessOrder.RETURN_ACCEPTED.equals(order.getStatus())) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 1. Update Order Status
        ProcessOrder process = new ProcessOrder();
        process.setOrder(order);
        process.setStatus(EnumProcessOrder.RETURNED);
        process.setCreatedAt(new Date());
        processOrderRepository.save(process);

        order.getProcessOrders().add(process);
        order.setStatus(EnumProcessOrder.RETURNED);

        // 2. Refund Payment
        Payment payment = order.getPayment();
        if (payment != null) {
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            // 3. Refund to Wallet
            try {
                // Assuming full refund of total order value
                Double refundAmount = order.getTotal();
                userClient.refundToWallet(order.getUserId(), refundAmount);
                log.info("Refunded {} to wallet for user {}", refundAmount, order.getUserId());
            } catch (Exception e) {
                log.error("Failed to refund to wallet for order {}: {}", orderId, e.getMessage());
                // Should we throw exception to rollback?
                // YES, consistency is key. If wallet refund fails, transaction should probably
                // fail
                // OR we accept manual intervention.
                // For now, I'll catch and rethrow as AppException to trigger rollback if
                // strict.
                // Or if we want to allow 'RETURNED' state but 'PENDING REFUND', we might need
                // more states.
                // Given requirements, I'll throw to ensure system consistency (no status update
                // without refund success).
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
            }
        }

        // 4. Restore Stock
        try {
            for (OrderDetail detail : order.getOrderDetails()) {
                inventoryClient.restoreStock(detail.getProductColorId(), detail.getQuantity());
            }
            log.info("Restored stock for order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to restore stock for order {}: {}", orderId, e.getMessage());
            // Do not fail the return process if stock update fails, just log it.
            // Or should we fail? Usually financial consistency > inventory consistency.
            // I will choose to NOT fail, but log ERROR clearly.
        }

        return mapToResponse(orderRepository.save(order));
    }

}