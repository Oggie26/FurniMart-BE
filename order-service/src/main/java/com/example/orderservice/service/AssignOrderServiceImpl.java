package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderDetail;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.event.OrderPlacedEvent;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.InventoryClient;
import com.example.orderservice.feign.StoreClient;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.InventoryResponse;
import com.example.orderservice.service.inteface.AssignOrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignOrderServiceImpl implements AssignOrderService {

    private final StoreClient storeClient;
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    @Override
    @Transactional
    public String assignOrderToStore(Long orderId) {

        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        List<String> productIds = order.getOrderDetails()
                .stream()
                .map(OrderDetail::getProductId)
                .toList();

        List<InventoryResponse> inventories = new ArrayList<>();
        for (String productId : productIds) {
            inventories.addAll(getInventoryResponse(productId));
        }

//        String selectedStoreId = findStoreWithAvailableStock(inventories, productIds);
//
//        if (selectedStoreId == null) {
//            throw new AppException(ErrorCode.STORE_NOT_FOUND);
//        }
//
//        return selectedStoreId;
        return null;
    }

    private List<InventoryResponse> getInventoryResponse(String productId) {
        ResponseEntity<ApiResponse<List<InventoryResponse>>> response =  inventoryClient.getInventoryByProduct(productId);
        return response.getBody().getData();
    }

//    private String findStoreWithAvailableStock(List<InventoryResponse> inventories, List<OrderDetail> orderDetails) {
//        // Group tồn kho theo storeId
//        Map<String, List<InventoryResponse>> storeMap = inventories.stream()
//                .collect(Collectors.groupingBy(InventoryResponse::getStoreId));
//
//        for (String storeId : storeMap.keySet()) {
//            List<InventoryResponse> storeInventory = storeMap.get(storeId);
//
//            boolean hasAllProducts = orderDetails.stream().allMatch(orderDetail -> {
//                return storeInventory.stream()
//                        .anyMatch(inv ->
//                                inv.getProductId().equals(orderDetail.getProductId()) &&
//                                        inv.getQuantity() >= orderDetail.getQuantity()
//                        );
//            });
//
//            if (hasAllProducts) {
//                return storeId; // Store này đủ hàng
//            }
//        }
//
//        return null; // Không store nào đủ hàng
//    }

}
