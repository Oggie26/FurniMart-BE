package com.example.deliveryservice.service;

import com.example.deliveryservice.entity.DeliveryAssignment;
import com.example.deliveryservice.entity.DeliveryConfirmation;
import com.example.deliveryservice.enums.DeliveryConfirmationStatus;
import com.example.deliveryservice.enums.DeliveryStatus;
import com.example.deliveryservice.enums.EnumProcessOrder;
import com.example.deliveryservice.exception.AppException;
import com.example.deliveryservice.enums.ErrorCode;
import com.example.deliveryservice.event.OrderDeliveredEvent;
import com.example.deliveryservice.feign.OrderClient;
import com.example.deliveryservice.feign.WarrantyClient;
import com.example.deliveryservice.repository.DeliveryAssignmentRepository;
import com.example.deliveryservice.repository.DeliveryConfirmationRepository;
import com.example.deliveryservice.request.DeliveryConfirmationRequest;
import com.example.deliveryservice.request.QRCodeScanRequest;
import com.example.deliveryservice.response.ApiResponse;
import com.example.deliveryservice.response.DeliveryConfirmationResponse;
import com.example.deliveryservice.response.OrderResponse;
import com.example.deliveryservice.service.inteface.DeliveryConfirmationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeliveryConfirmationServiceImpl implements DeliveryConfirmationService {

    private final DeliveryConfirmationRepository deliveryConfirmationRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final ObjectMapper objectMapper;
    private final OrderClient orderClient;
    private final WarrantyClient warrantyClient;
    private final KafkaTemplate<String, Object> genericKafkaTemplate;

    @Override
    @Transactional
    public DeliveryConfirmationResponse createDeliveryConfirmation(DeliveryConfirmationRequest request) {
        log.info("Creating delivery confirmation for order: {}", request.getOrderId());

        // ✅ KIỂM TRA: Đã có delivery confirmation chưa? (Idempotency)
        Optional<DeliveryConfirmation> existingConfirmation = 
            deliveryConfirmationRepository.findByOrderIdAndIsDeletedFalse(request.getOrderId());
        
        if (existingConfirmation.isPresent()) {
            log.warn("Delivery confirmation already exists for order: {}. Returning existing confirmation.", 
                     request.getOrderId());
            return toDeliveryConfirmationResponse(existingConfirmation.get());
        }

        var orderResponse = orderClient.getOrderById(request.getOrderId());

        String customerId = null;
        if (orderResponse != null) {
            var body = orderResponse.getBody();
            if (body != null && body.getData() != null && body.getData().getAddress() != null) {
                customerId = body.getData().getAddress().getUserId();
            }
        }

        if (customerId == null) {
            throw new RuntimeException("Không tìm thấy thông tin khách hàng cho đơn hàng: " + request.getOrderId());
        }

        String deliveryPhotosJson = null;
        if (request.getDeliveryPhotos() != null && !request.getDeliveryPhotos().isEmpty()) {
            try {
                deliveryPhotosJson = objectMapper.writeValueAsString(request.getDeliveryPhotos());
            } catch (JsonProcessingException e) {
                log.error("Error serializing delivery photos", e);
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String deliveryStaffId = authentication.getName();

        String qrCode = getQRCodeFromOrder(request.getOrderId());

        DeliveryConfirmation confirmation = DeliveryConfirmation.builder()
                .orderId(request.getOrderId())
                .deliveryStaffId(deliveryStaffId)
                .customerId(customerId)
                .deliveryPhotos(deliveryPhotosJson)
                .deliveryNotes(request.getDeliveryNotes())
                .qrCode(qrCode)
                .status(DeliveryConfirmationStatus.DELIVERED)
                .build();

        DeliveryConfirmation savedConfirmation;
        try {
            savedConfirmation = deliveryConfirmationRepository.save(confirmation);
        } catch (DataIntegrityViolationException e) {
            // ✅ Xử lý duplicate QR code
            if (e.getMessage() != null && e.getMessage().contains("qr_code")) {
                log.warn("Duplicate QR code detected for order: {}. Fetching existing confirmation.", 
                         request.getOrderId());
                existingConfirmation = 
                    deliveryConfirmationRepository.findByOrderIdAndIsDeletedFalse(request.getOrderId());
                if (existingConfirmation.isPresent()) {
                    return toDeliveryConfirmationResponse(existingConfirmation.get());
                }
            }
            throw e;
        }
        
        DeliveryAssignment deliveryAssignment = deliveryAssignmentRepository
                .findByOrderIdAndIsDeletedFalse(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        deliveryAssignment.setStatus(DeliveryStatus.DELIVERED);
        deliveryAssignmentRepository.save(deliveryAssignment);

        try {
            orderClient.updateOrderStatus(request.getOrderId(), EnumProcessOrder.FINISHED);
            orderClient.confirmCodPayment(request.getOrderId());
            warrantyClient.generateWarranties(request.getOrderId());

            // Send Kafka Notification
            sendDeliveryNotification(request.getOrderId(), deliveryStaffId, new Date());

        } catch (Exception ex) {
            log.warn("Post-delivery side-effects failed for order {}: {}", request.getOrderId(), ex.getMessage());
        }

        log.info("Created delivery confirmation: {} for order: {}", savedConfirmation.getId(), request.getOrderId());
        return toDeliveryConfirmationResponse(savedConfirmation);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryConfirmationResponse getDeliveryConfirmationByOrderId(Long orderId) {
        DeliveryConfirmation confirmation = deliveryConfirmationRepository.findByOrderIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.DELIVERY_CONFIRMATION_NOT_FOUND));
        return toDeliveryConfirmationResponse(confirmation);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryConfirmationResponse getDeliveryConfirmationByQRCode(String qrCode) {
        DeliveryConfirmation confirmation = deliveryConfirmationRepository.findByQrCodeAndIsDeletedFalse(qrCode)
                .orElseThrow(() -> new AppException(ErrorCode.DELIVERY_CONFIRMATION_NOT_FOUND));
        return toDeliveryConfirmationResponse(confirmation);
    }

    @Override
    @Transactional
    public DeliveryConfirmationResponse scanQRCode(QRCodeScanRequest request) {
        log.info("Scanning QR code: {}", request.getQrCode());

        DeliveryConfirmation confirmation = deliveryConfirmationRepository
                .findByQrCodeAndIsDeletedFalse(request.getQrCode())
                .orElseThrow(() -> new AppException(ErrorCode.DELIVERY_CONFIRMATION_NOT_FOUND));

        if (confirmation.getQrCodeScannedAt() != null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getCustomerSignature() != null) {
            confirmation.setCustomerSignature(request.getCustomerSignature());
        }

        confirmation.setQrCodeScannedAt(LocalDateTime.now());
        confirmation.setStatus(DeliveryConfirmationStatus.DELIVERED);

        DeliveryConfirmation savedConfirmation = deliveryConfirmationRepository.save(confirmation);

        // Set order status to FINISHED
        try {
            orderClient.updateOrderStatus(confirmation.getOrderId(), EnumProcessOrder.FINISHED);

            // Send Kafka Notification
            // Delivery staff ID might not be in context if customer scans?
            // DeliveryConfirmation has deliveryStaffId.
            sendDeliveryNotification(confirmation.getOrderId(), confirmation.getDeliveryStaffId(), new Date());

        } catch (Exception ex) {
            log.warn("Failed to update order {} to FINISHED: {}", confirmation.getOrderId(), ex.getMessage());
        }

        log.info("QR code scanned successfully for order: {}", confirmation.getOrderId());
        return toDeliveryConfirmationResponse(savedConfirmation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryConfirmationResponse> getDeliveryConfirmationsByStaff(String deliveryStaffId) {
        List<DeliveryConfirmation> confirmations = deliveryConfirmationRepository
                .findByDeliveryStaffIdOrderByDeliveryDateDesc(deliveryStaffId);
        return confirmations.stream()
                .map(this::toDeliveryConfirmationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryConfirmationResponse> getDeliveryConfirmationsByCustomer(String customerId) {
        List<DeliveryConfirmation> confirmations = deliveryConfirmationRepository
                .findByCustomerIdOrderByDeliveryDateDesc(customerId);
        return confirmations.stream()
                .map(this::toDeliveryConfirmationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryConfirmationResponse> getAllDeliveryConfirmations() {
        List<DeliveryConfirmation> confirmations = deliveryConfirmationRepository.findByIsDeletedFalse();
        return confirmations.stream()
                .map(this::toDeliveryConfirmationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryConfirmationResponse> getScannedConfirmations() {
        List<DeliveryConfirmation> confirmations = deliveryConfirmationRepository.findScannedConfirmations();
        return confirmations.stream()
                .map(this::toDeliveryConfirmationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryConfirmationResponse> getUnscannedConfirmations() {
        List<DeliveryConfirmation> confirmations = deliveryConfirmationRepository.findUnscannedConfirmations();
        return confirmations.stream()
                .map(this::toDeliveryConfirmationResponse)
                .collect(Collectors.toList());
    }

    private String getQRCodeFromOrder(Long orderId) {
        try {
            ResponseEntity<ApiResponse<OrderResponse>> response = orderClient.getOrderById(orderId);
            if (response != null) {
                var body = response.getBody();
                if (body != null && body.getData() != null) {
                    String qrCode = body.getData().getQrCode();
                    if (qrCode != null && !qrCode.isEmpty()) {
                        return qrCode;
                    }
                }
            }
            log.warn("QR code not found for order: {}", orderId);
            return "QR_NOT_FOUND_" + orderId;
        } catch (Exception e) {
            log.error("Error fetching QR code for order {}: {}", orderId, e.getMessage());
            return "QR_ERROR_" + orderId;
        }
    }

    private DeliveryConfirmationResponse toDeliveryConfirmationResponse(DeliveryConfirmation confirmation) {
        List<String> deliveryPhotos = null;
        if (confirmation.getDeliveryPhotos() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<String> photos = (List<String>) objectMapper.readValue(confirmation.getDeliveryPhotos(),
                        List.class);
                deliveryPhotos = photos;
            } catch (JsonProcessingException e) {
                log.error("Error deserializing delivery photos", e);
            }
        }

        return DeliveryConfirmationResponse.builder()
                .id(confirmation.getId())
                .orderId(confirmation.getOrderId())
                .deliveryStaffId(confirmation.getDeliveryStaffId())
                .customerId(confirmation.getCustomerId())
                .deliveryDate(confirmation.getDeliveryDate())
                .deliveryPhotos(deliveryPhotos)
                .deliveryNotes(confirmation.getDeliveryNotes())
                .qrCode(confirmation.getQrCode())
                .qrCodeGeneratedAt(confirmation.getQrCodeGeneratedAt())
                .qrCodeScannedAt(confirmation.getQrCodeScannedAt())
                .customerSignature(confirmation.getCustomerSignature())
                .status(confirmation.getStatus())
                .deliveryAddress(confirmation.getDeliveryAddress())
                .isQrCodeScanned(confirmation.getQrCodeScannedAt() != null)
                .createdAt(confirmation.getCreatedAt())
                .updatedAt(confirmation.getUpdatedAt())
                .build();
    }

    private void sendDeliveryNotification(Long orderId, String deliveryStaffId, Date deliveryDate) {
        try {
            var orderRes = orderClient.getOrderById(orderId);
            if (orderRes != null) {
                var responseBody = orderRes.getBody();
                if (responseBody != null && responseBody.getData() != null) {
                    var order = responseBody.getData();
                    if (order.getUser() != null) {
                    List<OrderDeliveredEvent.Item> items = Collections.emptyList();
                    if (order.getOrderDetails() != null) {
                        items = order.getOrderDetails().stream()
                                .map(d -> OrderDeliveredEvent.Item.builder()
                                        .productColorId(d.getProductColorId())
                                        .quantity(d.getQuantity())
                                        .price(d.getPrice())
                                        // Note: AddressResponse/OrderResponse in delivery-service might differ from
                                        // order-service
                                        // Assuming basic fields are available or defaulting
                                        .productName("Product " + d.getProductColorId()) // Placeholder if name not
                                                                                         // available
                                        .colorName("")
                                        .build())
                                .collect(Collectors.toList());
                        // If we have ProductColor object in OrderDetailResponse, use it.
                        // Checking OrderDetailResponse: usually has primitives unless populated.
                        // OrderDetailResponse in delivery-service (step 244) exists.
                        // We will check its content if we need better names.
                    }

                    OrderDeliveredEvent event = OrderDeliveredEvent.builder()
                            .orderId(orderId)
                            .email(order.getUser().getEmail())
                            .fullName(order.getUser().getFullName())
                            .deliveryDate(deliveryDate)
                            .deliveryStaffId(deliveryStaffId)
                            .totalAmount(order.getTotal())
                            .items(items)
                            .build();

                    genericKafkaTemplate.send("order-delivered-topic", event)
                            .whenComplete((result, ex) -> {
                                if (ex != null) {
                                    log.error("Failed to send order delivered event: {}", ex.getMessage());
                                } else {
                                    log.info("Sent order delivered event for order: {}", orderId);
                                }
                            });
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error sending delivery notification for order {}: {}", orderId, e.getMessage());
        }
    }
}
