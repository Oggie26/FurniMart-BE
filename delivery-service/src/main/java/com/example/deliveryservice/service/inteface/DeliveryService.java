package com.example.deliveryservice.service.inteface;

import com.example.deliveryservice.request.AssignOrderRequest;
import com.example.deliveryservice.request.PrepareProductsRequest;
import com.example.deliveryservice.response.DeliveryAssignmentResponse;
import com.example.deliveryservice.response.DeliveryProgressResponse;
import com.example.deliveryservice.response.StoreBranchInfoResponse;

import java.util.List;

public interface DeliveryService {
    
    // Assign order to delivery staff
    DeliveryAssignmentResponse assignOrderToDelivery(AssignOrderRequest request);
    
    // Get store branch information with stock availability (for guests)
    StoreBranchInfoResponse getStoreBranchInfo(String storeId);
    
    // Generate invoice (for branch manager)
    DeliveryAssignmentResponse generateInvoice(Long orderId);
    
    // Prepare products for delivery (for staff)
    DeliveryAssignmentResponse prepareProducts(PrepareProductsRequest request);
    
    // Monitor delivery progress (for branch manager)
    DeliveryProgressResponse getDeliveryProgressByStore(String storeId);
    
    // Get delivery assignments by store
    List<DeliveryAssignmentResponse> getDeliveryAssignmentsByStore(String storeId);
    
    // Get delivery assignments by delivery staff
    List<DeliveryAssignmentResponse> getDeliveryAssignmentsByStaff(String deliveryStaffId);
    
    // Get delivery assignment by order ID
    DeliveryAssignmentResponse getDeliveryAssignmentByOrderId(Long orderId);
    
    // Update delivery status
    DeliveryAssignmentResponse updateDeliveryStatus(Long assignmentId, String status);
}
