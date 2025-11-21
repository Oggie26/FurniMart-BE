package com.example.deliveryservice.service;

import com.example.deliveryservice.entity.DeliveryAssignment;
import com.example.deliveryservice.enums.DeliveryStatus;
import com.example.deliveryservice.enums.EnumProcessOrder;
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
        log.info("Assigning order {} to delivery staff {}", request.getOrderId(), request.getDeliveryStaffId());

        // Find existing delivery assignment (must be created during prepare-products step)
        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(request.getOrderId())
                .orElseThrow(() -> {
                    log.warn("Cannot assign order {}: Delivery assignment not found. Please prepare products first.", request.getOrderId());
                    throw new AppException(ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND);
                });

        // Check if already assigned to a delivery staff
        if (assignment.getDeliveryStaffId() != null && !assignment.getDeliveryStaffId().isEmpty()) {
            String errorMessage = String.format("Order đã được assign cho delivery staff: %s. Assignment ID: %d, Status: %s", 
                    assignment.getDeliveryStaffId(), assignment.getId(), assignment.getStatus());
            log.warn(errorMessage);
            throw new AppException(ErrorCode.ASSIGNMENT_ALREADY_EXISTS);
        }

        // Validate workflow prerequisites:
        // 1. Products must be prepared
        if (!assignment.getProductsPrepared()) {
            log.warn("Cannot assign order {}: Products not prepared yet. Please prepare products first.", request.getOrderId());
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // 2. Status must be READY (Manager must confirm ready)
        if (assignment.getStatus() != DeliveryStatus.READY) {
            log.warn("Cannot assign order {}: Status is {}, but must be READY. Please confirm ready first.", 
                    request.getOrderId(), assignment.getStatus());
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // 3. Invoice must be generated (Manager must approve export)
        if (!assignment.getInvoiceGenerated()) {
            log.warn("Cannot assign order {}: Invoice not generated yet. Please approve export (generate invoice) first.", request.getOrderId());
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Verify order exists
        ResponseEntity<ApiResponse<OrderResponse>> orderResponse = orderClient.getOrderById(request.getOrderId());
        if (orderResponse.getBody() == null || orderResponse.getBody().getData() == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }

        // Verify store exists
        ApiResponse<StoreResponse> storeResponse = storeClient.getStoreById(request.getStoreId());
        if (storeResponse == null || storeResponse.getData() == null) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        // Verify store matches assignment
        if (!assignment.getStoreId().equals(request.getStoreId())) {
            log.warn("Store ID mismatch: Assignment store {}, Request store {}", assignment.getStoreId(), request.getStoreId());
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String assignedBy = authentication.getName();

        // Update assignment with delivery staff info
        assignment.setDeliveryStaffId(request.getDeliveryStaffId());
        assignment.setAssignedBy(assignedBy);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        assignment.setStatus(DeliveryStatus.ASSIGNED);
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            assignment.setNotes(request.getNotes());
        }

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        log.info("Order {} assigned to delivery staff {} after products prepared, confirmed ready, and export approved", 
                request.getOrderId(), request.getDeliveryStaffId());

        // Update order status to SHIPPING when assigned to delivery
        try {
            orderClient.updateOrderStatus(request.getOrderId(), EnumProcessOrder.SHIPPING);
            log.info("Order {} status updated to SHIPPING", request.getOrderId());
        } catch (Exception e) {
            log.error("Failed to update order status for order {}: {}", request.getOrderId(), e.getMessage());
            // Don't fail the assignment if status update fails, but log the error
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreBranchInfoResponse getStoreBranchInfo(String storeId) {
        log.info("Getting store branch info for store: {}", storeId);

        // Get store information
        ApiResponse<StoreResponse> storeResponse = storeClient.getStoreById(storeId);
        if (storeResponse == null || storeResponse.getData() == null) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
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

        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND));

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

        // Verify order exists and get order details
        ResponseEntity<ApiResponse<OrderResponse>> orderResponse = orderClient.getOrderById(request.getOrderId());
        if (orderResponse.getBody() == null || orderResponse.getBody().getData() == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }

        OrderResponse order = orderResponse.getBody().getData();
        
        // Get or create delivery assignment
        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(request.getOrderId())
                .orElseGet(() -> {
                    // Create new assignment if not exists
                    log.info("Creating new delivery assignment for order: {}", request.getOrderId());
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    String preparedBy = authentication != null ? authentication.getName() : "SYSTEM";
                    
                    return DeliveryAssignment.builder()
                            .orderId(request.getOrderId())
                            .storeId(order.getStoreId())
                            .deliveryStaffId(null) // Will be assigned later
                            .assignedBy(preparedBy)
                            .assignedAt(LocalDateTime.now())
                            .status(DeliveryStatus.PREPARING)
                            .productsPrepared(false)
                            .invoiceGenerated(false)
                            .build();
                });

        // Check if already prepared
        if (assignment.getProductsPrepared()) {
            String errorMessage = String.format("Products đã được prepare cho order này. Assignment ID: %d", 
                    assignment.getId());
            log.warn(errorMessage);
            throw new AppException(ErrorCode.PRODUCTS_ALREADY_PREPARED);
        }

        // Ensure storeId matches order
        if (assignment.getStoreId() == null || !assignment.getStoreId().equals(order.getStoreId())) {
            assignment.setStoreId(order.getStoreId());
        }
        
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

        // Update assignment
        assignment.setProductsPrepared(true);
        assignment.setProductsPreparedAt(LocalDateTime.now());
        assignment.setStatus(DeliveryStatus.PREPARING);

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        log.info("Products prepared for order: {}. Status set to PREPARING. Manager needs to confirm readiness.", request.getOrderId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryProgressResponse getDeliveryProgressByStore(String storeId) {
        log.info("Getting delivery progress for store: {}", storeId);

        // Verify store exists
        ApiResponse<StoreResponse> storeResponse = storeClient.getStoreById(storeId);
        if (storeResponse == null || storeResponse.getData() == null) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
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
                .orElseThrow(() -> new AppException(ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND));
        return mapToResponse(assignment);
    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse updateDeliveryStatus(Long assignmentId, String status) {
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND));

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
                order = sanitizeOrderResponse(orderResponse.getBody().getData());
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


    private OrderResponse sanitizeOrderResponse(OrderResponse order) {
        if (order == null) {
            return null;
        }

        // Sanitize user information - only keep essential fields for delivery
        UserResponse sanitizedUser = null;
        if (order.getUser() != null) {
            sanitizedUser = UserResponse.builder()
                    .id(order.getUser().getId())
                    .fullName(order.getUser().getFullName())
                    .email(order.getUser().getEmail())
                    .phone(order.getUser().getPhone())
                    .gender(order.getUser().getGender())
                    .birthday(order.getUser().getBirthday())
                    .avatar(order.getUser().getAvatar())
                    // Removed: role, status, createdAt, updatedAt, cccd, point
                    .build();
        }

        // Ensure depositPrice is not null (set to 0 if null)
        Double depositPrice = order.getDepositPrice();
        if (depositPrice == null) {
            depositPrice = 0.0;
        }

        // Build sanitized order response
        return OrderResponse.builder()
                .id(order.getId())
                .user(sanitizedUser)
                .storeId(order.getStoreId())
                .address(order.getAddress())
                .total(order.getTotal())
                .note(order.getNote())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .reason(order.getReason())
                .orderDetails(order.getOrderDetails())
                .processOrders(order.getProcessOrders())
                .payment(order.getPayment())
                .qrCode(order.getQrCode())
                .depositPrice(depositPrice)
                .qrCodeGeneratedAt(order.getQrCodeGeneratedAt())
                .build();
    }


}

