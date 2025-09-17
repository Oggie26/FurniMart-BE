package com.example.orderservice.request;

import com.example.orderservice.enums.EnumProcessOrder;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProcessOrderRequest {

    @NotNull(message = "OrderId is required")
    private String orderId;

    @NotNull(message = "Status is required")
    private EnumProcessOrder status;
}
