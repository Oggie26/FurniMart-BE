package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Payment;
import com.example.orderservice.entity.ProcessOrder;
import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.InventoryClient;
import com.example.orderservice.feign.StoreClient;
import com.example.orderservice.feign.UserClient;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.PaymentRepository;
import com.example.orderservice.repository.ProcessOrderRepository;
import com.example.orderservice.response.*;
import com.example.orderservice.service.inteface.AssignOrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;
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
//    private final KafkaTemplate<String, OrderAssignedEvent> kafkaTemplate;

    @Override
    @Transactional
    public void assignOrderToStore(Long orderId) {

        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        AddressResponse address = safeGetAddress(order.getAddressId());

        if (address == null) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        order.setStoreId(getStoreNear(address.getLatitude(), address.getLongitude(), 1));
        order.setStatus(EnumProcessOrder.ASSIGN_ORDER_STORE);

        ProcessOrder process =  ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.ASSIGN_ORDER_STORE)
                .createdAt(new Date())
                .build();

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
            handleManagerReject(order,storeId, reason);
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

    private void handleManagerReject(Order order, String storeId, String reason) {
        ProcessOrder rejectProcess = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.MANAGER_REJECT)
                .createdAt(new Date())
                .build();
        processOrderRepository.save(rejectProcess);
        order.setReason(reason);
        order.setStoreId(storeId);
        order.setStatus(EnumProcessOrder.ASSIGN_ORDER_STORE);
        orderRepository.save(order);

        ProcessOrder assignProcess = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.ASSIGN_ORDER_STORE)
                .createdAt(new Date())
                .build();
        processOrderRepository.save(assignProcess);
    }

    @SuppressWarnings("unused")
    private List<InventoryResponse> getInventoryResponse(String productId) {
        ApiResponse<List<InventoryResponse>>response =  inventoryClient.getInventoryByProduct(productId);
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
        if (addressId == null) return null;
        ApiResponse<AddressResponse> resp = userClient.getAddressById(addressId);
        if (resp == null || resp.getData() == null) return null;
        return resp.getData();
    }

    @SuppressWarnings("unused")
    private UserResponse safeGetUser(String userId) {
        if (userId == null) return null;
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

}
