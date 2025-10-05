package com.example.orderservice.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderRequest {

    @NotBlank(message = "UserId is required")
    private String userId;

    @NotBlank(message = "StoreId is required")
    private String storeId;

    @NotNull(message = "AddressId is required")
    private Integer addressId;

    @Min(value = 1, message = "Total quantity must be greater than 0")
    private Integer quantity;

    @Min(value = 1, message = "Total amount must be greater than 0")
    private Double total;

    private String note;

    private String reason;

    @NotNull(message = "Order details are required")
    private List<OrderDetailRequest> orderDetails;
}
