# Luồng Hoạt Động: Assign Order Delivery - Giải Thích Chi Tiết

## Tổng Quan

Luồng này mô tả quy trình từ khi order được tạo đến khi giao hàng thành công, bao gồm các bước phân công, chuẩn bị, và giao hàng.

---

## Sơ Đồ Luồng Tổng Quan

```
┌─────────────────┐
│  Order Created  │
│  (CONFIRMED)    │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│  STAFF/BRANCH_MANAGER       │
│  Assign Order to Delivery   │
│  POST /api/delivery/assign  │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│  DeliveryAssignment Created │
│  Status: ASSIGNED           │
│  invoiceGenerated: false    │
│  productsPrepared: false    │
└────────┬────────────────────┘
         │
         ├──────────────────────┐
         │                      │
         ▼                      ▼
┌──────────────────┐  ┌──────────────────────┐
│  Generate Invoice│  │  Prepare Products    │
│  (STAFF only)    │  │  (STAFF only)        │
└────────┬─────────┘  └──────────┬───────────┘
         │                        │
         │                        │
         └──────────┬─────────────┘
                    │
                    ▼
         ┌──────────────────────┐
         │  Status: READY       │
         │  invoiceGenerated:   │
         │    true              │
         │  productsPrepared:   │
         │    true              │
         └──────────┬───────────┘
                    │
                    ▼
         ┌──────────────────────┐
         │  DELIVERY Staff      │
         │  Update Status       │
         │  IN_TRANSIT          │
         └──────────┬───────────┘
                    │
                    ▼
         ┌──────────────────────┐
         │  DELIVERY Staff      │
         │  Confirm Delivery    │
         │  Status: DELIVERED   │
         └──────────────────────┘
```

---

## Chi Tiết Từng Bước

### Bước 1: Order Được Tạo (Order Service)

**Trạng thái:** Order được tạo với status `CONFIRMED` hoặc `PAID`

**Actor:** Customer (thông qua Order Service)

**API:** `POST /api/orders` (Order Service)

**Kết quả:**
- Order được lưu vào database
- Order có status phù hợp để có thể assign cho delivery

---

### Bước 2: Assign Order to Delivery (Delivery Service)

**Actor:** STAFF hoặc BRANCH_MANAGER

**API:** `POST /api/delivery/assign`

**Request:**
```json
{
  "orderId": 1,
  "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
  "deliveryStaffId": "ef5ec40c-198f-4dfb-84dc-5bf86db68940",  // Optional
  "estimatedDeliveryDate": "2025-11-15T10:00:00",              // Optional
  "notes": "Giao hàng vào buổi sáng"                           // Optional
}
```

**Quy trình xử lý:**

1. **Validation:**
   - ✅ Kiểm tra order đã được assign chưa (nếu có → throw error)
   - ✅ Verify order tồn tại (gọi Order Service)
   - ✅ Verify store tồn tại (gọi User Service)
   - ✅ Lấy thông tin user hiện tại (từ JWT token)

2. **Tạo DeliveryAssignment:**
   ```java
   DeliveryAssignment assignment = DeliveryAssignment.builder()
       .orderId(request.getOrderId())
       .storeId(request.getStoreId())
       .deliveryStaffId(request.getDeliveryStaffId())  // Có thể null
       .assignedBy(authentication.getName())            // Email của STAFF/BRANCH_MANAGER
       .assignedAt(LocalDateTime.now())
       .estimatedDeliveryDate(request.getEstimatedDeliveryDate())
       .status(DeliveryStatus.ASSIGNED)                 // Mặc định
       .notes(request.getNotes())
       .invoiceGenerated(false)                         // Mặc định
       .productsPrepared(false)                         // Mặc định
       .build();
   ```

3. **Lưu vào database:**
   - Tạo record mới trong bảng `delivery_assignments`
   - Status = `ASSIGNED`
   - `invoiceGenerated` = `false`
   - `productsPrepared` = `false`

**Response:**
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
    "notes": "Giao hàng vào buổi sáng",
    "invoiceGenerated": false,
    "productsPrepared": false
  }
}
```

**Lưu ý:**
- `deliveryStaffId` có thể là `null` nếu chưa xác định được nhân viên giao hàng
- Có thể assign sau bằng cách update assignment

---

### Bước 3: Generate Invoice (Optional - Có thể làm song song với Bước 4)

**Actor:** STAFF (chỉ STAFF, không phải BRANCH_MANAGER)

**API:** `POST /api/delivery/generate-invoice/{orderId}`

**Quy trình xử lý:**

1. **Validation:**
   - ✅ Tìm DeliveryAssignment theo orderId
   - ✅ Kiểm tra invoice đã được generate chưa (nếu có → throw error)

2. **Update assignment:**
   ```java
   assignment.setInvoiceGenerated(true);
   assignment.setInvoiceGeneratedAt(LocalDateTime.now());
   ```

3. **Lưu vào database:**
   - `invoiceGenerated` = `true`
   - `invoiceGeneratedAt` = thời gian hiện tại
   - Status vẫn giữ nguyên `ASSIGNED`

**Response:**
```json
{
  "status": 200,
  "message": "Invoice generated successfully",
  "data": {
    "id": 1,
    "orderId": 1,
    "status": "ASSIGNED",
    "invoiceGenerated": true,
    "invoiceGeneratedAt": "2025-11-10T08:35:00",
    "productsPrepared": false
  }
}
```

**Lưu ý:**
- Có thể generate invoice trước hoặc sau khi prepare products
- Không ảnh hưởng đến status của assignment

---

### Bước 4: Prepare Products (Optional - Có thể làm song song với Bước 3)

**Actor:** STAFF (chỉ STAFF, không phải BRANCH_MANAGER)

**API:** `POST /api/delivery/prepare-products`

**Request:**
```json
{
  "orderId": 1
}
```

**Quy trình xử lý:**

1. **Validation:**
   - ✅ Tìm DeliveryAssignment theo orderId
   - ✅ Kiểm tra products đã được prepare chưa (nếu có → throw error)
   - ✅ Verify order tồn tại và lấy order details

2. **Kiểm tra stock:**
   ```java
   // Với mỗi sản phẩm trong order
   for (OrderDetailResponse detail : order.getOrderDetails()) {
       // Gọi Inventory Service để kiểm tra stock
       int availableStock = inventoryClient.getTotalAvailableStock(detail.getProductColorId());
       
       // Nếu không đủ stock → throw error
       if (availableStock < detail.getQuantity()) {
           throw new AppException(ErrorCode.INVALID_REQUEST);
       }
   }
   ```

3. **Update assignment:**
   ```java
   assignment.setProductsPrepared(true);
   assignment.setProductsPreparedAt(LocalDateTime.now());
   assignment.setStatus(DeliveryStatus.READY);  // Tự động chuyển sang READY
   ```

4. **Lưu vào database:**
   - `productsPrepared` = `true`
   - `productsPreparedAt` = thời gian hiện tại
   - Status = `READY` (tự động chuyển)

**Response:**
```json
{
  "status": 200,
  "message": "Products prepared successfully",
  "data": {
    "id": 1,
    "orderId": 1,
    "status": "READY",
    "invoiceGenerated": true,
    "productsPrepared": true,
    "productsPreparedAt": "2025-11-10T08:40:00"
  }
}
```

**Lưu ý:**
- Sau khi prepare products, status tự động chuyển sang `READY`
- Cần đảm bảo đủ stock trước khi prepare
- Có thể prepare products trước hoặc sau khi generate invoice

---

### Bước 5: Delivery Staff Xem Assignments

**Actor:** DELIVERY Staff

**API:** `GET /api/delivery/assignments/staff/{deliveryStaffId}`

**Quy trình:**
- DELIVERY staff đăng nhập và xem danh sách assignments được giao cho mình
- Có thể filter theo status (ASSIGNED, READY, IN_TRANSIT, etc.)

**Response:**
```json
{
  "status": 200,
  "message": "Delivery assignments retrieved successfully",
  "data": [
    {
      "id": 1,
      "orderId": 1,
      "status": "READY",
      "estimatedDeliveryDate": "2025-11-15T10:00:00",
      "notes": "Giao hàng vào buổi sáng",
      "invoiceGenerated": true,
      "productsPrepared": true
    }
  ]
}
```

---

### Bước 6: Delivery Staff Bắt Đầu Giao Hàng

**Actor:** DELIVERY Staff hoặc BRANCH_MANAGER

**API:** `PUT /api/delivery/assignments/{assignmentId}/status?status=IN_TRANSIT`

**Quy trình xử lý:**

1. **Validation:**
   - ✅ Tìm DeliveryAssignment theo assignmentId
   - ✅ Verify status hợp lệ (phải là một trong các giá trị của DeliveryStatus enum)

2. **Update status:**
   ```java
   DeliveryStatus deliveryStatus = DeliveryStatus.valueOf(status.toUpperCase());
   assignment.setStatus(deliveryStatus);
   ```

3. **Lưu vào database:**
   - Status = `IN_TRANSIT`

**Response:**
```json
{
  "status": 200,
  "message": "Delivery status updated successfully",
  "data": {
    "id": 1,
    "orderId": 1,
    "status": "IN_TRANSIT",
    "invoiceGenerated": true,
    "productsPrepared": true
  }
}
```

**Lưu ý:**
- DELIVERY staff có thể update status từ `READY` → `IN_TRANSIT`
- BRANCH_MANAGER cũng có thể update status để theo dõi

---

### Bước 7: Delivery Staff Xác Nhận Giao Hàng

**Actor:** DELIVERY Staff

**API:** `PUT /api/delivery/assignments/{assignmentId}/status?status=DELIVERED`

**Quy trình:**
- Tương tự Bước 6, nhưng status = `DELIVERED`

**Response:**
```json
{
  "status": 200,
  "message": "Delivery status updated successfully",
  "data": {
    "id": 1,
    "orderId": 1,
    "status": "DELIVERED",
    "invoiceGenerated": true,
    "productsPrepared": true
  }
}
```

**Lưu ý:**
- Sau khi status = `DELIVERED`, order được coi là đã giao hàng thành công
- Có thể có thêm endpoint để confirm delivery với QR code hoặc signature

---

## Các Trạng Thái (DeliveryStatus)

| Status | Mô tả | Ai có thể set | Điều kiện |
|--------|-------|---------------|-----------|
| `ASSIGNED` | Đã được assign cho delivery | Hệ thống (tự động) | Khi tạo assignment |
| `PREPARING` | Đang chuẩn bị sản phẩm | DELIVERY/BRANCH_MANAGER | Có thể set thủ công |
| `READY` | Sẵn sàng giao hàng | Hệ thống (tự động) | Sau khi prepare products |
| `IN_TRANSIT` | Đang giao hàng | DELIVERY/BRANCH_MANAGER | Khi bắt đầu giao hàng |
| `DELIVERED` | Đã giao hàng | DELIVERY/BRANCH_MANAGER | Khi giao hàng thành công |
| `CANCELLED` | Đã hủy | DELIVERY/BRANCH_MANAGER | Khi hủy giao hàng |

---

## Luồng Trạng Thái (State Machine)

```
ASSIGNED ──┐
           │
           ├──> PREPARING ──> READY ──> IN_TRANSIT ──> DELIVERED
           │
           └──> CANCELLED
```

**Quy tắc chuyển trạng thái:**
- `ASSIGNED` → `READY`: Tự động khi prepare products
- `ASSIGNED` → `PREPARING`: Có thể set thủ công
- `PREPARING` → `READY`: Có thể set thủ công
- `READY` → `IN_TRANSIT`: DELIVERY staff bắt đầu giao hàng
- `IN_TRANSIT` → `DELIVERED`: Giao hàng thành công
- Bất kỳ trạng thái nào → `CANCELLED`: Hủy giao hàng

---

## Phân Quyền Theo Role

### STAFF
- ✅ Assign order to delivery
- ✅ Generate invoice
- ✅ Prepare products
- ✅ Xem assignments trong store
- ❌ Update delivery status (chỉ BRANCH_MANAGER và DELIVERY)

### BRANCH_MANAGER
- ✅ Assign order to delivery
- ✅ Xem assignments trong store
- ✅ Monitor delivery progress
- ✅ Update delivery status
- ❌ Generate invoice (chỉ STAFF)
- ❌ Prepare products (chỉ STAFF)

### DELIVERY
- ✅ Xem assignments của mình
- ✅ Update delivery status
- ✅ Xác nhận giao hàng
- ❌ Assign order (chỉ STAFF/BRANCH_MANAGER)
- ❌ Generate invoice
- ❌ Prepare products

---

## Các Trường Hợp Đặc Biệt

### 1. Assign Order Không Có Delivery Staff
- `deliveryStaffId` có thể là `null`
- Assignment vẫn được tạo với status `ASSIGNED`
- Có thể assign delivery staff sau bằng cách update assignment

### 2. Prepare Products Trước Generate Invoice
- Có thể prepare products trước khi generate invoice
- Không có thứ tự bắt buộc giữa 2 bước này

### 3. Không Đủ Stock Khi Prepare Products
- Hệ thống sẽ throw error
- Assignment vẫn ở status `ASSIGNED`
- Cần kiểm tra và cập nhật stock trước khi prepare lại

### 4. Hủy Assignment
- Có thể set status = `CANCELLED` ở bất kỳ thời điểm nào
- Assignment bị đánh dấu là cancelled, không thể tiếp tục

---

## Ví Dụ Luồng Hoàn Chỉnh

### Timeline Example:

```
08:30 - STAFF assign order #1 → Status: ASSIGNED
08:35 - STAFF generate invoice → invoiceGenerated: true
08:40 - STAFF prepare products → Status: READY, productsPrepared: true
09:00 - DELIVERY staff xem assignments → Thấy order #1 ở status READY
09:15 - DELIVERY staff update status → Status: IN_TRANSIT
10:30 - DELIVERY staff giao hàng thành công → Status: DELIVERED
```

---

## API Endpoints Summary

| Endpoint | Method | Role | Mô tả |
|----------|--------|------|-------|
| `/api/delivery/assign` | POST | STAFF/BRANCH_MANAGER | Assign order to delivery |
| `/api/delivery/generate-invoice/{orderId}` | POST | STAFF | Generate invoice |
| `/api/delivery/prepare-products` | POST | STAFF | Prepare products |
| `/api/delivery/assignments/store/{storeId}` | GET | STAFF/BRANCH_MANAGER | Xem assignments trong store |
| `/api/delivery/assignments/staff/{deliveryStaffId}` | GET | DELIVERY | Xem assignments của mình |
| `/api/delivery/assignments/{assignmentId}/status` | PUT | DELIVERY/BRANCH_MANAGER | Update delivery status |
| `/api/delivery/progress/store/{storeId}` | GET | BRANCH_MANAGER | Monitor delivery progress |

---

## Database Schema

### delivery_assignments Table

```sql
CREATE TABLE delivery_assignments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    store_id VARCHAR(255) NOT NULL,
    delivery_staff_id VARCHAR(255),
    assigned_by VARCHAR(255) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    estimated_delivery_date TIMESTAMP,
    status VARCHAR(255) NOT NULL CHECK (status IN ('ASSIGNED','PREPARING','READY','IN_TRANSIT','DELIVERED','CANCELLED')),
    notes TEXT,
    invoice_generated BOOLEAN NOT NULL DEFAULT FALSE,
    invoice_generated_at TIMESTAMP,
    products_prepared BOOLEAN NOT NULL DEFAULT FALSE,
    products_prepared_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
```

---

## Notes

1. **Soft Delete:** Tất cả assignments sử dụng soft delete (`is_deleted`)
2. **Timestamps:** Tất cả các bước đều có timestamp để tracking
3. **Validation:** Mỗi bước đều có validation để đảm bảo data integrity
4. **Error Handling:** Tất cả các lỗi đều được handle và trả về error code phù hợp
5. **Concurrent Updates:** Cần xử lý race condition khi nhiều user update cùng lúc

