package com.example.orderservice.service;

import com.example.orderservice.entity.*;
import com.example.orderservice.enums.*;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.repository.*;
import com.example.orderservice.request.WarrantyClaimRequest;
import com.example.orderservice.request.WarrantyClaimResolutionRequest;
import com.example.orderservice.response.OrderResponse;
import com.example.orderservice.response.WarrantyClaimResponse;
import com.example.orderservice.response.WarrantyReportResponse;
import com.example.orderservice.response.WarrantyResponse;
import com.example.orderservice.response.AddressResponse;
import com.example.orderservice.response.PageResponse;
import com.example.orderservice.service.inteface.OrderService;
import com.example.orderservice.service.inteface.WarrantyService;
import com.example.orderservice.feign.UserClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
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
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final UserClient userClient;
    @Lazy
    private final OrderService orderService;

    @Override
    @Transactional
    public void createWarrantiesForOrder(Long orderId) {
        log.info("Creating warranties for order: {}", orderId);

        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderIdAndIsDeletedFalse(orderId);

        for (OrderDetail orderDetail : orderDetails) {
            if (warrantyRepository.findByOrderIdAndOrderDetailId(orderId, orderDetail.getId()).isEmpty()) {
                String address = null;
                try {
                    AddressResponse addressResponse = userClient.getAddressById(order.getAddressId()).getResult();
                    if (addressResponse != null) {
                        address = addressResponse.getAddressLine();
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch address for warranty creation: {}", e.getMessage());
                }

                Warranty warranty = Warranty.builder()
                        .orderId(orderId)
                        .orderDetailId(orderDetail.getId())
                        .productColorId(orderDetail.getProductColorId())
                        .customerId(order.getUserId())
                        .deliveryDate(LocalDateTime.now()) // This should be set when order is actually delivered
                        .warrantyDurationMonths(24) // 2 years
                        .description("Standard 2-year warranty")
                        .address(address)
                        .storeId(order.getStoreId())
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
    public PageResponse<WarrantyResponse> getWarrantiesByStore(String storeId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Warranty> warrantyPage = warrantyRepository.findByStoreIdAndIsDeletedFalse(storeId, pageable);

        List<WarrantyResponse> responses = warrantyPage.getContent().stream()
                .map(this::toWarrantyResponse)
                .collect(Collectors.toList());

        return PageResponse.<WarrantyResponse>builder()
                .currentPage(page)
                .pageSize(size)
                .totalPages(warrantyPage.getTotalPages())
                .totalElements(warrantyPage.getTotalElements())
                .data(responses)
                .build();
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
    public WarrantyClaimResponse updateWarrantyClaimStatus(Long claimId, String status, String adminResponse,
            String resolutionNotes) {
        // This method is kept for backward compatibility but delegates to
        // resolveWarrantyClaim if needed
        // Or can be used for simple status updates without resolution actions
        log.info("Updating warranty claim status: {} to {}", claimId, status);

        WarrantyClaim claim = warrantyClaimRepository.findByIdAndIsDeletedFalse(claimId)
                .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_CLAIM_NOT_FOUND));

        try {
            WarrantyClaimStatus newStatus = WarrantyClaimStatus.valueOf(status.toUpperCase());
            claim.setStatus(newStatus);
            claim.setAdminResponse(adminResponse);
            claim.setResolutionNotes(resolutionNotes);

            if (newStatus == WarrantyClaimStatus.RESOLVED || newStatus == WarrantyClaimStatus.REJECTED) {
                claim.setResolvedDate(LocalDateTime.now());
            }

            // Get current admin user ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof String) {
                claim.setAdminId(authentication.getName());
            }

            WarrantyClaim savedClaim = warrantyClaimRepository.save(claim);
            return toWarrantyClaimResponse(savedClaim);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
    }

    @Override
    @Transactional
    public WarrantyClaimResponse resolveWarrantyClaim(WarrantyClaimResolutionRequest request) {
        log.info("Resolving warranty claim: {} with action: {}", request.getClaimId(), request.getActionType());

        WarrantyClaim claim = warrantyClaimRepository.findByIdAndIsDeletedFalse(request.getClaimId())
                .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_CLAIM_NOT_FOUND));

        if (claim.getStatus() == WarrantyClaimStatus.RESOLVED || claim.getStatus() == WarrantyClaimStatus.REJECTED
                || claim.getStatus() == WarrantyClaimStatus.CANCELLED) {
            throw new AppException(ErrorCode.WARRANTY_CLAIM_ALREADY_RESOLVED);
        }

        claim.setActionType(request.getActionType());
        claim.setAdminResponse(request.getAdminResponse());
        claim.setResolutionNotes(request.getResolutionNotes());
        claim.setResolvedDate(LocalDateTime.now());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            claim.setAdminId(authentication.getName());
        }

        switch (request.getActionType()) {
            case RETURN:
                claim.setStatus(WarrantyClaimStatus.RESOLVED);
                claim.setRefundAmount(request.getRefundAmount());
                break;
            case REPAIR:
                claim.setStatus(WarrantyClaimStatus.RESOLVED);
                claim.setRepairCost(request.getRepairCost());
                break;
            case DO_NOTHING:
                claim.setStatus(WarrantyClaimStatus.REJECTED);
                break;
            default:
                throw new AppException(ErrorCode.INVALID_WARRANTY_ACTION);
        }

        WarrantyClaim savedClaim = warrantyClaimRepository.save(claim);
        return toWarrantyClaimResponse(savedClaim);
    }

    @Override
    @Transactional
    public OrderResponse createWarrantyOrder(Long claimId) {
        log.info("Creating order from warranty claim: {}", claimId);

        WarrantyClaim claim = warrantyClaimRepository.findByIdAndIsDeletedFalse(claimId)
                .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_CLAIM_NOT_FOUND));

        if (claim.getStatus() != WarrantyClaimStatus.RESOLVED) {
            throw new AppException(ErrorCode.WARRANTY_CANNOT_BE_CLAIMED);
        }

        if (claim.getActionType() != WarrantyActionType.RETURN) {
            throw new AppException(ErrorCode.CANNOT_CREATE_WARRANTY_ORDER);
        }

        Warranty warranty = warrantyRepository.findByIdAndIsDeletedFalse(claim.getWarrantyId())
                .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_NOT_FOUND));

        Order originalOrder = orderRepository.findByIdAndIsDeletedFalse(warranty.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        OrderDetail originalDetail = orderDetailRepository.findById(warranty.getOrderDetailId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        OrderType newOrderType = OrderType.WARRANTY_RETURN;

        // Create new Order
        Order newOrder = Order.builder()
                .userId(originalOrder.getUserId())
                .storeId(originalOrder.getStoreId())
                .addressId(originalOrder.getAddressId())
                .total(0.0) // Warranty orders usually 0 cost unless paid upgrade
                .status(EnumProcessOrder.CONFIRMED)
                .orderDate(new Date())
                .orderType(newOrderType)
                .warrantyClaimId(claimId)
                .note("Created from warranty claim #" + claimId)
                .build();

        Order savedOrder = orderRepository.save(newOrder);

        // Create OrderDetail
        OrderDetail newDetail = OrderDetail.builder()
                .order(savedOrder)
                .productColorId(originalDetail.getProductColorId())
                .quantity(1)
                .price(0.0)
                .build();

        orderDetailRepository.save(newDetail);

        // Payment will be created at confirmReturn to avoid pending records
        if (claim.getRefundAmount() == null || claim.getRefundAmount() <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        return orderService.getOrderById(savedOrder.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public WarrantyReportResponse getWarrantyReport(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null)
            startDate = LocalDateTime.now().minusMonths(1);
        if (endDate == null)
            endDate = LocalDateTime.now();

        Long totalClaims = (long) warrantyClaimRepository.findByIsDeletedFalse().size(); // Filter by date in finding if
                                                                                         // needed
        Long pendingClaims = warrantyClaimRepository.countByStatusAndIsDeletedFalse(WarrantyClaimStatus.PENDING);
        Long resolvedClaims = warrantyClaimRepository.countByStatusAndIsDeletedFalse(WarrantyClaimStatus.RESOLVED);

        Long returnCount = warrantyClaimRepository.countByActionTypeAndIsDeletedFalse(WarrantyActionType.RETURN);
        Long repairCount = warrantyClaimRepository.countByActionTypeAndIsDeletedFalse(WarrantyActionType.REPAIR);
        Long rejectedCount = warrantyClaimRepository.countByStatusAndIsDeletedFalse(WarrantyClaimStatus.REJECTED); // Or
                                                                                                                   // action
                                                                                                                   // type

        Double totalRepairCost = warrantyClaimRepository.sumRepairCost();
        Double totalRefundAmount = warrantyClaimRepository.sumRefundAmount();

        // claims
        // limited.
        // For
        // simplicity,
        // retrieving
        // first
        // 10
        List<WarrantyClaim> allClaims = warrantyClaimRepository.findByIsDeletedFalse();
        List<WarrantyClaimResponse> recentResponse = allClaims.stream()
                .sorted((c1, c2) -> c2.getClaimDate().compareTo(c1.getClaimDate()))
                .limit(10)
                .map(this::toWarrantyClaimResponse)
                .collect(Collectors.toList());

        return WarrantyReportResponse.builder()
                .totalClaims(totalClaims)
                .pendingClaims(pendingClaims)
                .resolvedClaims(resolvedClaims)
                .returnCount(returnCount)
                .repairCount(repairCount)
                .rejectedCount(rejectedCount)
                .totalRepairCost(totalRepairCost != null ? totalRepairCost : 0.0)
                .totalRefundAmount(totalRefundAmount != null ? totalRefundAmount : 0.0)
                .recentClaims(recentResponse)
                .build();
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
                .canClaimWarranty(warranty.canClaimWarranty())
                .createdAt(warranty.getCreatedAt())
                .updatedAt(warranty.getUpdatedAt())
                .address(warranty.getAddress())
                .storeId(warranty.getStoreId())
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
                .actionType(claim.getActionType())
                .repairCost(getVisibleRepairCost(claim))
                .refundAmount(claim.getRefundAmount())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }

    private Double getVisibleRepairCost(WarrantyClaim claim) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isStaffOrAdmin = false;
        if (authentication != null) {
            isStaffOrAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                            a.getAuthority().equals("ROLE_MANAGER") ||
                            a.getAuthority().equals("ROLE_STAFF"));
        }
        return isStaffOrAdmin ? claim.getRepairCost() : null;
    }
}
