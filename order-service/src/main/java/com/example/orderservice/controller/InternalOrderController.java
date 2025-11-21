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

    @PostMapping("/{orderId}/generate-pdf")
    public ApiResponse<String> generatePDF(@PathVariable Long orderId) {
        log.info("Internal API: Generating PDF for order: {}", orderId);
        
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Check if order is at MANAGER_ACCEPT
        if (order.getStatus() != EnumProcessOrder.MANAGER_ACCEPT) {
            log.warn("Cannot generate PDF for order {}: Order must be at MANAGER_ACCEPT. Current status: {}", 
                    orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
        
        // Check if order already has READY_FOR_INVOICE in status history
        // If yes, PDF already exists, cannot create again
        boolean hasReadyForInvoice = false;
        if (order.getProcessOrders() != null && !order.getProcessOrders().isEmpty()) {
            hasReadyForInvoice = order.getProcessOrders().stream()
                    .anyMatch(po -> po.getStatus() == EnumProcessOrder.READY_FOR_INVOICE);
        }
        
        if (hasReadyForInvoice) {
            log.warn("Cannot generate PDF for order {}: Order already has READY_FOR_INVOICE in status history. PDF already exists.", orderId);
            throw new AppException(ErrorCode.INVOICE_ALREADY_GENERATED);
        }
        
        // Check duplicate: PDF already exists (check pdfFilePath and file existence)
        if (order.getPdfFilePath() != null && !order.getPdfFilePath().isEmpty()) {
            try {
                java.io.File pdfFile = new java.io.File(order.getPdfFilePath());
                if (pdfFile.exists()) {
                    log.warn("PDF already exists for order {}: {}", orderId, order.getPdfFilePath());
                    throw new AppException(ErrorCode.INVOICE_ALREADY_GENERATED);
                }
            } catch (AppException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Error checking PDF file existence: {}", e.getMessage());
                // Continue if file check fails, allow regeneration
            }
        }

        // Get user and address
        UserResponse user = safeGetUser(order.getUserId());
        AddressResponse address = safeGetAddress(order.getAddressId());

        // Generate PDF
        String pdfPath = pdfService.generateOrderPDF(order, user, address);
        
        // Update order with PDF path
        order.setPdfFilePath(pdfPath);
        
        // Update order status to READY_FOR_INVOICE after successful PDF generation
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
        orderRepository.save(order);
        
        log.info("PDF generated successfully for order {}: {}. Status updated to READY_FOR_INVOICE", orderId, pdfPath);
        
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
}

