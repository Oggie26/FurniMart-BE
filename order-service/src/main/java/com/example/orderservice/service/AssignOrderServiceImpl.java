package com.example.orderservice.service;
import lombok.extern.slf4j.Slf4j;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Payment;
import com.example.orderservice.entity.ProcessOrder;
import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.AIClient;
import com.example.orderservice.feign.InventoryClient;
import com.example.orderservice.feign.ProductClient;
import com.example.orderservice.feign.StoreClient;
import com.example.orderservice.feign.UserClient;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.PaymentRepository;
import com.example.orderservice.repository.ProcessOrderRepository;
import com.example.orderservice.response.*;
import com.example.orderservice.service.inteface.AssignOrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignOrderServiceImpl implements AssignOrderService {

    private final StoreClient storeClient;
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final UserClient userClient;
    private final ProcessOrderRepository processOrderRepository;
    private final QRCodeService qrCodeService;
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final ProductClient productClient;
    private final AIClient aiClient;
    // private final KafkaTemplate<String, OrderAssignedEvent> kafkaTemplate;

    @Override
    @Transactional
    public void assignOrderToStore(Long orderId) {

        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        AddressResponse address = safeGetAddress(order.getAddressId());

        if (order.getPayment().getPaymentMethod() == null){
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }
        if (address == null) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        order.setStoreId(getStoreNear(address.getLatitude(), address.getLongitude(), 1));
        order.setStatus(EnumProcessOrder.ASSIGN_ORDER_STORE);

        ProcessOrder process = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.ASSIGN_ORDER_STORE)
                .createdAt(new Date())
                .build();

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
                .paymentMethod(order.getPayment().getPaymentMethod() != null ? order.getPayment().getPaymentMethod() : null )
                .items(orderItems)
                .build();

        try {
            kafkaTemplate.send("store-assigned-topic", event)
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
        processOrderRepository.save(process);
        orderRepository.save(order);

    }

    @SuppressWarnings("unused")
    private StoreResponse getStoreResponse(String storeId) {
        try {
            ApiResponse<StoreResponse> response = storeClient.getStoreById(storeId);
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("Error getting store {}: {}", storeId, e.getMessage());
        }
        return null;
    }

    @Override
    @Transactional
    public void acceptRejectOrderByManager(Long orderId, String storeId, String reason, EnumProcessOrder status) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (status == EnumProcessOrder.MANAGER_ACCEPT) {
            handleManagerAccept(order, storeId);
        } else if (status == EnumProcessOrder.MANAGER_REJECT) {
            // Nếu không truyền storeId → lấy storeId hiện tại của order
            String rejectedStoreId = (storeId != null && !storeId.isBlank())
                    ? storeId
                    : order.getStoreId();

            handleManagerReject(order, rejectedStoreId, reason);
        } else {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
    }

    private void handleManagerAccept(Order order, String storeId) {
        QRCodeService.QRCodeResult qrCodeResult = qrCodeService.generateQRCode(order.getId());

        ProcessOrder acceptProcess = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.MANAGER_ACCEPT)
                .createdAt(new Date())
                .build();
        processOrderRepository.save(acceptProcess);

        order.setStatus(EnumProcessOrder.MANAGER_ACCEPT);
        order.setQrCode(qrCodeResult.getQrCodeString());
        order.setQrCodeGeneratedAt(new Date());
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        Order savedOrder = orderRepository.save(order);

        if (payment.getPaymentMethod().equals(PaymentMethod.COD)) {
            List<OrderCreatedEvent.OrderItem> eventItems = savedOrder.getOrderDetails().stream()
                    .map(detail -> OrderCreatedEvent.OrderItem.builder()
                            .productColorId(detail.getProductColorId())
                            .quantity(detail.getQuantity())
                            .price(detail.getPrice())
                            .productName(detail.getProductColorId())
                            .colorName("")
                            .build())
                    .collect(Collectors.toList());

            ApiResponse<UserResponse> userResponse = userClient.getUserById(order.getUserId());
            if (userResponse == null || userResponse.getData() == null) {
                throw new AppException(ErrorCode.NOT_FOUND_USER);
            }

            UserResponse userData = userResponse.getData();
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .email(userData.getEmail())
                    .fullName(userData.getFullName())
                    .orderDate(savedOrder.getOrderDate())
                    .totalPrice(savedOrder.getTotal())
                    .orderId(savedOrder.getId())
                    .storeId(savedOrder.getStoreId())
                    .addressLine(safeGetAddress(order.getAddressId()).getAddressLine())
                    .paymentMethod(order.getPayment().getPaymentMethod())
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

            orderRepository.save(order);
        }
    }

    private void handleManagerReject(Order order, String rejectedStoreId, String reason) {
        ProcessOrder rejectProcess = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.MANAGER_REJECT)
                .createdAt(new Date())
                .build();
        processOrderRepository.save(rejectProcess);

        int currentRejectionCount = (order.getRejectionCount() != null ? order.getRejectionCount() : 0);
        order.setRejectionCount(currentRejectionCount + 1);
        order.setLastRejectedStoreId(rejectedStoreId);
        order.setReason(reason);

        log.info("📊 Order {} bị reject lần thứ {} bởi store {}",
                order.getId(), order.getRejectionCount(), rejectedStoreId);

        if (order.getRejectionCount() >= 3) {
            log.warn("❌ Order {} đã bị reject {} lần → TỰ ĐỘNG HỦY",
                    order.getId(), order.getRejectionCount());

            order.setStatus(EnumProcessOrder.CANCELLED);
            order.setReason("Đơn hàng bị hủy tự động: Đã bị từ chối bởi " +
                    order.getRejectionCount() + " cửa hàng");

            ProcessOrder cancelProcess = ProcessOrder.builder()
                    .order(order)
                    .status(EnumProcessOrder.CANCELLED)
                    .createdAt(new Date())
                    .build();
            processOrderRepository.save(cancelProcess);

            orderRepository.save(order);

            return;
        }

        try {
            String newStoreId = findBestStoreWithAI(order, rejectedStoreId);

            if (newStoreId != null) {
                log.info("🤖 AI recommend store mới: {} cho order {}", newStoreId, order.getId());

                order.setStoreId(newStoreId);
                order.setStatus(EnumProcessOrder.ASSIGN_ORDER_STORE);

                ProcessOrder assignProcess = ProcessOrder.builder()
                        .order(order)
                        .status(EnumProcessOrder.ASSIGN_ORDER_STORE)
                        .createdAt(new Date())
                        .build();
                processOrderRepository.save(assignProcess);

                orderRepository.save(order);

                log.info("✅ Đã assign order {} sang store {} (AI-powered)",
                        order.getId(), newStoreId);
            } else {
                log.warn("⚠️ AI không tìm được store phù hợp → Cancel order {}", order.getId());

                order.setStatus(EnumProcessOrder.CANCELLED);
                order.setReason("Không tìm được cửa hàng phù hợp có đủ hàng");

                ProcessOrder cancelProcess = ProcessOrder.builder()
                        .order(order)
                        .status(EnumProcessOrder.CANCELLED)
                        .createdAt(new Date())
                        .build();
                processOrderRepository.save(cancelProcess);

                orderRepository.save(order);
            }
        } catch (Exception e) {
            log.error("❌ AI service error: {}", e.getMessage());
            order.setStatus(EnumProcessOrder.CANCELLED);
            order.setReason("Lỗi hệ thống khi tìm cửa hàng mới: " + e.getMessage());
            orderRepository.save(order);
        }
    }

    private String findBestStoreWithAI(Order order, String rejectedStoreId) {
        try {
            AddressResponse customerAddress = safeGetAddress(order.getAddressId());
            if (customerAddress == null) {
                log.warn("⚠️ Không lấy được địa chỉ khách hàng");
                return null;
            }

            log.info("🤖 Calling AI Service for order {}", order.getId());

            List<String> rejectedStores = new ArrayList<>();
            rejectedStores.add(rejectedStoreId);
            if (order.getLastRejectedStoreId() != null) {
                rejectedStores.add(order.getLastRejectedStoreId());
            }

            List<Map<String, Object>> orderItems = order.getOrderDetails().stream()
                    .map(detail -> Map.<String, Object>of(
                            "productColorId", detail.getProductColorId(),
                            "quantity", detail.getQuantity()))
                    .toList();

            Map<String, Object> addressMap = Map.of(
                    "addressId", customerAddress.getId(),
                    "latitude", customerAddress.getLatitude(),
                    "longitude", customerAddress.getLongitude(),
                    "addressLine", customerAddress.getAddressLine());

            Map<String, Object> aiRequest = Map.of(
                    "orderId", order.getId(),
                    "rejectedStoreIds", rejectedStores,
                    "orderItems", orderItems,
                    "customerAddress", addressMap);

            // Call AI Service
            ApiResponse<AIStoreRecommendationResponse> aiResponse = aiClient.recommendStore(aiRequest);

            if (aiResponse != null && aiResponse.getData() != null) {
                AIStoreRecommendationResponse recommendation = aiResponse.getData();

                log.info("✅ AI recommend: {} (confidence: {}%, score: {}/100)",
                        recommendation.getRecommendedStoreId(),
                        recommendation.getConfidence() * 100,
                        recommendation.getScore());

                return recommendation.getRecommendedStoreId();
            } else {
                log.warn("⚠️ AI không tìm được store phù hợp");
                return null;
            }

        } catch (Exception e) {
            log.error("❌ AI Service error: {}", e.getMessage());
            log.info("🔄 Fallback to traditional algorithm");
            AddressResponse address = safeGetAddress(order.getAddressId());
            return findBestStoreFallback(order, rejectedStoreId, address);
        }
    }

    private String findBestStoreFallback(Order order, String rejectedStoreId, AddressResponse address) {
        if (address == null)
            return null;

        ApiResponse<List<StoreDistance>> response = storeClient.getNearestStores(
                address.getLatitude(),
                address.getLongitude(),
                5);

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            return null;
        }

        List<StoreDistance> candidates = response.getData().stream()
                .filter(sd -> !sd.getStore().getId().equals(rejectedStoreId))
                .filter(sd -> !sd.getStore().getId().equals(order.getLastRejectedStoreId()))
                .toList();

        for (StoreDistance candidate : candidates) {
            String storeId = candidate.getStore().getId();

            boolean hasAllProducts = order.getOrderDetails().stream().allMatch(detail -> {
                try {
                    ApiResponse<Boolean> stockCheck = inventoryClient.checkStockAtStore(
                            detail.getProductColorId(),
                            storeId,
                            detail.getQuantity());
                    return stockCheck != null && stockCheck.getData() != null && stockCheck.getData();
                } catch (Exception e) {
                    log.warn("Error checking stock: {}", e.getMessage());
                    return false;
                }
            });

            if (hasAllProducts) {
                log.info("✅ Tìm thấy store {} có đủ hàng (distance: {}km)",
                        storeId, candidate.getDistance());
                return storeId;
            }
        }

        log.warn("⚠️ Không có store nào trong top 10 có đủ hàng");
        return null;
    }

    @SuppressWarnings("unused")
    private List<InventoryResponse> getInventoryResponse(String productId) {
        ApiResponse<List<InventoryResponse>> response = inventoryClient.getInventoryByProduct(productId);
        return response.getData();
    }

    @SuppressWarnings("unused")
    private String getStoreById(String storeId) {
        ApiResponse<StoreResponse> response = storeClient.getStoreById(storeId);
        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }
        return response.getData().getId();
    }

    private String getStoreNear(Double lat, Double lon, int limit) {
        ApiResponse<List<StoreDistance>> response = storeClient.getNearestStores(lat, lon, limit);
        System.out.println(response);
        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        return response.getData().getFirst().getStore().getId();
    }

    private AddressResponse safeGetAddress(Long addressId) {
        if (addressId == null)
            return null;
        ApiResponse<AddressResponse> resp = userClient.getAddressById(addressId);
        if (resp == null || resp.getData() == null)
            return null;
        return resp.getData();
    }

    @SuppressWarnings("unused")
    private UserResponse safeGetUser(String userId) {
        if (userId == null)
            return null;
        try {
            ApiResponse<UserResponse> response = userClient.getUserById(userId);
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("Error getting user {}: {}", userId, e.getMessage());
        }
        return null;
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

}
