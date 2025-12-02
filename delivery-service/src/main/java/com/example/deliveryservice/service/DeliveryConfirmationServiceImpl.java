package com.example.deliveryservice.service;

import com.example.deliveryservice.entity.DeliveryConfirmation;
import com.example.deliveryservice.enums.DeliveryConfirmationStatus;
import com.example.deliveryservice.enums.EnumProcessOrder;
import com.example.deliveryservice.exception.AppException;
import com.example.deliveryservice.enums.ErrorCode;
import com.example.deliveryservice.feign.OrderClient;
import com.example.deliveryservice.feign.WarrantyClient;
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
    private final ObjectMapper objectMapper;
    private final OrderClient orderClient;
    private final WarrantyClient warrantyClient;

    @Override
    @Transactional
    public DeliveryConfirmationResponse createDeliveryConfirmation(DeliveryConfirmationRequest request) {
        log.info("Creating delivery confirmation for order: {}", request.getOrderId());

        // 1. GỌI SANG ORDER SERVICE ĐỂ LẤY THÔNG TIN KHÁCH HÀNG (SỬA Ở ĐÂY)
        // Giả sử bạn có hàm getOrderById trong OrderClient trả về ApiResponse<OrderResponse> hoặc OrderResponse
        // Bạn cần lấy userId từ order này.
        var orderResponse = orderClient.getOrderById(request.getOrderId());

        // Tùy cấu trúc Feign Client của bạn mà lấy data ra (ví dụ .getData() hoặc lấy trực tiếp)
        String customerId = null;
        if (orderResponse != null && orderResponse.getBody().getData() != null) {
            customerId = orderResponse.getBody().getData().getAddress().getUserId();
        }

        // Validate: Nếu không tìm thấy khách hàng thì không cho tạo xác nhận
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

        // Có thể tận dụng orderResponse ở trên để lấy QR Code luôn nếu có, đỡ phải query 2 lần
        String qrCode = getQRCodeFromOrder(request.getOrderId());

        DeliveryConfirmation confirmation = DeliveryConfirmation.builder()
                .orderId(request.getOrderId())
                .deliveryStaffId(deliveryStaffId)
                .customerId(customerId) // <--- ĐÃ FIX: Truyền ID lấy từ Order Service vào
                .deliveryPhotos(deliveryPhotosJson)
                .deliveryNotes(request.getDeliveryNotes())
                .qrCode(qrCode)
                .status(DeliveryConfirmationStatus.DELIVERED)
                .build();

        DeliveryConfirmation savedConfirmation = deliveryConfirmationRepository.save(confirmation);

        // Update order status and generate warranties via order-service
        try {
            orderClient.updateOrderStatus(request.getOrderId(), EnumProcessOrder.DELIVERED);
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
        confirmation.setStatus(DeliveryConfirmationStatus.CONFIRMED);

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


