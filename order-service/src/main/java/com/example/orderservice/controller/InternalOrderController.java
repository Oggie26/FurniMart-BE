package com.example.orderservice.controller;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.ProcessOrder;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.UserClient;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.ProcessOrderRepository;
import com.example.orderservice.response.AddressResponse;
import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.UserResponse;
import com.example.orderservice.service.PDFService;
import com.example.orderservice.service.inteface.OrderService;
import java.util.ArrayList;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
@Slf4j
public class InternalOrderController {

    private final OrderRepository orderRepository;
    private final PDFService pdfService;
    private final UserClient userClient;
    private final ProcessOrderRepository processOrderRepository;
    private final OrderService orderService;

    @PostMapping("/{orderId}/generate-pdf")
    public ApiResponse<String> generatePDF(@PathVariable Long orderId) {
        log.info("Internal API: Generating PDF for order: {}", orderId);
        
        // Validate orderId
        if (orderId == null || orderId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Verify orderId matches (prevent creating invoice for wrong order)
        if (!order.getId().equals(orderId)) {
            log.error("Order ID mismatch: requested {}, found {}", orderId, order.getId());
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Check if order is at MANAGER_ACCEPT
        if (order.getStatus() != EnumProcessOrder.MANAGER_ACCEPT) {
            log.warn("Cannot generate PDF for order {}: Order must be at MANAGER_ACCEPT. Current status: {}", 
                    orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
        
        // Check if order already has READY_FOR_INVOICE in status history
        boolean hasReadyForInvoice = false;
        if (order.getProcessOrders() != null && !order.getProcessOrders().isEmpty()) {
            hasReadyForInvoice = order.getProcessOrders().stream()
                    .anyMatch(po -> po.getStatus() == EnumProcessOrder.READY_FOR_INVOICE);
        }
        
        // Check duplicate: PDF file already exists and belongs to this order
        boolean pdfFileExists = false;
        if (order.getPdfFilePath() != null && !order.getPdfFilePath().isEmpty()) {
            try {
                java.io.File pdfFile = new java.io.File(order.getPdfFilePath());
                if (pdfFile.exists()) {
                    // Verify PDF file name contains this orderId to prevent wrong order invoice
                    String fileName = pdfFile.getName();
                    if (fileName.contains("order_" + orderId + "_") || fileName.contains("_" + orderId + ".pdf")) {
                        pdfFileExists = true;
                        log.info("PDF file already exists for order {}: {}", orderId, order.getPdfFilePath());
                    } else {
                        // PDF file exists but doesn't match orderId - this is suspicious, log warning
                        log.warn("PDF file exists but filename doesn't match orderId {}: {}", orderId, order.getPdfFilePath());
                        // Don't consider this as valid PDF for this order
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking PDF file existence for order {}: {}", orderId, e.getMessage());
                // Continue if file check fails, allow regeneration
            }
        }
        
        // If invoice already exists (READY_FOR_INVOICE in history) AND PDF file exists, cannot create again
        if (hasReadyForInvoice && pdfFileExists) {
            log.warn("Cannot generate PDF for order {}: Invoice already exists and PDF file is present.", orderId);
            throw new AppException(ErrorCode.INVOICE_ALREADY_GENERATED);
        }
        
        // If invoice exists but PDF file is missing, allow regeneration
        if (hasReadyForInvoice && !pdfFileExists) {
            log.info("Order {} has READY_FOR_INVOICE in history but PDF file is missing. Allowing PDF regeneration.", orderId);
        }

        // Get user and address
        UserResponse user = safeGetUser(order.getUserId());
        AddressResponse address = safeGetAddress(order.getAddressId());

        // Generate PDF
        String pdfPath = pdfService.generateOrderPDF(order, user, address);
        
        // Update order with PDF path
        order.setPdfFilePath(pdfPath);
        
        // Update order status to READY_FOR_INVOICE after successful PDF generation
        // Only add READY_FOR_INVOICE to history if it doesn't exist yet
        if (!hasReadyForInvoice) {
            ProcessOrder readyProcess = ProcessOrder.builder()
                    .order(order)
                    .status(EnumProcessOrder.READY_FOR_INVOICE)
                    .createdAt(new Date())
                    .build();
            processOrderRepository.save(readyProcess);
            
            order.setStatus(EnumProcessOrder.READY_FOR_INVOICE);
            if (order.getProcessOrders() == null) {
                order.setProcessOrders(new ArrayList<>());
            }
            order.getProcessOrders().add(readyProcess);
        } else {
            // If READY_FOR_INVOICE already exists in history, just update status and PDF path
            order.setStatus(EnumProcessOrder.READY_FOR_INVOICE);
        }
        orderRepository.save(order);
        
        log.info("PDF generated successfully for order {}: {}. Status: READY_FOR_INVOICE", orderId, pdfPath);
        
        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("PDF generated successfully")
                .data(pdfPath)
                .build();
    }

    private UserResponse safeGetUser(String userId) {
        if (userId == null) return null;
        try {
            ApiResponse<UserResponse> response = userClient.getUserById(userId);
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("Error getting user {}: {}", userId, e.getMessage());
        }
        return null;
    }

    private AddressResponse safeGetAddress(Long addressId) {
        if (addressId == null) return null;
        try {
            ApiResponse<AddressResponse> response = userClient.getAddressById(addressId);
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("Error getting address {}: {}", addressId, e.getMessage());
        }
        return null;
    }

    @PutMapping("/{orderId}/mark-customer-refused")
    public ApiResponse<Void> markCustomerRefused(
            @PathVariable Long orderId,
            @RequestParam(value = "contactable", required = false) Boolean contactable) {
        log.info("Internal API: Marking customer as refused for order: {}, contactable: {}", orderId, contactable);
        
        orderService.markCustomerRefused(orderId, contactable);
        
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Customer marked as refused")
                .data(null)
                .build();
    }
}
