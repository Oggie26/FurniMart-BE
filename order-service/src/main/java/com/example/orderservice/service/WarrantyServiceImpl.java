package com.example.orderservice.service;

import com.example.orderservice.entity.*;
import com.example.orderservice.enums.WarrantyClaimStatus;
import com.example.orderservice.enums.WarrantyStatus;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.repository.*;
import com.example.orderservice.request.WarrantyClaimRequest;
import com.example.orderservice.response.WarrantyClaimResponse;
import com.example.orderservice.response.WarrantyResponse;
import com.example.orderservice.service.inteface.WarrantyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WarrantyServiceImpl implements WarrantyService {

    private final WarrantyRepository warrantyRepository;
    private final WarrantyClaimRepository warrantyClaimRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void createWarrantiesForOrder(Long orderId) {
        log.info("Creating warranties for order: {}", orderId);
        
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderIdAndIsDeletedFalse(orderId);
        
        for (OrderDetail orderDetail : orderDetails) {
            // Check if warranty already exists for this order detail
            if (warrantyRepository.findByOrderIdAndOrderDetailId(orderId, orderDetail.getId()).isEmpty()) {
                Warranty warranty = Warranty.builder()
                        .orderId(orderId)
                        .orderDetailId(orderDetail.getId())
                        .productColorId(orderDetail.getProductColorId())
                        .customerId(order.getUserId())
                        .deliveryDate(LocalDateTime.now()) // This should be set when order is actually delivered
                        .warrantyDurationMonths(24) // 2 years
                        .description("Standard 2-year warranty")
                        .build();
                
                warrantyRepository.save(warranty);
                log.info("Created warranty for order detail: {}", orderDetail.getId());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyResponse> getWarrantiesByCustomer(String customerId) {
        List<Warranty> warranties = warrantyRepository.findByCustomerIdAndIsDeletedFalse(customerId);
        return warranties.stream()
                .map(this::toWarrantyResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyResponse> getActiveWarrantiesByCustomer(String customerId) {
        List<Warranty> warranties = warrantyRepository.findActiveWarrantiesByCustomer(customerId, LocalDateTime.now());
        return warranties.stream()
                .map(this::toWarrantyResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WarrantyResponse getWarrantyById(Long warrantyId) {
        Warranty warranty = warrantyRepository.findByIdAndIsDeletedFalse(warrantyId)
                .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_NOT_FOUND));
        return toWarrantyResponse(warranty);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyResponse> getWarrantiesByOrder(Long orderId) {
        List<Warranty> warranties = warrantyRepository.findByOrderIdAndIsDeletedFalse(orderId);
        return warranties.stream()
                .map(this::toWarrantyResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WarrantyClaimResponse createWarrantyClaim(WarrantyClaimRequest request) {
        log.info("Creating warranty claim for warranty: {}", request.getWarrantyId());
        
        Warranty warranty = warrantyRepository.findByIdAndIsDeletedFalse(request.getWarrantyId())
                .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_NOT_FOUND));
        
        if (!warranty.canClaimWarranty()) {
            throw new AppException(ErrorCode.WARRANTY_CANNOT_BE_CLAIMED);
        }
        
        String customerPhotosJson = null;
        if (request.getCustomerPhotos() != null && !request.getCustomerPhotos().isEmpty()) {
            try {
                customerPhotosJson = objectMapper.writeValueAsString(request.getCustomerPhotos());
            } catch (JsonProcessingException e) {
                log.error("Error serializing customer photos", e);
            }
        }
        
        WarrantyClaim claim = WarrantyClaim.builder()
                .warrantyId(request.getWarrantyId())
                .customerId(warranty.getCustomerId())
                .issueDescription(request.getIssueDescription())
                .customerPhotos(customerPhotosJson)
                .status(WarrantyClaimStatus.PENDING)
                .build();
        
        WarrantyClaim savedClaim = warrantyClaimRepository.save(claim);
        
        // Increment claim count on warranty
        warranty.incrementClaimCount();
        warrantyRepository.save(warranty);
        
        log.info("Created warranty claim: {}", savedClaim.getId());
        return toWarrantyClaimResponse(savedClaim);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyClaimResponse> getWarrantyClaimsByCustomer(String customerId) {
        List<WarrantyClaim> claims = warrantyClaimRepository.findByCustomerIdOrderByClaimDateDesc(customerId);
        return claims.stream()
                .map(this::toWarrantyClaimResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyClaimResponse> getWarrantyClaimsByWarranty(Long warrantyId) {
        List<WarrantyClaim> claims = warrantyClaimRepository.findByWarrantyIdOrderByClaimDateDesc(warrantyId);
        return claims.stream()
                .map(this::toWarrantyClaimResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WarrantyClaimResponse updateWarrantyClaimStatus(Long claimId, String status, String adminResponse, String resolutionNotes) {
        log.info("Updating warranty claim status: {} to {}", claimId, status);
        
        WarrantyClaim claim = warrantyClaimRepository.findByIdAndIsDeletedFalse(claimId)
                .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_CLAIM_NOT_FOUND));
        
        try {
            WarrantyClaimStatus newStatus = WarrantyClaimStatus.valueOf(status.toUpperCase());
            claim.setStatus(newStatus);
            claim.setAdminResponse(adminResponse);
            claim.setResolutionNotes(resolutionNotes);
            
            if (newStatus == WarrantyClaimStatus.RESOLVED) {
                claim.setResolvedDate(LocalDateTime.now());
            }
            
            // Get current admin user ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof String) {
                claim.setAdminId(authentication.getName());
            }
            
            WarrantyClaim savedClaim = warrantyClaimRepository.save(claim);
            log.info("Updated warranty claim status: {}", savedClaim.getId());
            
            return toWarrantyClaimResponse(savedClaim);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyClaimResponse> getAllWarrantyClaims() {
        List<WarrantyClaim> claims = warrantyClaimRepository.findByIsDeletedFalse();
        return claims.stream()
                .map(this::toWarrantyClaimResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyClaimResponse> getWarrantyClaimsByStatus(String status) {
        try {
            WarrantyClaimStatus claimStatus = WarrantyClaimStatus.valueOf(status.toUpperCase());
            List<WarrantyClaim> claims = warrantyClaimRepository.findByStatusAndIsDeletedFalse(claimStatus);
            return claims.stream()
                    .map(this::toWarrantyClaimResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
    }

    @Override
    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    @Transactional
    public void expireWarranties() {
        log.info("Running scheduled task to expire warranties");
        
        List<Warranty> expiredWarranties = warrantyRepository.findExpiredWarranties(LocalDateTime.now());
        
        for (Warranty warranty : expiredWarranties) {
            warranty.setStatus(WarrantyStatus.EXPIRED);
            warrantyRepository.save(warranty);
            log.info("Expired warranty: {}", warranty.getId());
        }
        
        log.info("Expired {} warranties", expiredWarranties.size());
    }

    private WarrantyResponse toWarrantyResponse(Warranty warranty) {
        return WarrantyResponse.builder()
                .id(warranty.getId())
                .orderId(warranty.getOrderId())
                .orderDetailId(warranty.getOrderDetailId())
                .productColorId(warranty.getProductColorId())
                .customerId(warranty.getCustomerId())
                .deliveryDate(warranty.getDeliveryDate())
                .warrantyStartDate(warranty.getWarrantyStartDate())
                .warrantyEndDate(warranty.getWarrantyEndDate())
                .status(warranty.getStatus())
                .warrantyDurationMonths(warranty.getWarrantyDurationMonths())
                .description(warranty.getDescription())
                .claimCount(warranty.getClaimCount())
                .maxClaims(warranty.getMaxClaims())
                .isActive(warranty.isActive())
                .canClaimWarranty(warranty.canClaimWarranty())
                .createdAt(warranty.getCreatedAt())
                .updatedAt(warranty.getUpdatedAt())
                .build();
    }

    private WarrantyClaimResponse toWarrantyClaimResponse(WarrantyClaim claim) {
        List<String> customerPhotos = null;
        if (claim.getCustomerPhotos() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<String> photos = (List<String>) objectMapper.readValue(claim.getCustomerPhotos(), List.class);
                customerPhotos = photos;
            } catch (JsonProcessingException e) {
                log.error("Error deserializing customer photos", e);
            }
        }
        
        List<String> resolutionPhotos = null;
        if (claim.getResolutionPhotos() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<String> photos = (List<String>) objectMapper.readValue(claim.getResolutionPhotos(), List.class);
                resolutionPhotos = photos;
            } catch (JsonProcessingException e) {
                log.error("Error deserializing resolution photos", e);
            }
        }
        
        return WarrantyClaimResponse.builder()
                .id(claim.getId())
                .warrantyId(claim.getWarrantyId())
                .customerId(claim.getCustomerId())
                .claimDate(claim.getClaimDate())
                .issueDescription(claim.getIssueDescription())
                .customerPhotos(customerPhotos)
                .status(claim.getStatus())
                .adminResponse(claim.getAdminResponse())
                .resolutionNotes(claim.getResolutionNotes())
                .resolutionPhotos(resolutionPhotos)
                .resolvedDate(claim.getResolvedDate())
                .adminId(claim.getAdminId())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }
}
