package com.example.inventoryservice.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LowStockAlertResponse {
    private String productColorId;
    private ProductColorResponse productColor;
    private String productName;
    private String colorName;
    private Integer currentStock; // Tồn kho hiện tại (available)
    private Integer totalStock; // Tổng tồn kho (physical)
    private Integer reservedStock; // Số lượng đã được đặt trước
    private Integer threshold; // Ngưỡng cảnh báo
    private String alertLevel; // "LOW" hoặc "CRITICAL"
    private String message; // Thông báo cảnh báo
}

