# Fix Lỗi Inventory Service - Endpoint Mismatch

## 🔴 Nguyên Nhân

**Lỗi**: `NoResourceFoundException: No static resource api/inventory/total-available/{productColorId}`

### Vấn Đề:

1. **Feign Client** trong `delivery-service` gọi:
   ```java
   @GetMapping("/api/inventory/total-available/{productColorId}")
   ApiResponse<Integer> getTotalAvailableStock(@PathVariable("productColorId") String productColorId);
   ```
   - Path: `/api/inventory/total-available/{productColorId}`
   - Sử dụng `@PathVariable`

2. **Endpoint thực tế** trong `inventory-service`:
   ```java
   @GetMapping("/stock/total-available")
   public ApiResponse<Integer> getAvailableStockByProductColorId(
           @RequestParam @NotBlank String productColorId)
   ```
   - Path: `/api/inventory/stock/total-available?productColorId=...`
   - Sử dụng `@RequestParam`
   - **Thiếu `/stock/` trong path của Feign client**

## 🔧 Giải Pháp

### Option 1: Sửa Feign Client (Khuyến Nghị)

Sửa `InventoryClient` trong `delivery-service` để match với endpoint thực tế:

```java
@GetMapping("/api/inventory/stock/total-available")
ApiResponse<Integer> getTotalAvailableStock(@RequestParam("productColorId") String productColorId);
```

### Option 2: Sửa Endpoint trong Inventory Service

Thêm endpoint mới hoặc sửa endpoint hiện tại để match với Feign client:

```java
@GetMapping("/total-available/{productColorId}")
public ApiResponse<Integer> getAvailableStockByProductColorId(
        @PathVariable @NotBlank String productColorId) {
    // ...
}
```

## ✅ Fix Được Áp Dụng

Sẽ sửa **Option 1** (sửa Feign client) vì:
- Endpoint trong inventory-service đã đúng (sử dụng query parameter)
- Không cần thay đổi inventory-service
- Chỉ cần sửa delivery-service

