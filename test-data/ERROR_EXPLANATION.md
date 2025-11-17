# Giải Thích Các Lỗi Trong Test

## 🔴 Lỗi 400 Bad Request

### 1. Assign Order - Lỗi 400

**Nguyên nhân có thể:**

#### a) Order đã được assign (CODE_EXISTED)
```java
// Code trong DeliveryServiceImpl.java:45-48
deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(request.getOrderId())
    .ifPresent(assignment -> {
        throw new AppException(ErrorCode.CODE_EXISTED); // 400 Bad Request
    });
```

**Giải thích:**
- Mỗi order chỉ có thể được assign **1 lần duy nhất**
- Nếu order đã có assignment (chưa bị xóa), sẽ trả về lỗi 400 với message "Code has existed"
- **Cách fix:** Kiểm tra xem order đã được assign chưa trước khi assign lại, hoặc sử dụng order khác

#### b) Validation Error (@NotNull)
```java
// Code trong AssignOrderRequest.java:16-20
@NotNull(message = "Order ID is required")
private Long orderId;

@NotNull(message = "Store ID is required")
private String storeId;
```

**Giải thích:**
- Nếu thiếu `orderId` hoặc `storeId` trong request body, sẽ trả về 400
- **Cách fix:** Đảm bảo request body có đầy đủ các trường bắt buộc

#### c) Order không tồn tại (CODE_NOT_FOUND)
```java
// Code trong DeliveryServiceImpl.java:51-54
ResponseEntity<ApiResponse<OrderResponse>> orderResponse = orderClient.getOrderById(request.getOrderId());
if (orderResponse.getBody() == null || orderResponse.getBody().getData() == null) {
    throw new AppException(ErrorCode.CODE_NOT_FOUND); // 404 Not Found
}
```

**Lưu ý:** Trường hợp này thường trả về **404 Not Found**, không phải 400

---

### 2. Generate Invoice - Lỗi 400

**Nguyên nhân:**

#### Invoice đã được generate (CODE_EXISTED)
```java
// Code trong DeliveryServiceImpl.java:119-121
if (assignment.getInvoiceGenerated()) {
    throw new AppException(ErrorCode.CODE_EXISTED); // 400 Bad Request
}
```

**Giải thích:**
- Mỗi order chỉ có thể generate invoice **1 lần duy nhất**
- Nếu invoice đã được generate, sẽ trả về lỗi 400
- **Cách fix:** Kiểm tra `invoiceGenerated` flag trước khi generate lại

#### Assignment không tồn tại (CODE_NOT_FOUND)
```java
// Code trong DeliveryServiceImpl.java:116-117
DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(orderId)
    .orElseThrow(() -> new AppException(ErrorCode.CODE_NOT_FOUND)); // 404 Not Found
```

**Lưu ý:** Trường hợp này thường trả về **404 Not Found**

---

### 3. Prepare Products - Lỗi 400

**Nguyên nhân:**

#### a) Products đã được prepare (CODE_EXISTED)
```java
// Code trong DeliveryServiceImpl.java:140-142
if (assignment.getProductsPrepared()) {
    throw new AppException(ErrorCode.CODE_EXISTED); // 400 Bad Request
}
```

**Giải thích:**
- Mỗi order chỉ có thể prepare products **1 lần duy nhất**
- **Cách fix:** Kiểm tra `productsPrepared` flag trước khi prepare lại

#### b) Stock không đủ (INVALID_REQUEST)
```java
// Code trong DeliveryServiceImpl.java:158-160
int availableStock = stockResponse.getData();
if (availableStock < detail.getQuantity()) {
    throw new AppException(ErrorCode.INVALID_REQUEST); // 400 Bad Request
}
```

**Giải thích:**
- Nếu số lượng sản phẩm trong kho (`availableStock`) **nhỏ hơn** số lượng order (`detail.getQuantity()`), sẽ trả về lỗi 400
- **Cách fix:** Đảm bảo có đủ stock trong inventory trước khi prepare products

#### c) Assignment không tồn tại (CODE_NOT_FOUND)
```java
// Code trong DeliveryServiceImpl.java:137-138
DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(request.getOrderId())
    .orElseThrow(() -> new AppException(ErrorCode.CODE_NOT_FOUND)); // 404 Not Found
```

**Lưu ý:** Trường hợp này thường trả về **404 Not Found**

---

## 🔴 Lỗi 500 Internal Server Error từ Order-Service

### Nguyên nhân có thể:

#### 1. Order-Service không khởi động
- Service có thể đã crash hoặc không được start
- **Cách kiểm tra:** `docker ps | grep order-service`

#### 2. Database connection issue
- Order-service không thể kết nối đến database
- **Cách kiểm tra:** Xem logs của order-service container

#### 3. Feign Client timeout
- Delivery-service gọi order-service nhưng bị timeout
- **Cách kiểm tra:** Xem logs của delivery-service khi gọi order-service

#### 4. NullPointerException hoặc Exception khác
- Code trong order-service có bug
- **Cách kiểm tra:** Xem logs chi tiết của order-service

### Cách Debug:

```bash
# 1. Kiểm tra order-service có đang chạy không
docker ps | grep order-service

# 2. Xem logs của order-service
docker logs order-service --tail 100

# 3. Kiểm tra network connection
docker exec delivery-service ping order-service

# 4. Test trực tiếp order-service endpoint
curl -X GET "http://152.53.227.115:8087/api/orders/search?keyword=&page=0&size=10" \
  -H "Authorization: Bearer {TOKEN}"
```

---

## 📊 Tóm Tắt Error Codes

| Error Code | HTTP Status | Message | Nguyên nhân |
|------------|-------------|---------|-------------|
| `CODE_EXISTED` | 400 | Code has existed | Order/Invoice/Products đã được xử lý |
| `CODE_NOT_FOUND` | 404 | Code not found | Order/Assignment không tồn tại |
| `INVALID_REQUEST` | 400 | Invalid Request | Stock không đủ, validation failed |
| `INVALID_STATUS` | 400 | Invalid Status | Status không hợp lệ |
| `UNCATEGORIZED_EXCEPTION` | 500 | Uncategorized error | Lỗi không xác định |

---

## ✅ Cách Tránh Lỗi 400

### 1. Trước khi Assign Order:
```powershell
# Kiểm tra order đã được assign chưa
GET /api/delivery/assignments/order/{orderId}
# Nếu trả về 404 → Order chưa được assign → Có thể assign
# Nếu trả về 200 → Order đã được assign → Không thể assign lại
```

### 2. Trước khi Generate Invoice:
```powershell
# Kiểm tra assignment và invoiceGenerated flag
GET /api/delivery/assignments/order/{orderId}
# Nếu invoiceGenerated = false → Có thể generate
# Nếu invoiceGenerated = true → Đã generate rồi
```

### 3. Trước khi Prepare Products:
```powershell
# Kiểm tra assignment và productsPrepared flag
GET /api/delivery/assignments/order/{orderId}
# Nếu productsPrepared = false → Có thể prepare
# Nếu productsPrepared = true → Đã prepare rồi

# Kiểm tra stock availability
GET /api/inventories/stock/total-available?productColorId={id}
# Đảm bảo availableStock >= orderQuantity
```

---

## 🔍 Debug Tips

1. **Luôn kiểm tra response body** để xem error message chi tiết
2. **Kiểm tra logs** của service để biết nguyên nhân chính xác
3. **Sử dụng Swagger UI** để test và xem response chi tiết
4. **Kiểm tra database** để verify trạng thái của assignment

