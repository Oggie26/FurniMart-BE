# Hướng Dẫn Test Assign Order Delivery

## Cách 1: Sử dụng Script Tự Động (Khuyến Nghị)

### ✨ Tính Năng Tự Động Lấy Thông Tin

Script sẽ **tự động lấy thông tin** từ các API:
- ✅ **Store ID**: Lấy từ `GET /api/stores` (store đầu tiên)
- ✅ **Order ID**: Tìm order chưa được assign từ `GET /api/orders/search`
- ✅ **Delivery Staff ID**: Lấy từ `GET /api/employees/role/delivery` (delivery staff đầu tiên)

**Lợi ích:**
- Không cần nhập thủ công store ID, order ID
- Tự động tìm order chưa được assign
- Tự động fallback nếu không tìm thấy dữ liệu

### Windows (PowerShell)

1. Mở PowerShell
2. Chỉnh sửa email/password trong script (nếu cần):
```powershell
$STAFF_EMAIL = "staff@furnimart.com"
$STAFF_PASSWORD = "Staff@123"
```

3. Chạy script:
```powershell
cd test-data
.\test-assign-order-delivery.ps1
```

### Linux/Mac (Bash)

1. Mở Terminal
2. Cấp quyền thực thi:
```bash
chmod +x test-data/test-assign-order-delivery.sh
```

3. Chỉnh sửa email/password trong script (nếu cần):
```bash
STAFF_EMAIL="staff@furnimart.com"
STAFF_PASSWORD="Staff@123"
```

4. Chạy script:
```bash
./test-data/test-assign-order-delivery.sh
```

**Lưu ý:** 
- Chỉ cần chỉnh sửa `STAFF_EMAIL` và `STAFF_PASSWORD`
- Các thông tin khác (store ID, order ID, delivery staff ID) sẽ được lấy tự động
- Script sẽ hiển thị thông tin đã lấy được trước khi test

---

## Cách 2: Test Thủ Công với Swagger UI

### Bước 1: Truy cập Swagger UI

1. Mở trình duyệt
2. Truy cập: `http://152.53.227.115:8089/swagger-ui.html`

### Bước 2: Đăng nhập để lấy token

1. Tìm endpoint `POST /api/auth/login` (trong User Service)
2. Click "Try it out"
3. Nhập thông tin:
```json
{
  "email": "staff@furnimart.com",
  "password": "Staff@123"
}
```
4. Click "Execute"
5. Copy `token` từ response

### Bước 3: Authorize trong Swagger

1. Click nút "Authorize" ở đầu trang Swagger
2. Nhập: `Bearer {token}` (thay {token} bằng token vừa copy)
3. Click "Authorize" và "Close"

### Bước 4: Test Assign Order

1. Tìm endpoint `POST /api/delivery/assign`
2. Click "Try it out"
3. Nhập request body:
```json
{
  "orderId": 1,
  "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
  "deliveryStaffId": "ef5ec40c-198f-4dfb-84dc-5bf86db68940",
  "estimatedDeliveryDate": "2025-11-15T10:00:00",
  "notes": "Test từ Swagger UI"
}
```
4. Click "Execute"
5. Xem kết quả

---

## Cách 3: Test với cURL (Command Line)

### Bước 1: Đăng nhập

```bash
curl -X POST "http://152.53.227.115:8086/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "staff@furnimart.com",
    "password": "Staff@123"
  }'
```

**Lưu token từ response vào biến:**
```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."  # Thay bằng token thực tế
```

### Bước 2: Test Assign Order

```bash
curl -X POST "http://152.53.227.115:8089/api/delivery/assign" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "orderId": 1,
    "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
    "deliveryStaffId": "ef5ec40c-198f-4dfb-84dc-5bf86db68940",
    "estimatedDeliveryDate": "2025-11-15T10:00:00",
    "notes": "Test từ cURL"
  }'
```

### Bước 3: Test các trường hợp khác

**Test missing orderId:**
```bash
curl -X POST "http://152.53.227.115:8089/api/delivery/assign" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7"
  }'
```

**Test order not found:**
```bash
curl -X POST "http://152.53.227.115:8089/api/delivery/assign" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "orderId": 99999,
    "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7"
  }'
```

**Test unauthorized (no token):**
```bash
curl -X POST "http://152.53.227.115:8089/api/delivery/assign" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7"
  }'
```

**Get assignments by store:**
```bash
curl -X GET "http://152.53.227.115:8089/api/delivery/assignments/store/8d46e317-0596-4413-81b6-1a526398b3d7" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Cách 4: Test với Postman

### Bước 1: Import Collection

1. Mở Postman
2. Import collection từ file (nếu có)
3. Hoặc tạo request mới

### Bước 2: Tạo Environment Variables

Tạo environment với các biến:
- `base_url`: `http://152.53.227.115:8086`
- `delivery_service_url`: `http://152.53.227.115:8089`
- `token`: (sẽ được set sau khi login)

### Bước 3: Test Login

1. Tạo request: `POST {{base_url}}/api/auth/login`
2. Body (raw JSON):
```json
{
  "email": "staff@furnimart.com",
  "password": "Staff@123"
}
```
3. Chạy request
4. Trong Tests tab, thêm script để lưu token:
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.data.token);
}
```

### Bước 4: Test Assign Order

1. Tạo request: `POST {{delivery_service_url}}/api/delivery/assign`
2. Headers:
   - `Authorization`: `Bearer {{token}}`
   - `Content-Type`: `application/json`
3. Body (raw JSON):
```json
{
  "orderId": 1,
  "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
  "deliveryStaffId": "ef5ec40c-198f-4dfb-84dc-5bf86db68940",
  "estimatedDeliveryDate": "2025-11-15T10:00:00",
  "notes": "Test từ Postman"
}
```
4. Chạy request và xem kết quả

---

## Kiểm Tra Kết Quả

### Success Response (201 Created)
```json
{
  "status": 201,
  "message": "Order assigned to delivery successfully",
  "data": {
    "id": 1,
    "orderId": 1,
    "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
    "deliveryStaffId": "ef5ec40c-198f-4dfb-84dc-5bf86db68940",
    "status": "ASSIGNED",
    "invoiceGenerated": false,
    "productsPrepared": false
  }
}
```

### Error Response (400 Bad Request)
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "orderId",
      "message": "Order ID is required"
    }
  ]
}
```

### Error Response (404 Not Found)
```json
{
  "status": 404,
  "message": "Code not found"
}
```

### Error Response (401 Unauthorized)
```json
{
  "status": 401,
  "message": "Unauthorized"
}
```

---

## Troubleshooting

### Lỗi: "Connection refused"
- Kiểm tra service có đang chạy không
- Kiểm tra URL và port có đúng không

### Lỗi: "401 Unauthorized"
- Token đã hết hạn, cần đăng nhập lại
- Token không đúng format (phải có "Bearer " ở đầu)

### Lỗi: "403 Forbidden"
- User không có quyền (không phải STAFF hoặc BRANCH_MANAGER)
- Kiểm tra role của user

### Lỗi: "404 Not Found"
- Order ID không tồn tại
- Store ID không tồn tại
- Kiểm tra lại dữ liệu test

### Lỗi: "400 Bad Request - Code existed"
- Order đã được assign rồi
- Cần kiểm tra xem order đã có assignment chưa

---

## Checklist Test

- [ ] Đăng nhập thành công
- [ ] Assign order thành công (với đầy đủ thông tin)
- [ ] Assign order thành công (không có deliveryStaffId)
- [ ] Test missing orderId → 400 error
- [ ] Test missing storeId → 400 error
- [ ] Test order not found → 404 error
- [ ] Test store not found → 404 error
- [ ] Test order already assigned → 400 error
- [ ] Test unauthorized (no token) → 401 error
- [ ] Test forbidden (wrong role) → 403 error
- [ ] Get assignments by store thành công

---

## Tips

1. **Lưu token:** Sau khi login, lưu token để dùng cho các request tiếp theo
2. **Check logs:** Nếu có lỗi, kiểm tra logs của delivery-service
3. **Test từng bước:** Test từng scenario một, không test tất cả cùng lúc
4. **Verify data:** Đảm bảo order ID và store ID tồn tại trong database
5. **Clean up:** Sau khi test, có thể cần xóa assignment để test lại

