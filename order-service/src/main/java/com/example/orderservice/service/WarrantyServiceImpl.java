package com.example.orderservice.service;

import com.example.orderservice.entity.*;
import com.example.orderservice.enums.*;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.repository.*;
import com.example.orderservice.request.WarrantyClaimRequest;
import com.example.orderservice.request.WarrantyClaimResolutionRequest;
import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.WarrantyClaimResponse;
import com.example.orderservice.response.WarrantyReportResponse;
import com.example.orderservice.response.WarrantyResponse;
import com.example.orderservice.response.AddressResponse;
import com.example.orderservice.response.PageResponse;
import com.example.orderservice.service.inteface.WarrantyService;
import com.example.orderservice.feign.UserClient;
import feign.FeignException;
import com.example.orderservice.event.OrderReturnedEvent;
import com.example.orderservice.event.WarrantyClaimApprovedEvent;
import com.example.orderservice.event.WarrantyClaimRejectedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
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
public class WarrantyServiceImpl implements WarrantyService {

    private final WarrantyRepository warrantyRepository;
    private final WarrantyClaimRepository warrantyClaimRepository;
    private final WarrantyClaimDetailRepository warrantyClaimDetailRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ObjectMapper objectMapper;
    private final UserClient userClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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
                    AddressResponse addressResponse = userClient.getAddressById(order.getAddressId()).getData();
                    if (addressResponse != null) {
                        address = addressResponse.getAddressLine();
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch address for warranty creation: {}", e.getMessage());
                }

                Warranty warranty = new Warranty();
                warranty.setOrder(order);
                warranty.setOrderDetailId(orderDetail.getId());
                warranty.setProductColorId(orderDetail.getProductColorId());
                warranty.setCustomerId(order.getUserId());
                warranty.setDeliveryDate(LocalDateTime.now()); // This should be set when order is actually delivered
                warranty.setWarrantyDurationMonths(24); // 2 years
                warranty.setDescription("Standard 2-year warranty");
                warranty.setAddress(address);
                warranty.setStoreId(order.getStoreId());
                warranty.setClaimCount(0);
                warranty.setMaxClaims(3);

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
        List<Warranty> warranties = warrantyRepository.findByOrder_IdAndIsDeletedFalse(orderId);
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
                .number(page)
                .size(size)
                .totalPages(warrantyPage.getTotalPages())
                .totalElements(warrantyPage.getTotalElements())
                .content(responses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WarrantyClaimResponse> getWarrantyClaimsByStore(String storeId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<WarrantyClaim> claimPage = warrantyClaimRepository.findByStoreIdOrderByClaimDateDesc(storeId, pageable);

        List<WarrantyClaimResponse> responses = claimPage.getContent().stream()
                .map(this::toWarrantyClaimResponseWithAddress)
                .collect(Collectors.toList());

        return PageResponse.<WarrantyClaimResponse>builder()
                .number(page)
                .size(size)
                .totalPages(claimPage.getTotalPages())
                .totalElements(claimPage.getTotalElements())
                .content(responses)
                .build();
    }

    @Override
    @Transactional
    public WarrantyClaimResponse createWarrantyClaim(WarrantyClaimRequest request) {
        log.info("Creating warranty claim for order: {}", request.getOrderId());

        Order originalOrder = orderRepository.findByIdAndIsDeletedFalse(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Validation: Đảm bảo đơn hàng có storeId (đã được bán bởi một cửa hàng)
        if (originalOrder.getStoreId() == null || originalOrder.getStoreId().isEmpty()) {
            log.error("Order {} does not have a storeId assigned", request.getOrderId());
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Long addressId = request.getAddressId();
        if (addressId == null) {
            addressId = originalOrder.getAddressId();
            log.info("AddressId not provided, using address from original order: {}", addressId);
        }

        ApiResponse<AddressResponse> addressResponse = userClient.getAddressById(addressId);
        if (addressResponse == null || addressResponse.getData() == null) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        String orderStoreId = originalOrder.getStoreId();

        // Validation: Kiểm tra tất cả warranties trước khi tạo claim (chống spam và validate sớm)
        for (com.example.orderservice.request.WarrantyClaimItemRequest item : request.getItems()) {
            Warranty warranty = warrantyRepository.findByIdAndIsDeletedFalse(item.getWarrantyId())
                    .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_NOT_FOUND));

            if (!warranty.canClaimWarranty()) {
                throw new AppException(ErrorCode.WARRANTY_CANNOT_BE_CLAIMED);
            }

            // Validation: Đảm bảo warranty thuộc về cùng store với order
            if (warranty.getStoreId() == null || !warranty.getStoreId().equals(orderStoreId)) {
                log.error("Warranty {} belongs to store {}, but order {} belongs to store {}", 
                    warranty.getId(), warranty.getStoreId(), request.getOrderId(), orderStoreId);
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            // Validation: Kiểm tra xem đã có claim PENDING hoặc APPROVED cho orderDetailId này chưa (chống spam)
            // Cho phép tạo claim mới nếu claim trước đó bị REJECTED hoặc đã RESOLVED
            List<WarrantyClaimStatus> activeStatuses = List.of(
                WarrantyClaimStatus.PENDING, 
                WarrantyClaimStatus.APPROVED,
                WarrantyClaimStatus.UNDER_REVIEW
            );
            List<WarrantyClaim> existingActiveClaims = warrantyClaimRepository
                    .findActiveClaimsByOrderDetailId(warranty.getOrderDetailId(), activeStatuses);
            if (!existingActiveClaims.isEmpty()) {
                log.warn("Customer {} attempted to create duplicate warranty claim for orderDetailId {}. Existing active claim (status: {}) found: {}", 
                    originalOrder.getUserId(), warranty.getOrderDetailId(), 
                    existingActiveClaims.get(0).getStatus(), existingActiveClaims.get(0).getId());
                throw new AppException(ErrorCode.WARRANTY_CLAIM_PENDING_EXISTS);
            }
        }

        // Sau khi validate xong, mới tạo claim
        WarrantyClaim claim = WarrantyClaim.builder()
                .orderId(request.getOrderId())
                .customerId(originalOrder.getUserId())
                .addressId(addressId)
                .status(WarrantyClaimStatus.PENDING)
                .build();

        WarrantyClaim savedClaim = warrantyClaimRepository.save(claim);

        // Gán claimId vào đơn hàng để OrderResponse.warrantyClaimId không còn null
        originalOrder.setWarrantyClaimId(savedClaim.getId());
        orderRepository.save(originalOrder);

        // Tạo claim details sau khi đã validate và tạo claim
        for (com.example.orderservice.request.WarrantyClaimItemRequest item : request.getItems()) {
            Warranty warranty = warrantyRepository.findByIdAndIsDeletedFalse(item.getWarrantyId())
                    .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_NOT_FOUND));

            String customerPhotosJson = null;
            if (item.getCustomerPhotos() != null && !item.getCustomerPhotos().isEmpty()) {
                try {
                    customerPhotosJson = objectMapper.writeValueAsString(item.getCustomerPhotos());
                } catch (JsonProcessingException e) {
                    log.error("Error serializing customer photos", e);
                }
            }

            WarrantyClaimDetail detail = WarrantyClaimDetail.builder()
                    .warrantyClaim(savedClaim)
                    .warranty(warranty)
                    .quantity(item.getQuantity())
                    .issueDescription(item.getIssueDescription())
                    .customerPhotos(customerPhotosJson)
                    .build();

            warrantyClaimDetailRepository.save(detail);

            warranty.incrementClaimCount();
            warrantyRepository.save(warranty);
        }

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

        // Validation: Kiểm tra trạng thái hiện tại để tránh race condition (Câu 8)
        // Nếu claim đã được xử lý (không còn PENDING/UNDER_REVIEW), không cho phép update
        WarrantyClaimStatus currentStatus = claim.getStatus();
        if (currentStatus == WarrantyClaimStatus.RESOLVED || 
            currentStatus == WarrantyClaimStatus.REJECTED || 
            currentStatus == WarrantyClaimStatus.CANCELLED) {
            log.warn("Attempted to update warranty claim {} that is already in final status: {}", claimId, currentStatus);
            throw new AppException(ErrorCode.WARRANTY_CLAIM_ALREADY_RESOLVED);
        }

        try {
            WarrantyClaimStatus newStatus = WarrantyClaimStatus.valueOf(status.toUpperCase());
            WarrantyClaimStatus oldStatus = claim.getStatus();
            claim.setStatus(newStatus);
            claim.setAdminResponse(adminResponse);
            claim.setResolutionNotes(resolutionNotes);

            if (newStatus == WarrantyClaimStatus.RESOLVED || newStatus == WarrantyClaimStatus.REJECTED) {
                claim.setResolvedDate(LocalDateTime.now());
            }

            // Get current admin user ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String adminId = null;
            if (authentication != null && authentication.getPrincipal() instanceof String) {
                adminId = authentication.getName();
                claim.setAdminId(adminId);
            }

            WarrantyClaim savedClaim = warrantyClaimRepository.save(claim);

            // Publish event khi status chuyển sang APPROVED (Câu 3)
            if (newStatus == WarrantyClaimStatus.APPROVED && oldStatus != WarrantyClaimStatus.APPROVED) {
                try {
                    WarrantyClaimApprovedEvent event = WarrantyClaimApprovedEvent.builder()
                            .claimId(savedClaim.getId())
                            .orderId(savedClaim.getOrderId())
                            .customerId(savedClaim.getCustomerId())
                            .addressId(savedClaim.getAddressId())
                            .actionType(savedClaim.getActionType())
                            .refundAmount(savedClaim.getRefundAmount())
                            .repairCost(savedClaim.getRepairCost())
                            .exchangeProductColorId(savedClaim.getExchangeProductColorId())
                            .approvedAt(LocalDateTime.now())
                            .approvedBy(adminId)
                            .build();

                    kafkaTemplate.send("warranty-claim-approved-topic", event)
                            .whenComplete((result, ex) -> {
                                if (ex != null) {
                                    log.error("Failed to send WarrantyClaimApprovedEvent for claim {}: {}", claimId, ex.getMessage());
                                } else {
                                    log.info("Successfully sent WarrantyClaimApprovedEvent for claim: {}", claimId);
                                }
                            });
                } catch (Exception e) {
                    log.error("Error publishing WarrantyClaimApprovedEvent for claim {}: {}", claimId, e.getMessage(), e);
                    // Không throw exception để không rollback transaction
                }
            }

            // Publish event khi status chuyển sang REJECTED
            if (newStatus == WarrantyClaimStatus.REJECTED && oldStatus != WarrantyClaimStatus.REJECTED) {
                try {
                    String rejectionReason = adminResponse != null && !adminResponse.isEmpty() 
                            ? adminResponse 
                            : (resolutionNotes != null && !resolutionNotes.isEmpty() ? resolutionNotes : "Không có lý do cụ thể");
                    
                    WarrantyClaimRejectedEvent event = WarrantyClaimRejectedEvent.builder()
                            .claimId(savedClaim.getId())
                            .orderId(savedClaim.getOrderId())
                            .customerId(savedClaim.getCustomerId())
                            .rejectionReason(rejectionReason)
                            .rejectedAt(LocalDateTime.now())
                            .rejectedBy(adminId)
                            .build();

                    kafkaTemplate.send("warranty-claim-rejected-topic", event)
                            .whenComplete((result, ex) -> {
                                if (ex != null) {
                                    log.error("Failed to send WarrantyClaimRejectedEvent for claim {}: {}", claimId, ex.getMessage());
                                } else {
                                    log.info("Successfully sent WarrantyClaimRejectedEvent for claim: {}", claimId);
                                }
                            });
                } catch (Exception e) {
                    log.error("Error publishing WarrantyClaimRejectedEvent for claim {}: {}", claimId, e.getMessage(), e);
                    // Không throw exception để không rollback transaction
                }
            }

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

        WarrantyClaimStatus oldStatus = claim.getStatus();
        String adminId = null;
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            adminId = authentication.getName();
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

        // Publish event khi status chuyển sang APPROVED (nếu resolveWarrantyClaim được dùng để approve)
        // Note: resolveWarrantyClaim thường set status là RESOLVED hoặc REJECTED, không phải APPROVED
        // Nhưng để đảm bảo, nếu có trường hợp approve qua method này, vẫn publish event
        if (savedClaim.getStatus() == WarrantyClaimStatus.APPROVED && oldStatus != WarrantyClaimStatus.APPROVED) {
            try {
                WarrantyClaimApprovedEvent event = WarrantyClaimApprovedEvent.builder()
                        .claimId(savedClaim.getId())
                        .orderId(savedClaim.getOrderId())
                        .customerId(savedClaim.getCustomerId())
                        .addressId(savedClaim.getAddressId())
                        .actionType(savedClaim.getActionType())
                        .refundAmount(savedClaim.getRefundAmount())
                        .repairCost(savedClaim.getRepairCost())
                        .exchangeProductColorId(savedClaim.getExchangeProductColorId())
                        .approvedAt(LocalDateTime.now())
                        .approvedBy(adminId)
                        .build();

                kafkaTemplate.send("warranty-claim-approved-topic", event)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to send WarrantyClaimApprovedEvent for claim {}: {}", request.getClaimId(), ex.getMessage());
                            } else {
                                log.info("Successfully sent WarrantyClaimApprovedEvent for claim: {}", request.getClaimId());
                            }
                        });
            } catch (Exception e) {
                log.error("Error publishing WarrantyClaimApprovedEvent for claim {}: {}", request.getClaimId(), e.getMessage(), e);
                // Không throw exception để không rollback transaction
            }
        }

        // Publish event khi status chuyển sang REJECTED (khi actionType = DO_NOTHING)
        if (savedClaim.getStatus() == WarrantyClaimStatus.REJECTED && oldStatus != WarrantyClaimStatus.REJECTED) {
            try {
                String rejectionReason = savedClaim.getAdminResponse() != null && !savedClaim.getAdminResponse().isEmpty()
                        ? savedClaim.getAdminResponse()
                        : (savedClaim.getResolutionNotes() != null && !savedClaim.getResolutionNotes().isEmpty()
                                ? savedClaim.getResolutionNotes()
                                : "Không có lý do cụ thể");

                WarrantyClaimRejectedEvent event = WarrantyClaimRejectedEvent.builder()
                        .claimId(savedClaim.getId())
                        .orderId(savedClaim.getOrderId())
                        .customerId(savedClaim.getCustomerId())
                        .rejectionReason(rejectionReason)
                        .rejectedAt(LocalDateTime.now())
                        .rejectedBy(adminId)
                        .build();

                kafkaTemplate.send("warranty-claim-rejected-topic", event)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to send WarrantyClaimRejectedEvent for claim {}: {}", request.getClaimId(), ex.getMessage());
                            } else {
                                log.info("Successfully sent WarrantyClaimRejectedEvent for claim: {}", request.getClaimId());
                            }
                        });
            } catch (Exception e) {
                log.error("Error publishing WarrantyClaimRejectedEvent for claim {}: {}", request.getClaimId(), e.getMessage(), e);
                // Không throw exception để không rollback transaction
            }
        }

        return toWarrantyClaimResponse(savedClaim);
    }

//    @Override
//    @Transactional
//    public OrderResponse createWarrantyOrder(Long claimId) {
//        log.info("Creating order from warranty claim: {}", claimId);
//
//        WarrantyClaim claim = warrantyClaimRepository.findByIdAndIsDeletedFalse(claimId)
//                .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_CLAIM_NOT_FOUND));
//
//        if (claim.getStatus() != WarrantyClaimStatus.RESOLVED) {
//            throw new AppException(ErrorCode.WARRANTY_CANNOT_BE_CLAIMED);
//        }
//
//        if (claim.getActionType() != WarrantyActionType.RETURN) {
//            throw new AppException(ErrorCode.CANNOT_CREATE_WARRANTY_ORDER);
//        }
//
//        Warranty warranty = warrantyRepository.findByIdAndIsDeletedFalse(claim.getWarrantyId())
//                .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_NOT_FOUND));
//
//        Order originalOrder = orderRepository.findByIdAndIsDeletedFalse(warranty.getOrderId())
//                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
//
//        OrderDetail originalDetail = orderDetailRepository.findById(warranty.getOrderDetailId())
//                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
//
//        OrderType newOrderType = OrderType.WARRANTY_RETURN;
//
//        // Create new Order
//        // Use addressId from claim (customer's current address) instead of original order address
//        // Store remains the same as original order (store that handled the original order)
//        Order newOrder = Order.builder()
//                .userId(originalOrder.getUserId())
//                .storeId(originalOrder.getStoreId()) // Store that handled original order will handle warranty
//                .addressId(claim.getAddressId()) // Use address from claim (customer's current address)
//                .total(0.0) // Warranty orders usually 0 cost unless paid upgrade
//                .status(EnumProcessOrder.CONFIRMED)
//                .orderDate(new Date())
//                .orderType(newOrderType)
//                .warrantyClaimId(claimId)
//                .note("Created from warranty claim #" + claimId)
//                .build();
//
//        Order savedOrder = orderRepository.save(newOrder);
//
//        // Create OrderDetail
//        OrderDetail newDetail = OrderDetail.builder()
//                .order(savedOrder)
//                .productColorId(originalDetail.getProductColorId())
//                .quantity(1)
//                .price(0.0)
//                .build();
//
//        orderDetailRepository.save(newDetail);
//
//        // Payment will be created at confirmReturn to avoid pending records
//        if (claim.getRefundAmount() == null || claim.getRefundAmount() <= 0) {
//            throw new AppException(ErrorCode.INVALID_REQUEST);
//        }
//
//        // Use ApplicationContext to get OrderService lazily to avoid circular dependency
//        OrderService orderService = applicationContext.getBean(OrderService.class);
//        return orderService.getOrderById(savedOrder.getId());
//    }

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
                .orderId(warranty.getOrder() != null ? warranty.getOrder().getId() : null)
                .orderDetailId(warranty.getOrderDetailId())
                .productColorId(warranty.getProductColorId())
                .customerId(warranty.getCustomerId())
                .deliveryDate(warranty.getDeliveryDate())
                .warrantyStartDate(warranty.getWarrantyStartDate())
                .warrantyEndDate(warranty.getWarrantyEndDate())
                .status(warranty.getStatus().name())
                .warrantyDurationMonths(warranty.getWarrantyDurationMonths())
                .description(warranty.getDescription())
                .claimCount(warranty.getClaimCount())
                .maxClaims(warranty.getMaxClaims())
                .isActive(warranty.isActive())
                .canClaimWarranty(warranty.canClaimWarranty())
                .createdAt(warranty.getCreatedAt())
                .updatedAt(warranty.getUpdatedAt())
                .address(warranty.getAddress())
                .storeId(warranty.getStoreId())
                .build();
    }

    private WarrantyClaimResponse toWarrantyClaimResponse(WarrantyClaim claim) {
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

        List<com.example.orderservice.response.WarrantyClaimDetailResponse> itemResponses = null;
        if (claim.getClaimDetails() != null) {
            itemResponses = claim.getClaimDetails().stream().map(detail -> {
                List<String> customerPhotos = null;
                if (detail.getCustomerPhotos() != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<String> photos = (List<String>) objectMapper.readValue(detail.getCustomerPhotos(), List.class);
                        customerPhotos = photos;
                    } catch (JsonProcessingException e) {
                        log.error("Error deserializing customer photos", e);
                    }
                }
                return com.example.orderservice.response.WarrantyClaimDetailResponse.builder()
                        .id(detail.getId())
                        .warrantyId(detail.getWarranty().getId())
                        .quantity(detail.getQuantity())
                        .issueDescription(detail.getIssueDescription())
                        .customerPhotos(customerPhotos)
                        .productColorId(detail.getWarranty().getProductColorId())
                        .build();
            }).collect(Collectors.toList());
        }

        return WarrantyClaimResponse.builder()
                .id(claim.getId())
                .orderId(claim.getOrderId())
                .customerId(claim.getCustomerId())
                .addressId(claim.getAddressId())
                .claimDate(claim.getClaimDate())
                .items(itemResponses)
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
        boolean isStaffOrManager = false;
        if (authentication != null) {
            isStaffOrManager = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_BRANCH_MANAGER") ||
                            a.getAuthority().equals("ROLE_STAFF"));
        }
        return isStaffOrManager ? claim.getRepairCost() : null;
    }

    private WarrantyClaimResponse toWarrantyClaimResponseWithAddress(WarrantyClaim claim) {
        WarrantyClaimResponse response = toWarrantyClaimResponse(claim);
        
        // Fetch address information
        if (claim.getAddressId() != null) {
            try {
                ApiResponse<AddressResponse> addressResponse = userClient.getAddressById(claim.getAddressId());
                if (addressResponse != null && addressResponse.getData() != null) {
                    AddressResponse addressData = addressResponse.getData();
                    response.setAddress(addressData.getAddressLine() != null ? addressData.getAddressLine() : addressData.getFullAddress());
                    response.setName(addressData.getName());
                    response.setPhone(addressData.getPhone());
                }
            } catch (feign.FeignException e) {
                log.warn("Feign error when fetching address {} for warranty claim {}: {} (status: {})", 
                    claim.getAddressId(), claim.getId(), e.getMessage(), e.status());
                // Continue without address information rather than failing the entire request
            } catch (Exception e) {
                log.error("Unexpected error when fetching address {} for warranty claim {}: {}", 
                    claim.getAddressId(), claim.getId(), e.getMessage(), e);
                // Continue without address information rather than failing the entire request
            }
        }
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateRefundAmount(OrderReturnedEvent event) {
        log.info("Calculating refund amount for order: {}, warranty claim: {}", 
                event.getOrderId(), event.getWarrantyClaimId());

        // Lấy thông tin order
        Order order = orderRepository.findByIdAndIsDeletedFalse(event.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Tính tổng giá trị các items được return
        double returnedItemsTotal = 0.0;
        int totalOrderItems = 0;
        
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            log.warn("Order {} has no order details. Cannot calculate refund.", event.getOrderId());
            return 0.0;
        }

        // Đếm tổng số items trong order
        totalOrderItems = order.getOrderDetails().stream()
                .mapToInt(OrderDetail::getQuantity)
                .sum();

        // Tính tổng giá trị các items được return
        for (OrderReturnedEvent.ReturnedItem returnedItem : event.getItems()) {
            // Tìm OrderDetail tương ứng
            OrderDetail orderDetail = order.getOrderDetails().stream()
                    .filter(detail -> detail.getProductColorId().equals(returnedItem.getProductColorId()))
                    .findFirst()
                    .orElse(null);

            if (orderDetail != null) {
                // Tính giá trị: price * quantity (của returned item)
                double itemValue = orderDetail.getPrice() * returnedItem.getQuantity();
                returnedItemsTotal += itemValue;
                log.debug("Returned item: {} x {} = {}", 
                        returnedItem.getProductColorId(), returnedItem.getQuantity(), itemValue);
            } else {
                log.warn("OrderDetail not found for productColorId: {} in order: {}", 
                        returnedItem.getProductColorId(), event.getOrderId());
            }
        }

        if (returnedItemsTotal <= 0) {
            log.warn("Returned items total is 0 or negative for order: {}", event.getOrderId());
            return 0.0;
        }

        // Tính voucher discount (nếu có)
        double voucherDiscount = 0.0;
        if (order.getVouchers() != null && !order.getVouchers().isEmpty()) {
            // Tính tổng voucher discount
            double totalVoucherDiscount = order.getVouchers().stream()
                    .mapToDouble(v -> v.getAmount() != null ? v.getAmount().doubleValue() : 0.0)
                    .sum();

            // Tính số lượng items được return
            int returnedItemsQuantity = event.getItems().stream()
                    .mapToInt(OrderReturnedEvent.ReturnedItem::getQuantity)
                    .sum();

            // Tính voucher discount tương ứng với phần items được return
            // Công thức: (Voucher Discount / Total Items) * Returned Items
            if (totalOrderItems > 0) {
                voucherDiscount = (totalVoucherDiscount / totalOrderItems) * returnedItemsQuantity;
                log.info("Total voucher discount: {}, Total order items: {}, Returned items: {}, Proportional discount: {}", 
                        totalVoucherDiscount, totalOrderItems, returnedItemsQuantity, voucherDiscount);
            }
        }

        // Refund Amount = Returned Items Total - Proportional Voucher Discount
        double refundAmount = returnedItemsTotal - voucherDiscount;
        
        // Đảm bảo refund amount không âm
        refundAmount = Math.max(0.0, refundAmount);

        log.info("Refund calculation for order {}: Returned items total: {}, Voucher discount: {}, Final refund: {}", 
                event.getOrderId(), returnedItemsTotal, voucherDiscount, refundAmount);

        return refundAmount;
    }
}
