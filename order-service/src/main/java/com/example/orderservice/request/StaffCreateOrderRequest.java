package com.example.orderservice.request;

import com.example.orderservice.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCreateOrderRequest {

    @NotBlank(message = "Customer userId is required")
    private String customerUserId;

    @NotBlank(message = "StoreId is required")
    private String storeId;

    @NotNull(message = "AddressId is required")
    private Long addressId;

    @NotEmpty(message = "Order details are required")
    @Valid
    private List<OrderDetailRequest> orderDetails;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String note;

    private String reason;
}

