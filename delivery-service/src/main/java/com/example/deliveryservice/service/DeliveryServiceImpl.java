package com.example.deliveryservice.service;

import com.example.deliveryservice.entity.DeliveryAssignment;
import com.example.deliveryservice.enums.DeliveryStatus;
import com.example.deliveryservice.enums.ErrorCode;
import com.example.deliveryservice.exception.AppException;
import com.example.deliveryservice.feign.InventoryClient;
import com.example.deliveryservice.feign.OrderClient;
import com.example.deliveryservice.feign.StoreClient;
import com.example.deliveryservice.repository.DeliveryAssignmentRepository;
import com.example.deliveryservice.request.AssignOrderRequest;
import com.example.deliveryservice.request.PrepareProductsRequest;
import com.example.deliveryservice.response.*;
import com.example.deliveryservice.service.inteface.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final OrderClient orderClient;
    private final StoreClient storeClient;
    private final InventoryClient inventoryClient;

    @Override
    @Transactional
    public DeliveryAssignmentResponse assignOrderToDelivery(AssignOrderRequest request) {
        log.info("Assigning order {} to delivery", request.getOrderId());

        // Check if order already assigned
        deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(request.getOrderId())
                .ifPresent(assignment -> {
                    String errorMessage = String.format("Order đã được assign. Assignment ID: %d, Status: %s", 
                            assignment.getId(), assignment.getStatus());
                    log.warn(errorMessage);
                    throw new AppException(ErrorCode.ASSIGNMENT_ALREADY_EXISTS);
                });

        // Verify order exists
        ResponseEntity<ApiResponse<OrderResponse>> orderResponse = orderClient.getOrderById(request.getOrderId());
        if (orderResponse.getBody() == null || orderResponse.getBody().getData() == null) {
            throw new AppException(ErrorCode.CODE_NOT_FOUND);
        }

        // Verify store exists
        ApiResponse<StoreResponse> storeResponse = storeClient.getStoreById(request.getStoreId());
        if (storeResponse == null || storeResponse.getData() == null) {
            throw new AppException(ErrorCode.CODE_NOT_FOUND);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String assignedBy = authentication.getName();

        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .orderId(request.getOrderId())
                .storeId(request.getStoreId())
                .deliveryStaffId(request.getDeliveryStaffId())
                .assignedBy(assignedBy)
                .assignedAt(LocalDateTime.now())
                .estimatedDeliveryDate(request.getEstimatedDeliveryDate())
                .status(DeliveryStatus.ASSIGNED)
                .notes(request.getNotes())
                .invoiceGenerated(false)
                .productsPrepared(false)
                .build();

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        log.info("Order {} assigned to delivery staff {}", request.getOrderId(), request.getDeliveryStaffId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreBranchInfoResponse getStoreBranchInfo(String storeId) {
        log.info("Getting store branch info for store: {}", storeId);

        // Get store information
        ApiResponse<StoreResponse> storeResponse = storeClient.getStoreById(storeId);
        if (storeResponse == null || storeResponse.getData() == null) {
            throw new AppException(ErrorCode.CODE_NOT_FOUND);
        }
        StoreResponse store = storeResponse.getData();

        // Get all inventory to show stock availability
        // Note: This is a simplified version. In production, you might want to filter by store's warehouse
        List<StoreBranchInfoResponse.ProductStockInfo> productStockInfo = new ArrayList<>();
        
        // For now, we'll return empty list. In production, you would:
        // 1. Get all products available in the store
        // 2. Check inventory for each product
        // 3. Build the response

        return StoreBranchInfoResponse.builder()
                .store(store)
                .productStockInfo(productStockInfo)
                .build();
    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse generateInvoice(Long orderId) {
        log.info("Generating invoice for order: {}", orderId);

        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.CODE_NOT_FOUND));

        if (assignment.getInvoiceGenerated()) {
            String errorMessage = String.format("Invoice đã được generate cho order này. Assignment ID: %d", 
                    assignment.getId());
            log.warn(errorMessage);
            throw new AppException(ErrorCode.INVOICE_ALREADY_GENERATED);
        }

        assignment.setInvoiceGenerated(true);
        assignment.setInvoiceGeneratedAt(LocalDateTime.now());

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        log.info("Invoice generated for order: {}", orderId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse prepareProducts(PrepareProductsRequest request) {
        log.info("Preparing products for order: {}", request.getOrderId());

        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.CODE_NOT_FOUND));

        if (assignment.getProductsPrepared()) {
            String errorMessage = String.format("Products đã được prepare cho order này. Assignment ID: %d", 
                    assignment.getId());
            log.warn(errorMessage);
            throw new AppException(ErrorCode.PRODUCTS_ALREADY_PREPARED);
        }

        // Verify order exists and get order details
        ResponseEntity<ApiResponse<OrderResponse>> orderResponse = orderClient.getOrderById(request.getOrderId());
        if (orderResponse.getBody() == null || orderResponse.getBody().getData() == null) {
            throw new AppException(ErrorCode.CODE_NOT_FOUND);
        }

        OrderResponse order = orderResponse.getBody().getData();
        
        // Check stock availability for each product in order
        List<String> insufficientProducts = new ArrayList<>();
        if (order.getOrderDetails() != null) {
            for (OrderDetailResponse detail : order.getOrderDetails()) {
                ApiResponse<Integer> stockResponse = inventoryClient.getTotalAvailableStock(detail.getProductColorId());
                if (stockResponse != null && stockResponse.getData() != null) {
                    int availableStock = stockResponse.getData();
                    if (availableStock < detail.getQuantity()) {
                        insufficientProducts.add(String.format(
                            "Product %s: Required %d, Available %d, Shortage %d",
                            detail.getProductColorId(),
                            detail.getQuantity(),
                            availableStock,
                            detail.getQuantity() - availableStock
                        ));
                    }
                }
            }
        }
        
        if (!insufficientProducts.isEmpty()) {
            String errorMessage = "Stock không đủ cho các sản phẩm sau:\n" + 
                    String.join("\n", insufficientProducts);
            log.warn(errorMessage);
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        assignment.setProductsPrepared(true);
        assignment.setProductsPreparedAt(LocalDateTime.now());
        assignment.setStatus(DeliveryStatus.READY);

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        log.info("Products prepared for order: {}", request.getOrderId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryProgressResponse getDeliveryProgressByStore(String storeId) {
        log.info("Getting delivery progress for store: {}", storeId);

        // Verify store exists
        ApiResponse<StoreResponse> storeResponse = storeClient.getStoreById(storeId);
        if (storeResponse == null || storeResponse.getData() == null) {
            throw new AppException(ErrorCode.CODE_NOT_FOUND);
        }
        StoreResponse store = storeResponse.getData();

        List<DeliveryAssignment> assignments = deliveryAssignmentRepository.findByStoreIdAndIsDeletedFalse(storeId);

        long assignedCount = assignments.stream()
                .filter(a -> a.getStatus() == DeliveryStatus.ASSIGNED)
                .count();
        long preparingCount = assignments.stream()
                .filter(a -> a.getStatus() == DeliveryStatus.PREPARING)
                .count();
        long readyCount = assignments.stream()
                .filter(a -> a.getStatus() == DeliveryStatus.READY)
                .count();
        long inTransitCount = assignments.stream()
                .filter(a -> a.getStatus() == DeliveryStatus.IN_TRANSIT)
                .count();
        long deliveredCount = assignments.stream()
                .filter(a -> a.getStatus() == DeliveryStatus.DELIVERED)
                .count();

        List<DeliveryAssignmentResponse> assignmentResponses = assignments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return DeliveryProgressResponse.builder()
                .storeId(storeId)
                .storeName(store.getName())
                .totalAssignments((long) assignments.size())
                .assignedCount(assignedCount)
                .preparingCount(preparingCount)
                .readyCount(readyCount)
                .inTransitCount(inTransitCount)
                .deliveredCount(deliveredCount)
                .assignments(assignmentResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAssignmentResponse> getDeliveryAssignmentsByStore(String storeId) {
        List<DeliveryAssignment> assignments = deliveryAssignmentRepository.findByStoreIdAndIsDeletedFalse(storeId);
        return assignments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAssignmentResponse> getDeliveryAssignmentsByStaff(String deliveryStaffId) {
        List<DeliveryAssignment> assignments = deliveryAssignmentRepository.findByDeliveryStaffIdAndIsDeletedFalse(deliveryStaffId);
        return assignments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAssignmentResponse getDeliveryAssignmentByOrderId(Long orderId) {
        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.CODE_NOT_FOUND));
        return mapToResponse(assignment);
    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse updateDeliveryStatus(Long assignmentId, String status) {
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.CODE_NOT_FOUND));

        try {
            DeliveryStatus deliveryStatus = DeliveryStatus.valueOf(status.toUpperCase());
            assignment.setStatus(deliveryStatus);
            DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
            return mapToResponse(saved);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
    }

    private DeliveryAssignmentResponse mapToResponse(DeliveryAssignment assignment) {
        OrderResponse order = null;
        StoreResponse store = null;

        try {
            ResponseEntity<ApiResponse<OrderResponse>> orderResponse = orderClient.getOrderById(assignment.getOrderId());
            if (orderResponse.getBody() != null && orderResponse.getBody().getData() != null) {
                order = orderResponse.getBody().getData();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch order {}: {}", assignment.getOrderId(), e.getMessage());
        }

        try {
            ApiResponse<StoreResponse> storeResponse = storeClient.getStoreById(assignment.getStoreId());
            if (storeResponse != null && storeResponse.getData() != null) {
                store = storeResponse.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch store {}: {}", assignment.getStoreId(), e.getMessage());
        }

        return DeliveryAssignmentResponse.builder()
                .id(assignment.getId())
                .orderId(assignment.getOrderId())
                .storeId(assignment.getStoreId())
                .deliveryStaffId(assignment.getDeliveryStaffId())
                .assignedBy(assignment.getAssignedBy())
                .assignedAt(assignment.getAssignedAt())
                .estimatedDeliveryDate(assignment.getEstimatedDeliveryDate())
                .status(assignment.getStatus())
                .notes(assignment.getNotes())
                .invoiceGenerated(assignment.getInvoiceGenerated())
                .invoiceGeneratedAt(assignment.getInvoiceGeneratedAt())
                .productsPrepared(assignment.getProductsPrepared())
                .productsPreparedAt(assignment.getProductsPreparedAt())
                .order(order)
                .store(store)
                .build();
    }
}

