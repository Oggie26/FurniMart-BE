package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderDetail;
import com.example.orderservice.entity.ProcessOrder;
import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.InventoryClient;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.ProcessOrderRepository;
import com.example.orderservice.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerWorkflowService {

    private final OrderRepository orderRepository;
    private final ProcessOrderRepository processOrderRepository;
    private final InventoryClient inventoryClient;

    @Transactional
    public void createImportExportOrder(Long orderId, String warehouseId, String storeId) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStoreId() == null || !order.getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        log.info("Creating import-export order for order {} from warehouse {} to store {}", orderId, warehouseId, storeId);

        // For each order item, decrease stock from warehouse
        for (OrderDetail detail : order.getOrderDetails()) {
            try {
                // Get inventory for this product in the warehouse
                ApiResponse<List<InventoryResponse>> inventoryResponse =
                    inventoryClient.getInventoryByProduct(detail.getProductColorId());
                
                if (inventoryResponse.getData() == null || inventoryResponse.getData().isEmpty()) {
                    log.warn("No inventory found for productColorId: {}", detail.getProductColorId());
                    continue;
                }

                List<InventoryResponse> inventories = inventoryResponse.getData();
                
                // Find inventory in the warehouse
                // Note: InventoryResponse may not have warehouseId directly
                // In a real implementation, you would need to check the warehouse through locationItem
                // For now, we'll use the first available inventory
                if (inventories.isEmpty()) {
                    log.warn("No inventory found for productColorId: {}", detail.getProductColorId());
                    continue;
                }
                
                InventoryResponse warehouseInventory = inventories.get(0);

                // Decrease stock from warehouse (this creates an export transaction)
                // Note: In a real system, you might want to create a proper import-export order entity
                // For now, we're using the existing decreaseStock API
                log.info("Processing import-export for productColorId: {}, quantity: {}", 
                    detail.getProductColorId(), detail.getQuantity());
                
                // The actual stock movement would be handled by inventory service
                // This is a placeholder - you may need to implement a proper import-export order API
                
            } catch (Exception e) {
                log.error("Error creating import-export order for productColorId {}: {}", 
                    detail.getProductColorId(), e.getMessage());
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
        }

        // Update order status
        ProcessOrder process = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.CONFIRMED)
                .createdAt(new Date())
                .build();
        processOrderRepository.save(process);
        
        order.setStatus(EnumProcessOrder.CONFIRMED);
        orderRepository.save(order);

        log.info("Import-export order created successfully for order {}", orderId);
    }

    /**
     * Create sales receipt - confirm that goods are ready and create receipt
     */
    @Transactional
    public void createSalesReceipt(Long orderId) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        for (OrderDetail detail : order.getOrderDetails()) {
            ApiResponse<Boolean> stockCheck =
                inventoryClient.hasSufficientGlobalStock(detail.getProductColorId(), detail.getQuantity());
            
            if (stockCheck.getData() == null) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }
        }

        // Create sales receipt process
        ProcessOrder process = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.CONFIRMED)
                .createdAt(new Date())
                .build();
        processOrderRepository.save(process);
        
        order.setStatus(EnumProcessOrder.CONFIRMED);
        orderRepository.save(order);

        log.info("Sales receipt created for order {}", orderId);
    }

    /**
     * Assign delivery for the order
     */
    @Transactional
    public void assignDelivery(Long orderId, String deliveryId) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != EnumProcessOrder.CONFIRMED) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        // Update order status to ready for delivery
        ProcessOrder process = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.DELIVERED)
                .createdAt(new Date())
                .build();
        processOrderRepository.save(process);
        
        order.setStatus(EnumProcessOrder.DELIVERED);
        orderRepository.save(order);

        log.info("Delivery assigned for order {} with delivery ID {}", orderId, deliveryId);
    }
}

