# Phân Tích Lỗi Prepare Products

## 🔴 Lỗi Chính

**Endpoint**: `POST /api/delivery/prepare-products`  
**Status Code**: 500 Internal Server Error  
**Error Message**: "Uncategorized error"

## 📍 Nguyên Nhân

Lỗi **KHÔNG** phải từ `delivery-service`, mà từ **`inventory-service`**!

### Chi Tiết:

Khi `delivery-service` gọi `inventoryClient.getTotalAvailableStock()`, `inventory-service` trả về lỗi 500:

```
feign.FeignException$InternalServerError: [500] during [GET] 
to [http://inventory-service/api/inventory/total-available/1d76a39a-3dc4-40f2-8d21-f7f1188e6a45] 
[InventoryClient#getTotalAvailableStock(String)]: 
[{"status":9999,"message":"Uncategorized error"}]
```

## 🔍 Phân Tích

### Flow của Request:
1. ✅ `delivery-service` nhận request `POST /api/delivery/prepare-products`
2. ✅ Tìm `DeliveryAssignment` thành công
3. ✅ Gọi `orderClient.getOrderById()` thành công
4. ❌ Gọi `inventoryClient.getTotalAvailableStock()` → **FAIL**
5. ❌ `inventory-service` trả về 500 Internal Server Error
6. ❌ Feign client throw `FeignException$InternalServerError`
7. ❌ `delivery-service` catch và trả về 500

### Vấn Đề:
- **`inventory-service`** đang có lỗi ở endpoint `/api/inventory/total-available/{productColorId}`
- Endpoint này trả về `{"status":9999,"message":"Uncategorized error"}`
- Có thể do:
  - Logic trong `inventory-service` có bug
  - Database issue
  - `productColorId` không hợp lệ
  - Service chưa được khởi động đúng cách

## 🔧 Giải Pháp

### 1. Kiểm Tra Inventory Service (Ưu Tiên)

```bash
# Kiểm tra service có đang chạy không
docker ps | grep inventory-service

# Kiểm tra logs để tìm nguyên nhân lỗi 500
docker logs inventory-service --tail 100 | grep -i error

# Test endpoint trực tiếp
curl http://152.53.227.115:8083/api/inventory/total-available/1d76a39a-3dc4-40f2-8d21-f7f1188e6a45
```

### 2. Fix Code - Add Error Handling (Tùy Chọn)

Thêm try-catch để xử lý lỗi Feign client gracefully:

```java
try {
    ApiResponse<Integer> stockResponse = inventoryClient.getTotalAvailableStock(detail.getProductColorId());
    if (stockResponse != null && stockResponse.getData() != null) {
        int availableStock = stockResponse.getData();
        if (availableStock < detail.getQuantity()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }
} catch (FeignException e) {
    log.error("Failed to get stock for productColorId: {}. Error: {}", 
              detail.getProductColorId(), e.getMessage());
    // Option 1: Skip stock check if service unavailable (not recommended)
    // Option 2: Throw specific error (recommended)
    throw new AppException(ErrorCode.SERVICE_UNAVAILABLE, 
                          "Inventory service is currently unavailable");
}
```

### 3. Verify Product Color ID

Kiểm tra xem `productColorId` có hợp lệ không:
- Có thể `productColorId` không tồn tại trong database
- Hoặc format không đúng

## 📋 Kết Luận

**Lỗi Prepare Products là do `inventory-service` đang có vấn đề**, không phải do `delivery-service`.

**Cần làm:**
1. ✅ Kiểm tra logs của `inventory-service`
2. ✅ Fix lỗi trong `inventory-service` endpoint `/api/inventory/total-available/{productColorId}`
3. ⏳ (Optional) Thêm error handling tốt hơn trong `delivery-service`

## 🚀 Next Steps

1. Kiểm tra `inventory-service` logs
2. Fix lỗi trong `inventory-service`
3. Test lại endpoint `prepare-products`
4. Verify end-to-end flow
