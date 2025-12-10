package com.example.inventoryservice.feign;

import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.DeliveryAssignmentResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "delivery-service", contextId = "deliveryClientForInventory")
public interface DeliveryClient {

    @PutMapping("/api/delivery/assignments/{assignmentId}/status")
    ApiResponse<DeliveryAssignmentResponse> updateDelivertAsiStatus(@Parameter(description = "Delivery assignment ID", required = true, example = "1")
                                                                    @PathVariable Long assignmentId,
                                                                    @Parameter(
                                                                            description = "Delivery status. Valid values: ASSIGNED, PREPARING, READY, IN_TRANSIT, DELIVERED, CANCELLED",
                                                                            required = true,
                                                                            schema = @Schema(
                                                                                    type = "string",
                                                                                    allowableValues = {"ASSIGNED", "PREPARING", "READY", "IN_TRANSIT", "DELIVERED", "CANCELLED"},
                                                                                    example = "IN_TRANSIT"
                                                                            )
                                                                    )
                                                                    @RequestParam String status);

    @GetMapping("api/delivery/assignments/order/{orderId}")
    ApiResponse<DeliveryAssignmentResponse> getDeliveryAsiByOrderId(@PathVariable Long orderId);
}
