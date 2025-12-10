package com.example.inventoryservice.response;

import com.example.inventoryservice.enums.ReserveStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReserveStockResponse {
    private int quantityRequested; // Số lượng khách đặt ban đầu
    private int quantityReserved;  // Số lượng giữ được thành công
    private int quantityMissing;   // Số lượng bị thiếu (nếu = 0 là đủ hàng)
    @Enumerated(EnumType.STRING)
    private ReserveStatus reserveStatus;
    private List<InventoryResponse> reservations;
}
