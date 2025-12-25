package com.example.deliveryservice.service.inteface;

import com.example.deliveryservice.request.AssignOrderRequest;
import com.example.deliveryservice.request.PrepareProductsRequest;
import com.example.deliveryservice.response.DeliveryAssignmentResponse;
import com.example.deliveryservice.response.DeliveryProgressResponse;
import com.example.deliveryservice.response.StoreBranchInfoResponse;

import java.util.List;

public interface DeliveryService {

    DeliveryAssignmentResponse assignOrderToDelivery(AssignOrderRequest request);

    StoreBranchInfoResponse getStoreBranchInfo(String storeId);

    DeliveryAssignmentResponse prepareProducts(PrepareProductsRequest request);

    DeliveryProgressResponse getDeliveryProgressByStore(String storeId);

    List<DeliveryAssignmentResponse> getDeliveryAssignmentsByStore(String storeId);

    List<DeliveryAssignmentResponse> getDeliveryAssignmentsByStaff(String deliveryStaffId);

    DeliveryAssignmentResponse getDeliveryAssignmentByOrderId(Long orderId);

    DeliveryAssignmentResponse updateDeliveryStatus(Long assignmentId, String status);

    DeliveryAssignmentResponse rejectAssignment(Long assignmentId, String reason, String deliveryStaffId);

    List<com.example.deliveryservice.response.UserResponse> getFreeDrivers();

    DeliveryAssignmentResponse createAssignment(Long orderId, String storeId);
}
