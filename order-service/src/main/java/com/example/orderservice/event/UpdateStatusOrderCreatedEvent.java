package com.example.orderservice.event;

import com.example.orderservice.enums.EnumProcessOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusOrderCreatedEvent {
    private Long orderId;
    private EnumProcessOrder enumProcessOrder;
}
