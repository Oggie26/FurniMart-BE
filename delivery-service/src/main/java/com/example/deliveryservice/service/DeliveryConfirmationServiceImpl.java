package com.example.deliveryservice.service;

import com.example.deliveryservice.entity.DeliveryAssignment;
import com.example.deliveryservice.entity.DeliveryConfirmation;
import com.example.deliveryservice.enums.DeliveryConfirmationStatus;
import com.example.deliveryservice.enums.DeliveryStatus;
import com.example.deliveryservice.enums.EnumProcessOrder;
import com.example.deliveryservice.exception.AppException;
import com.example.deliveryservice.enums.ErrorCode;
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
import org.hibernate.query.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
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

    @Override
    @Transactional
    public DeliveryConfirmationResponse createDeliveryConfirmation(DeliveryConfirmationRequest request) {
        log.info("Creating delivery confirmation for order: {}", request.getOrderId());

        var orderResponse = orderClient.getOrderById(request.getOrderId());

        String customerId = null;
        if (orderResponse != null && orderResponse.getBody().getData() != null) {
            customerId = orderResponse.getBody().getData().getAddress().getUserId();
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

        DeliveryConfirmation savedConfirmation = deliveryConfirmationRepository.save(confirmation);
        DeliveryAssignment deliveryAssignment =  deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        deliveryAssignment.setStatus(DeliveryStatus.DELIVERED);
        deliveryAssignmentRepository.save(deliveryAssignment);

        try {
            orderClient.updateOrderStatus(request.getOrderId(), EnumProcessOrder.FINISHED);
            orderClient.confirmCodPayment(request.getOrderId());
            warrantyClient.generateWarranties(request.getOrderId());
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

        DeliveryConfirmation confirmation = deliveryConfirmationRepository.findByQrCodeAndIsDeletedFalse(request.getQrCode())
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
        } catch (Exception ex) {
            log.warn("Failed to update order {} to FINISHED: {}", confirmation.getOrderId(), ex.getMessage());
        }

        log.info("QR code scanned successfully for order: {}", confirmation.getOrderId());
        return toDeliveryConfirmationResponse(savedConfirmation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryConfirmationResponse> getDeliveryConfirmationsByStaff(String deliveryStaffId) {
        List<DeliveryConfirmation> confirmations = deliveryConfirmationRepository.findByDeliveryStaffIdOrderByDeliveryDateDesc(deliveryStaffId);
        return confirmations.stream()
                .map(this::toDeliveryConfirmationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryConfirmationResponse> getDeliveryConfirmationsByCustomer(String customerId) {
        List<DeliveryConfirmation> confirmations = deliveryConfirmationRepository.findByCustomerIdOrderByDeliveryDateDesc(customerId);
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
            if (response.getBody() != null && response.getBody().getData() != null) {
                String qrCode = response.getBody().getData().getQrCode();
                if (qrCode != null && !qrCode.isEmpty()) {
                    return qrCode;
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
                List<String> photos = (List<String>) objectMapper.readValue(confirmation.getDeliveryPhotos(), List.class);
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
}


