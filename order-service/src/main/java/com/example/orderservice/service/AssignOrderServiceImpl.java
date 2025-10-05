package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.ProcessOrder;
import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.InventoryClient;
import com.example.orderservice.feign.StoreClient;
import com.example.orderservice.feign.UserClient;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.ProcessOrderRepository;
import com.example.orderservice.response.*;
import com.example.orderservice.service.inteface.AssignOrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignOrderServiceImpl implements AssignOrderService {

    private final StoreClient storeClient;
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final UserClient userClient;
    private final ProcessOrderRepository processOrderRepository;

    @Override
    @Transactional
    public void assignOrderToStore(Long orderId) {

        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        AddressResponse address = safeGetAddress(order.getAddressId());

        if (address == null) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        order.setStoreId(getStoreNear(address.getLatitude(), address.getLongitude(), 1));
        order.setStatus(EnumProcessOrder.ASSIGN_ORDER_STORE);
        ProcessOrder process =  ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.ASSIGN_ORDER_STORE)
                .createdAt(new Date())
                .build();

        processOrderRepository.save(process);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void acceptRejectOrderByManager(Long orderId, String storeId, String reason, EnumProcessOrder status) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (status == EnumProcessOrder.MANAGER_ACCEPT) {
            handleManagerAccept(order);
        } else if (status == EnumProcessOrder.MANAGER_REJECT) {
            handleManagerReject(order,storeId, reason);
        } else {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
    }

    private void handleManagerAccept(Order order) {
        ProcessOrder process = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.MANAGER_ACCEPT)
                .createdAt(new Date())
                .build();
        processOrderRepository.save(process);
        order.setStatus(EnumProcessOrder.MANAGER_ACCEPT);
        order.setProcessOrders(order.getProcessOrders());
        orderRepository.save(order);
    }

    private void handleManagerReject(Order order, String storeId, String reason) {
        ProcessOrder rejectProcess = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.MANAGER_REJECT)
                .createdAt(new Date())
                .build();
        processOrderRepository.save(rejectProcess);
        order.setReason(reason);
        order.setStoreId(storeId);
        order.setStatus(EnumProcessOrder.ASSIGN_ORDER_STORE);
        orderRepository.save(order);

        ProcessOrder assignProcess = ProcessOrder.builder()
                .order(order)
                .status(EnumProcessOrder.ASSIGN_ORDER_STORE)
                .createdAt(new Date())
                .build();
        processOrderRepository.save(assignProcess);
    }

    private List<InventoryResponse> getInventoryResponse(String productId) {
        ResponseEntity<ApiResponse<List<InventoryResponse>>> response =  inventoryClient.getInventoryByProduct(productId);
        return response.getBody().getData();
    }

    private String getStoreById(String storeId) {
        ApiResponse<StoreResponse> response = storeClient.getStoreById(storeId);
        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }
        return response.getData().getId();
    }

    private String getStoreNear(Double lat, Double lon, int limit) {
        ApiResponse<List<StoreDistance>> response = storeClient.getNearestStores(lat, lon, limit);
        System.out.println(response);
        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        return response.getData().getFirst().getStore().getId();
    }

    private AddressResponse safeGetAddress(Long addressId) {
        if (addressId == null) return null;
        ApiResponse<AddressResponse> resp = userClient.getAddressById(addressId);
        if (resp == null || resp.getData() == null) return null;
        return resp.getData();
    }


}
