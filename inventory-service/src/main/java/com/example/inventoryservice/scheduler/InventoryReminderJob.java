//package com.example.inventoryservice.scheduler;
//
//import com.example.inventoryservice.repository.InventoryRepository;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Component
//public class InventoryReminderJob {
//
//    private final InventoryRepository inventoryRepository;
//
//    public InventoryReminderJob(InventoryRepository inventoryRepository) {
//        this.inventoryRepository = inventoryRepository;
//    }
//
//    @Scheduled(fixedRate = 3600000)
//    public void checkLowStock() {
//        inventoryRepository.findAll().forEach(inventory -> {
//            if (inventory.getQuantity() < inventory.getMinQuantity()) {
//                System.out.println("⚠ Cảnh báo: " + inventory.getProductColorId()
//                        + " dưới mức tồn tối thiểu (" + inventory.getQuantity() + "/" + inventory.getMinQuantity() + ")");
//                // Ở đây có thể gửi email, notification hoặc call API
//            }
//        });
//    }
//}
//
