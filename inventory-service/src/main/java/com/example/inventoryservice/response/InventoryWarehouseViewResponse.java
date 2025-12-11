package com.example.inventoryservice.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InventoryWarehouseViewResponse {

    private String warehouseId;

    private List<InventoryResponse> localTickets;

    private List<InventoryResponse> globalTickets;
}
