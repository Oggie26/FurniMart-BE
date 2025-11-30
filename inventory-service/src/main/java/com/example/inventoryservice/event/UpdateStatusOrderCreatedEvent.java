package com.example.inventoryservice.event;

import com.example.inventoryservice.enums.EnumProcessOrder;
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
