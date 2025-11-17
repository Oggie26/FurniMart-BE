# Kịch Bản Test: Assign Order to Delivery

## Endpoint
**POST** `/api/delivery/assign`

**Authorization:** Required (Bearer Token)
**Roles:** `STAFF` hoặc `BRANCH_MANAGER` (chỉ quản lý mới có quyền assign)

**Lưu ý:** Role `DELIVERY` **KHÔNG** có quyền assign order. DELIVERY staff chỉ có thể:
- Xem assignments được giao cho mình
- Cập nhật trạng thái delivery
- Xác nhận giao hàng

**Lý do:** Đây là logic phân quyền hợp lý:
- **STAFF/BRANCH_MANAGER**: Quản lý và phân công công việc (assign orders)
- **DELIVERY**: Nhận công việc và thực hiện giao hàng (receive assignments)

---

## Prerequisites (Điều kiện cần thiết)

1. **Authentication:**
   - Có tài khoản STAFF hoặc BRANCH_MANAGER
   - Đã đăng nhập và có JWT token hợp lệ

2. **Dữ liệu test:**
   - Order ID hợp lệ (đã tồn tại trong hệ thống)
   - Store ID hợp lệ (đã tồn tại trong hệ thống)
   - Delivery Staff ID (optional - có thể null)
   - Order chưa được assign cho delivery nào khác

3. **Base URL:**
   - Local: `http://localhost:8089`
   - Server: `http://152.53.227.115:8089`
   - Gateway: `http://152.53.227.115:8086` (nếu qua gateway)

---

## Test Scenarios

### Scenario 1: Assign Order Successfully (Happy Path)

**Mô tả:** Assign một order hợp lệ cho delivery staff thành công

**Preconditions:**
- User đã đăng nhập với role STAFF hoặc BRANCH_MANAGER
- Order tồn tại và chưa được assign
- Store tồn tại

**Request:**
```json
POST /api/delivery/assign
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "orderId": 1,
  "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
  "deliveryStaffId": "ef5ec40c-198f-4dfb-84dc-5bf86db68940",
  "estimatedDeliveryDate": "2025-11-15T10:00:00",
  "notes": "Giao hàng vào buổi sáng, khách hàng ở nhà"
}
```

**Expected Response:**
```json
{
  "status": 201,
  "message": "Order assigned to delivery successfully",
  "data": {
    "id": 1,
    "orderId": 1,
    "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
    "deliveryStaffId": "ef5ec40c-198f-4dfb-84dc-5bf86db68940",
    "assignedBy": "staff@furnimart.com",
    "assignedAt": "2025-11-10T08:30:00",
    "estimatedDeliveryDate": "2025-11-15T10:00:00",
    "status": "ASSIGNED",
    "notes": "Giao hàng vào buổi sáng, khách hàng ở nhà",
    "invoiceGenerated": false,
    "productsPrepared": false,
    "order": {
      "id": 1,
      "status": "CONFIRMED",
      "totalAmount": 1500000.00
    },
    "store": {
      "id": "8d46e317-0596-4413-81b6-1a526398b3d7",
      "name": "FurniMart Store Hà Nội",
      "addressLine": "123 Đường ABC, Quận XYZ"
    }
  },
  "timestamp": "2025-11-10T08:30:00.000Z"
}
```

**Validation Points:**
- ✅ Status code: 201 Created
- ✅ Response có đầy đủ thông tin assignment
- ✅ Status = "ASSIGNED"
- ✅ invoiceGenerated = false
- ✅ productsPrepared = false
- ✅ assignedBy = email của user hiện tại
- ✅ assignedAt được set tự động

---

### Scenario 2: Assign Order Without Delivery Staff (Optional Field)

**Mô tả:** Assign order mà không chỉ định delivery staff (có thể assign sau)

**Request:**
```json
POST /api/delivery/assign
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "orderId": 2,
  "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
  "estimatedDeliveryDate": "2025-11-16T14:00:00",
  "notes": "Chưa có nhân viên giao hàng, sẽ assign sau"
}
```

**Expected Response:**
```json
{
  "status": 201,
  "message": "Order assigned to delivery successfully",
  "data": {
    "id": 2,
    "orderId": 2,
    "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
    "deliveryStaffId": null,
    "assignedBy": "manager@furnimart.com",
    "assignedAt": "2025-11-10T08:35:00",
    "estimatedDeliveryDate": "2025-11-16T14:00:00",
    "status": "ASSIGNED",
    "notes": "Chưa có nhân viên giao hàng, sẽ assign sau",
    "invoiceGenerated": false,
    "productsPrepared": false
  }
}
```

**Validation Points:**
- ✅ Status code: 201 Created
- ✅ deliveryStaffId = null (cho phép)
- ✅ Assignment vẫn được tạo thành công

---

### Scenario 3: Missing Required Field - orderId

**Mô tả:** Request thiếu trường bắt buộc orderId

**Request:**
```json
POST /api/delivery/assign
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7"
}
```

**Expected Response:**
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "orderId",
      "message": "Order ID is required"
    }
  ],
  "timestamp": "2025-11-10T08:40:00.000Z"
}
```

**Validation Points:**
- ✅ Status code: 400 Bad Request
- ✅ Error message chỉ rõ field bị thiếu

---

### Scenario 4: Missing Required Field - storeId

**Mô tả:** Request thiếu trường bắt buộc storeId

**Request:**
```json
POST /api/delivery/assign
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "orderId": 1
}
```

**Expected Response:**
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "storeId",
      "message": "Store ID is required"
    }
  ],
  "timestamp": "2025-11-10T08:41:00.000Z"
}
```

---

### Scenario 5: Order Not Found

**Mô tả:** Order ID không tồn tại trong hệ thống

**Request:**
```json
POST /api/delivery/assign
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "orderId": 99999,
  "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7"
}
```

**Expected Response:**
```json
{
  "status": 404,
  "message": "Code not found",
  "timestamp": "2025-11-10T08:42:00.000Z"
}
```

**Validation Points:**
- ✅ Status code: 404 Not Found
- ✅ Error message phù hợp

---

### Scenario 6: Store Not Found

**Mô tả:** Store ID không tồn tại trong hệ thống

**Request:**
```json
POST /api/delivery/assign
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "orderId": 1,
  "storeId": "00000000-0000-0000-0000-000000000000"
}
```

**Expected Response:**
```json
{
  "status": 404,
  "message": "Code not found",
  "timestamp": "2025-11-10T08:43:00.000Z"
}
```

---

### Scenario 7: Order Already Assigned

**Mô tả:** Order đã được assign cho delivery staff khác

**Preconditions:**
- Order ID = 1 đã được assign trước đó

**Request:**
```json
POST /api/delivery/assign
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "orderId": 1,
  "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
  "deliveryStaffId": "ef5ec40c-198f-4dfb-84dc-5bf86db68940"
}
```

**Expected Response:**
```json
{
  "status": 400,
  "message": "Code existed",
  "timestamp": "2025-11-10T08:44:00.000Z"
}
```

**Validation Points:**
- ✅ Status code: 400 Bad Request
- ✅ Error message chỉ rõ order đã được assign

---

### Scenario 8: Unauthorized Access - No Token

**Mô tả:** Request không có JWT token

**Request:**
```json
POST /api/delivery/assign
Content-Type: application/json

{
  "orderId": 1,
  "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7"
}
```

**Expected Response:**
```json
{
  "status": 401,
  "message": "Unauthorized",
  "timestamp": "2025-11-10T08:45:00.000Z"
}
```

**Validation Points:**
- ✅ Status code: 401 Unauthorized

---

### Scenario 9: Forbidden Access - Wrong Role (DELIVERY Staff)

**Mô tả:** DELIVERY staff không có quyền assign order (chỉ có thể nhận assignments)

**Preconditions:**
- User đăng nhập với role DELIVERY (hoặc CUSTOMER)

**Request:**
```json
POST /api/delivery/assign
Authorization: Bearer {DELIVERY_JWT_TOKEN}
Content-Type: application/json

{
  "orderId": 1,
  "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7"
}
```

**Expected Response:**
```json
{
  "status": 403,
  "message": "Forbidden",
  "timestamp": "2025-11-10T08:46:00.000Z"
}
```

**Validation Points:**
- ✅ Status code: 403 Forbidden
- ✅ DELIVERY staff không thể assign order cho chính mình hoặc người khác
- ✅ DELIVERY staff chỉ có thể xem và cập nhật trạng thái assignments được giao cho mình

---

### Scenario 10: Invalid Order ID Format

**Mô tả:** Order ID không đúng format (phải là số)

**Request:**
```json
POST /api/delivery/assign
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "orderId": "invalid",
  "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7"
}
```

**Expected Response:**
```json
{
  "status": 400,
  "message": "Bad Request",
  "errors": [
    {
      "field": "orderId",
      "message": "Order ID must be a number"
    }
  ],
  "timestamp": "2025-11-10T08:47:00.000Z"
}
```

---

### Scenario 11: Invalid Store ID Format

**Mô tả:** Store ID không đúng format UUID

**Request:**
```json
POST /api/delivery/assign
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "orderId": 1,
  "storeId": "invalid-uuid"
}
```

**Expected Response:**
```json
{
  "status": 400,
  "message": "Bad Request",
  "errors": [
    {
      "field": "storeId",
      "message": "Store ID must be a valid UUID"
    }
  ],
  "timestamp": "2025-11-10T08:48:00.000Z"
}
```

---

### Scenario 12: Invalid Estimated Delivery Date Format

**Mô tả:** Estimated delivery date không đúng format

**Request:**
```json
POST /api/delivery/assign
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "orderId": 1,
  "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
  "estimatedDeliveryDate": "invalid-date"
}
```

**Expected Response:**
```json
{
  "status": 400,
  "message": "Bad Request",
  "errors": [
    {
      "field": "estimatedDeliveryDate",
      "message": "Invalid date format. Expected: yyyy-MM-ddTHH:mm:ss"
    }
  ],
  "timestamp": "2025-11-10T08:49:00.000Z"
}
```

---

## Test Execution Steps

### Bước 1: Chuẩn bị dữ liệu test
```bash
# 1. Đăng nhập với tài khoản STAFF hoặc BRANCH_MANAGER
POST /api/auth/login
{
  "email": "staff@furnimart.com",
  "password": "Staff@123"
}

# Lưu JWT token từ response
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# 2. Tạo order test (nếu chưa có)
POST /api/orders
Authorization: Bearer {TOKEN}
{
  "customerId": "customer-uuid",
  "items": [...]
}

# Lưu orderId từ response
ORDER_ID=1

# 3. Lấy store ID hợp lệ
STORE_ID="8d46e317-0596-4413-81b6-1a526398b3d7"
```

### Bước 2: Test Success Case
```bash
curl -X POST "http://152.53.227.115:8089/api/delivery/assign" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": '${ORDER_ID}',
    "storeId": "'${STORE_ID}'",
    "deliveryStaffId": "ef5ec40c-198f-4dfb-84dc-5bf86db68940",
    "estimatedDeliveryDate": "2025-11-15T10:00:00",
    "notes": "Test assignment"
  }'
```

### Bước 3: Test Validation Errors
```bash
# Test missing orderId
curl -X POST "http://152.53.227.115:8089/api/delivery/assign" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "storeId": "'${STORE_ID}'"
  }'

# Test missing storeId
curl -X POST "http://152.53.227.115:8089/api/delivery/assign" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": '${ORDER_ID}'
  }'
```

### Bước 4: Test Error Cases
```bash
# Test order not found
curl -X POST "http://152.53.227.115:8089/api/delivery/assign" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 99999,
    "storeId": "'${STORE_ID}'"
  }'

# Test store not found
curl -X POST "http://152.53.227.115:8089/api/delivery/assign" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": '${ORDER_ID}',
    "storeId": "00000000-0000-0000-0000-000000000000"
  }'

# Test order already assigned (chạy lại request từ Bước 2)
curl -X POST "http://152.53.227.115:8089/api/delivery/assign" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": '${ORDER_ID}',
    "storeId": "'${STORE_ID}'"
  }'
```

### Bước 5: Test Authorization
```bash
# Test without token
curl -X POST "http://152.53.227.115:8089/api/delivery/assign" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": '${ORDER_ID}',
    "storeId": "'${STORE_ID}'"
  }'

# Test with DELIVERY token (should fail - DELIVERY không có quyền assign)
DELIVERY_TOKEN="..."
curl -X POST "http://152.53.227.115:8089/api/delivery/assign" \
  -H "Authorization: Bearer ${DELIVERY_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": '${ORDER_ID}',
    "storeId": "'${STORE_ID}'"
  }'

# Test with CUSTOMER token (should fail)
CUSTOMER_TOKEN="..."
curl -X POST "http://152.53.227.115:8089/api/delivery/assign" \
  -H "Authorization: Bearer ${CUSTOMER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": '${ORDER_ID}',
    "storeId": "'${STORE_ID}'"
  }'
```

---

## Test Checklist

- [ ] **Success Cases:**
  - [ ] Assign order với đầy đủ thông tin
  - [ ] Assign order không có deliveryStaffId
  - [ ] Assign order không có estimatedDeliveryDate
  - [ ] Assign order không có notes

- [ ] **Validation Errors:**
  - [ ] Missing orderId
  - [ ] Missing storeId
  - [ ] Invalid orderId format
  - [ ] Invalid storeId format (không phải UUID)
  - [ ] Invalid estimatedDeliveryDate format

- [ ] **Business Logic Errors:**
  - [ ] Order not found
  - [ ] Store not found
  - [ ] Order already assigned

- [ ] **Security:**
  - [ ] Unauthorized (no token)
  - [ ] Forbidden (DELIVERY role - không có quyền assign)
  - [ ] Forbidden (CUSTOMER role)
  - [ ] Invalid token

- [ ] **Edge Cases:**
  - [ ] Order ID = 0
  - [ ] Order ID = negative number
  - [ ] Very long notes (> 1000 characters)
  - [ ] Estimated delivery date in the past
  - [ ] Estimated delivery date too far in future (> 1 year)

---

## Postman Collection

Có thể import vào Postman để test dễ dàng hơn:

```json
{
  "info": {
    "name": "Assign Order Delivery API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Assign Order Success",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{jwt_token}}",
            "type": "text"
          },
          {
            "key": "Content-Type",
            "value": "application/json",
            "type": "text"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"orderId\": {{order_id}},\n  \"storeId\": \"{{store_id}}\",\n  \"deliveryStaffId\": \"{{delivery_staff_id}}\",\n  \"estimatedDeliveryDate\": \"2025-11-15T10:00:00\",\n  \"notes\": \"Test assignment\"\n}"
        },
        "url": {
          "raw": "{{base_url}}/api/delivery/assign",
          "host": ["{{base_url}}"],
          "path": ["api", "delivery", "assign"]
        }
      }
    }
  ]
}
```

---

## Notes

1. **JWT Token:** Cần refresh token nếu hết hạn
2. **Order Status:** Đảm bảo order ở trạng thái có thể assign (ví dụ: CONFIRMED, PAID)
3. **Store Status:** Đảm bảo store đang ACTIVE
4. **Cleanup:** Sau khi test, có thể cần xóa assignment để test lại
5. **Concurrent Testing:** Test với nhiều request đồng thời để kiểm tra race condition

