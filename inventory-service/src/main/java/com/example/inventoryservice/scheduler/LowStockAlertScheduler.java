package com.example.inventoryservice.scheduler;

import com.example.inventoryservice.response.LowStockAlertResponse;
import com.example.inventoryservice.service.inteface.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled task để kiểm tra và cảnh báo sản phẩm sắp hết hàng định kỳ
 * Chạy mỗi ngày lúc 8:00 AM và 6:00 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LowStockAlertScheduler {

    private final InventoryService inventoryService;

    @Value("${inventory.low-stock.threshold:10}")
    private Integer defaultThreshold;

    @Value("${inventory.low-stock.enabled:true}")
    private Boolean enabled;

    /**
     * Kiểm tra low stock mỗi ngày lúc 8:00 AM
     */
    @Scheduled(cron = "0 0 8 * * ?") // 8:00 AM mỗi ngày
    public void checkLowStockMorning() {
        if (!enabled) {
            log.debug("Low stock alert scheduler is disabled");
            return;
        }
        
        log.info("[SCHEDULED] Bắt đầu kiểm tra sản phẩm sắp hết hàng (8:00 AM)");
        checkAndLogLowStock();
    }

    /**
     * Kiểm tra low stock mỗi ngày lúc 6:00 PM
     */
    @Scheduled(cron = "0 0 18 * * ?") // 6:00 PM mỗi ngày
    public void checkLowStockEvening() {
        if (!enabled) {
            log.debug("Low stock alert scheduler is disabled");
            return;
        }
        
        log.info("[SCHEDULED] Bắt đầu kiểm tra sản phẩm sắp hết hàng (6:00 PM)");
        checkAndLogLowStock();
    }

    /**
     * Kiểm tra low stock và ghi log cảnh báo
     * Có thể mở rộng để gửi email/notification
     */
    private void checkAndLogLowStock() {
        try {
            List<LowStockAlertResponse> alerts = inventoryService.getLowStockProducts(defaultThreshold);
            
            if (alerts.isEmpty()) {
                log.info("Không có sản phẩm nào sắp hết hàng (threshold: {})", defaultThreshold);
                return;
            }
            
            log.warn("[CẢNH BÁO] Tìm thấy {} sản phẩm sắp hết hàng:", alerts.size());
            
            int criticalCount = 0;
            int lowCount = 0;
            
            for (LowStockAlertResponse alert : alerts) {
                if ("CRITICAL".equals(alert.getAlertLevel())) {
                    criticalCount++;
                    log.error("[CRITICAL] {} - {}: HẾT HÀNG! (Tồn kho: {})", 
                        alert.getProductName(), 
                        alert.getColorName(),
                        alert.getCurrentStock());
                } else {
                    lowCount++;
                    log.warn("[LOW] {} - {}: Còn {} sản phẩm (Ngưỡng: {})", 
                        alert.getProductName(), 
                        alert.getColorName(),
                        alert.getCurrentStock(),
                        alert.getThreshold());
                }
            }
            
            log.warn("Tổng kết: {} sản phẩm CRITICAL, {} sản phẩm LOW", criticalCount, lowCount);
            
            // TODO: Có thể mở rộng để:
            // 1. Gửi email cho quản lý kho
            // 2. Gửi notification qua Kafka
            // 3. Tạo báo cáo tự động
            // 4. Gửi SMS/Telegram alert
            
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra low stock: {}", e.getMessage(), e);
        }
    }
}

