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
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;

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

        OrderResponse order = callWithRetry(() -> {
            ResponseEntity<ApiResponse<OrderResponse>> orderResponse = orderClient.getOrderById(request.getOrderId());
            if (orderResponse.getBody() == null || orderResponse.getBody().getData() == null) {
                throw new AppException(ErrorCode.ORDER_NOT_FOUND);
            }
            return orderResponse.getBody().getData();
        }, "getOrderById", 3);

        callWithRetry(() -> {
            ApiResponse<StoreResponse> storeResponse = storeClient.getStoreById(request.getStoreId());
            if (storeResponse == null || storeResponse.getData() == null) {
                throw new AppException(ErrorCode.STORE_NOT_FOUND);
            }
            return storeResponse.getData();
        }, "getStoreById", 3);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String assignedBy = authentication != null ? authentication.getName() : "SYSTEM";
        DeliveryAssignment assignment = getOrCreateAssignment(
                request.getOrderId(), 
                order.getStoreId(),
                assignedBy
        );

//        validatePrerequisites(assignment, request.getOrderId());

        DeliveryAssignment saved = assignDeliveryStaffAtomically(
                assignment,
                request.getDeliveryStaffId(),
                assignedBy,
                request.getEstimatedDeliveryDate(),
                request.getNotes()
        );

        orderClient.updateOrderStatus(request.getOrderId(), EnumProcessOrder.READY_FOR_INVOICE);


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

//    @Override
//    @Transactional
//    public DeliveryAssignmentResponse generateInvoice(Long orderId) {
//
//        // Verify order exists and check status
//        ResponseEntity<ApiResponse<OrderResponse>> orderResponse = orderClient.getOrderById(orderId);
//        if (orderResponse.getBody() == null || orderResponse.getBody().getData() == null) {
//            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
//        }
//
//        OrderResponse order = orderResponse.getBody().getData();
//
//        // Check if order is at MANAGER_ACCEPT
//        if (order.getStatus() != EnumProcessOrder.MANAGER_ACCEPT) {
//            log.warn("Cannot generate invoice for order {}: Order must be at MANAGER_ACCEPT. Current status: {}",
//                    orderId, order.getStatus());
//            throw new AppException(ErrorCode.INVALID_STATUS);
//        }
//
//        // Check if order already has READY_FOR_INVOICE in status history
//        // If yes, PDF already exists, cannot create again
//        boolean hasReadyForInvoice = false;
//        if (order.getProcessOrders() != null && !order.getProcessOrders().isEmpty()) {
//            hasReadyForInvoice = order.getProcessOrders().stream()
//                    .anyMatch(po -> po.getStatus() == EnumProcessOrder.READY_FOR_INVOICE);
//        }
//
//        if (hasReadyForInvoice) {
//            log.warn("Cannot generate invoice for order {}: Order already has READY_FOR_INVOICE in status history. PDF already exists.", orderId);
//            throw new AppException(ErrorCode.INVOICE_ALREADY_GENERATED);
//        }
//
//        // Get or create delivery assignment atomically
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String assignedBy = authentication != null ? authentication.getName() : "SYSTEM";
//        DeliveryAssignment assignment = getOrCreateAssignment(orderId, order.getStoreId(), assignedBy);
//
//        // Generate PDF by calling order-service (with retry)
//        String pdfPath;
//        try {
//            pdfPath = callWithRetry(() -> {
//                ResponseEntity<ApiResponse<String>> pdfResponse = orderClient.generatePDF(orderId);
//                if (pdfResponse.getBody() != null && pdfResponse.getBody().getData() != null) {
//                    return pdfResponse.getBody().getData();
//                }
//                throw new AppException(ErrorCode.INVALID_REQUEST);
//            }, "generatePDF", 3);
//            log.info("PDF generated successfully for order {}: {}", orderId, pdfPath);
//        } catch (Exception e) {
//            log.error("Failed to generate PDF for order {} after retries: {}", orderId, e.getMessage(), e);
//            throw new AppException(ErrorCode.INVALID_REQUEST);
//        }
//
//        // Update order status to READY_FOR_INVOICE after successful PDF generation
//        try {
//            orderClient.updateOrderStatus(orderId, EnumProcessOrder.READY_FOR_INVOICE);
//            log.info("Order {} status updated to READY_FOR_INVOICE after PDF generation", orderId);
//        } catch (Exception e) {
//            log.error("Failed to update order status for order {}: {}", orderId, e.getMessage());
//            // Don't fail the invoice generation if status update fails, but log the error
//        }
//
//        // Mark invoice as generated
//        assignment.setInvoiceGenerated(true);
//        assignment.setInvoiceGeneratedAt(LocalDateTime.now());
//
//        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
//        log.info("Invoice generated for order: {}", orderId);
//
//        return mapToResponse(saved);
//    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse prepareProducts(PrepareProductsRequest request) {
        log.info("Preparing products for order: {}", request.getOrderId());

        ResponseEntity<ApiResponse<OrderResponse>> orderResponse = orderClient.getOrderById(request.getOrderId());
        if (orderResponse.getBody() == null || orderResponse.getBody().getData() == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }

        OrderResponse order = orderResponse.getBody().getData();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String preparedBy = authentication != null ? authentication.getName() : "SYSTEM";
        DeliveryAssignment assignment = getOrCreateAssignment(
                request.getOrderId(), 
                order.getStoreId(), 
                preparedBy
        );

        if (assignment.getProductsPrepared()) {
            String errorMessage = String.format("Products đã được prepare cho order này. Assignment ID: %d", 
                    assignment.getId());
            log.warn(errorMessage);
            throw new AppException(ErrorCode.PRODUCTS_ALREADY_PREPARED);
        }

        if (assignment.getStoreId() == null || !assignment.getStoreId().equals(order.getStoreId())) {
            assignment.setStoreId(order.getStoreId());
        }
        
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
        orderClient.updateOrderStatus(order.getId(), EnumProcessOrder.SHIPPING);
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

    @Override
    @Transactional
    public DeliveryAssignmentResponse rejectAssignment(Long assignmentId, String reason, String deliveryStaffId) {
        log.info("Delivery staff {} rejecting assignment {}", deliveryStaffId, assignmentId);

        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND));

        // === VALIDATION ===

        // 1. Kiểm tra assignment thuộc về delivery staff hiện tại
        if (assignment.getDeliveryStaffId() == null || !assignment.getDeliveryStaffId().equals(deliveryStaffId)) {
            log.warn("Delivery staff {} tried to reject assignment {} assigned to {}", 
                    deliveryStaffId, assignmentId, assignment.getDeliveryStaffId());
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // 2. Kiểm tra status = ASSIGNED (chỉ có thể reject khi manager mới assign)
        if (assignment.getStatus() != DeliveryStatus.ASSIGNED) {
            log.warn("Cannot reject assignment {}: Status is {}, must be ASSIGNED", 
                    assignmentId, assignment.getStatus());
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        // 3. Update assignment
        assignment.setStatus(DeliveryStatus.CANCELLED);
        assignment.setRejectReason(reason);
        assignment.setRejectedAt(LocalDateTime.now());
        assignment.setRejectedBy(deliveryStaffId);

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        log.info("Assignment {} rejected by delivery staff {}", assignmentId, deliveryStaffId);

        // 4. Notify manager
        try {
            notifyManagerAboutRejection(assignment, deliveryStaffId, reason);
        } catch (Exception e) {
            log.error("Failed to notify manager about rejection: {}", e.getMessage());
            // Continue even if notification fails
        }

        // 5. Update order status (về CONFIRMED để manager có thể re-assign)
        try {
            orderClient.updateOrderStatus(assignment.getOrderId(), EnumProcessOrder.CONFIRMED);
            log.info("Order {} status updated to CONFIRMED after rejection", assignment.getOrderId());
        } catch (Exception e) {
            log.error("Failed to update order status: {}", e.getMessage());
            // Continue even if order status update fails
        }

        return mapToResponse(saved);
    }

    /**
     * Notify manager about delivery staff rejection
     */
    private void notifyManagerAboutRejection(DeliveryAssignment assignment, String deliveryStaffId, String reason) {
        try {
            String message = String.format(
                "⚠️ Delivery staff %s đã từ chối đơn hàng #%d.\n" +
                "📦 Store: %s\n" +
                "❌ Lý do: %s\n" +
                "🔄 Vui lòng re-assign cho delivery staff khác.",
                deliveryStaffId,
                assignment.getOrderId(),
                assignment.getStoreId(),
                reason
            );

            log.info("=== MANAGER NOTIFICATION ===");
            log.info("Store ID: {}", assignment.getStoreId());
            log.info("Order ID: {}", assignment.getOrderId());
            log.info("Delivery Staff ID: {}", deliveryStaffId);
            log.info("Message: {}", message);
            log.info("===========================");

            // TODO: Implement actual notification (Kafka, Email, SMS, etc.)
            // Example with Kafka:
            // DeliveryRejectedEvent event = DeliveryRejectedEvent.builder()
            //     .orderId(assignment.getOrderId())
            //     .storeId(assignment.getStoreId())
            //     .deliveryStaffId(deliveryStaffId)
            //     .reason(reason)
            //     .build();
            // kafkaTemplate.send("delivery-rejected-topic", event);

        } catch (Exception e) {
            log.error("Error notifying manager: {}", e.getMessage(), e);
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
                .storeId(assignment.getStoreId())
                .storeName(store.getName() != null && !store.getName().isEmpty() ? store.getName() : "")
                .deliveryStaffId(assignment.getDeliveryStaffId())
                .assignedBy(assignment.getAssignedBy())
                .assignedAt(assignment.getAssignedAt())
                .estimatedDeliveryDate(assignment.getEstimatedDeliveryDate())
                .status(assignment.getStatus())
                .notes(assignment.getNotes())
                .productsPrepared(assignment.getProductsPrepared())
                .productsPreparedAt(assignment.getProductsPreparedAt())
                .rejectReason(assignment.getRejectReason())
                .rejectedAt(assignment.getRejectedAt())
                .rejectedBy(assignment.getRejectedBy())
                .order(order)
                .build();
    }

    private DeliveryAssignment getOrCreateAssignment(Long orderId, String storeId, String assignedBy) {
        Optional<DeliveryAssignment> existing = deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(orderId);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            DeliveryAssignment newAssignment = DeliveryAssignment.builder()
                    .orderId(orderId)
                    .storeId(storeId)
                    .deliveryStaffId(null)
                    .assignedBy(assignedBy)
                    .assignedAt(LocalDateTime.now())
                    .status(DeliveryStatus.PREPARING)
                    .productsPrepared(false)
                    .invoiceGenerated(false)
                    .build();

            DeliveryAssignment saved = deliveryAssignmentRepository.save(newAssignment);
            log.info("Created new delivery assignment with ID: {} for order: {}", saved.getId(), orderId);
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Unique constraint violation - another thread created it
            log.info("Assignment already exists for order {} (race condition handled), fetching...", orderId);
            return deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(orderId)
                    .orElseThrow(() -> {
                        log.error("Failed to create or find assignment for order: {}", orderId);
                        return new AppException(ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND);
                    });
        }
    }

    private DeliveryAssignment assignDeliveryStaffAtomically(
            DeliveryAssignment assignment, 
            String deliveryStaffId, 
            String assignedBy,
            LocalDateTime estimatedDeliveryDate,
            String notes) {
        
        // Check if already assigned (atomic check)
        if (assignment.getDeliveryStaffId() != null && !assignment.getDeliveryStaffId().isEmpty()) {
            String errorMessage = String.format(
                    "Order đã được assign cho delivery staff: %s. Assignment ID: %d, Status: %s",
                    assignment.getDeliveryStaffId(), assignment.getId(), assignment.getStatus());
            log.warn(errorMessage);
            throw new AppException(ErrorCode.ASSIGNMENT_ALREADY_EXISTS);
        }

        // Atomic update
        assignment.setDeliveryStaffId(deliveryStaffId);
        assignment.setAssignedBy(assignedBy);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setEstimatedDeliveryDate(estimatedDeliveryDate);
        assignment.setStatus(DeliveryStatus.ASSIGNED);
        if (notes != null && !notes.isEmpty()) {
            assignment.setNotes(notes);
        }

        return deliveryAssignmentRepository.save(assignment);
    }


    private <T> T callWithRetry(java.util.function.Supplier<T> supplier, String operation, int maxRetries) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < maxRetries) {
            try {
                return supplier.get();
            } catch (Exception e) {
                attempts++;
                lastException = e;
                log.warn("Attempt {} failed for {}: {}. Retrying...", attempts, operation, e.getMessage());
                
                if (attempts < maxRetries) {
                    try {
                        Thread.sleep(100 * attempts); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
                    }
                }
            }
        }

        log.error("Failed {} after {} attempts. Last error: {}", operation, maxRetries, 
                lastException != null ? lastException.getMessage() : "Unknown");
        throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
    }


    /**
     * and ensure data consistency
     */
//    private OrderResponse sanitizeOrderResponse(OrderResponse order) {
//        if (order == null) {
//            return null;
//        }
//
//        UserResponse sanitizedUser = null;
//        if (order.getUser() != null) {
//            sanitizedUser = UserResponse.builder()
//                    .id(order.getUser().getId())
//                    .fullName(order.getUser().getFullName())
//                    .email(order.getUser().getEmail())
//                    .phone(order.getUser().getPhone())
//                    .gender(order.getUser().getGender())
//                    .birthday(order.getUser().getBirthday())
//                    .avatar(order.getUser().getAvatar())
//                    // Removed: role, status, createdAt, updatedAt, cccd, point
//                    .build();
//        }
//
//        Double depositPrice = order.getDepositPrice();
//        if (depositPrice == null) {
//            depositPrice = 0.0;
//        }
//
//        return OrderResponse.builder()
//                .id(order.getId())
//                .user(sanitizedUser)
//                .storeId(order.getStoreId())
//                .address(order.getAddress())
//                .total(order.getTotal())
//                .note(order.getNote())
//                .orderDate(order.getOrderDate())
//                .status(order.getStatus())
//                .reason(order.getReason())
//                .orderDetails(order.getOrderDetails())
//                .processOrders(order.getProcessOrders())
//                .payment(order.getPayment())
//                .qrCode(order.getQrCode())
//                .depositPrice(depositPrice)
//                .qrCodeGeneratedAt(order.getQrCodeGeneratedAt())
//                .build();
//    }


}

