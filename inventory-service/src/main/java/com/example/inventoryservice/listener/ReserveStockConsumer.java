package com.example.inventoryservice.listener;

import com.example.inventoryservice.event.OrderCancelRollbackStockEvent;
import com.example.inventoryservice.service.inteface.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReserveStockConsumer {

    private  final InventoryService inventoryService;

    @KafkaListener(topics = "order-cancel-rollback-topic", groupId = "inventory-group")
    public void onOrderCancelRollback(OrderCancelRollbackStockEvent event) {

        event.getItems().forEach(item ->
                inventoryService.rollbackInventoryTicket(
                        event.getOrderId()
                )
        );
    }
}
