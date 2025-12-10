package com.example.inventoryservice.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReserveStockResponse {

    private long orderId;
    private String productColorId;
    private int totalNeeded;
    private int totalReserved;
    private List<WarehouseReserveInfo> globalReservations;
    private Map<String, String> warehousePrintContentMap;
}

