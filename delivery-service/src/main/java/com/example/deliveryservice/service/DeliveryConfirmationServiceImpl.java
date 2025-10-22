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
import com.example.deliveryservice.response.DeliveryConfirmationResponse;
import com.example.deliveryservice.service.inteface.DeliveryConfirmationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

        String qrCode = generateQRCodeForOrder(request.getOrderId());

        DeliveryConfirmation confirmation = DeliveryConfirmation.builder()
                .orderId(request.getOrderId())
                .deliveryStaffId(deliveryStaffId)
                .customerId(null)
                .deliveryPhotos(deliveryPhotosJson)
                .deliveryNotes(request.getDeliveryNotes())
                .qrCode(qrCode)
                .deliveryLatitude(request.getDeliveryLatitude())
                .deliveryLongitude(request.getDeliveryLongitude())
                .deliveryAddress(request.getDeliveryAddress())
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
                .orElseThrow(() -> new AppException(ErrorCode.CODE_NOT_FOUND));
        return toDeliveryConfirmationResponse(confirmation);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryConfirmationResponse getDeliveryConfirmationByQRCode(String qrCode) {
        DeliveryConfirmation confirmation = deliveryConfirmationRepository.findByQrCodeAndIsDeletedFalse(qrCode)
                .orElseThrow(() -> new AppException(ErrorCode.CODE_NOT_FOUND));
        return toDeliveryConfirmationResponse(confirmation);
    }

    @Override
    @Transactional
    public DeliveryConfirmationResponse scanQRCode(QRCodeScanRequest request) {
        log.info("Scanning QR code: {}", request.getQrCode());

        DeliveryConfirmation confirmation = deliveryConfirmationRepository.findByQrCodeAndIsDeletedFalse(request.getQrCode())
                .orElseThrow(() -> new AppException(ErrorCode.CODE_NOT_FOUND));

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

    @Override
    public String generateQRCodeForOrder(Long orderId) {
        try {
            String data = "ORDER_" + orderId + "_" + LocalDateTime.now();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return "QR_" + hexString.substring(0, 16).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating QR code", e);
            return "QR_" + orderId + "_" + System.currentTimeMillis();
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
                .deliveryLatitude(confirmation.getDeliveryLatitude())
                .deliveryLongitude(confirmation.getDeliveryLongitude())
                .deliveryAddress(confirmation.getDeliveryAddress())
                .isQrCodeScanned(confirmation.getQrCodeScannedAt() != null)
                .createdAt(confirmation.getCreatedAt())
                .updatedAt(confirmation.getUpdatedAt())
                .build();
    }
}


