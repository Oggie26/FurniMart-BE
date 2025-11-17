# Cách Hoạt Động Của Assign Đơn Cho Delivery

## 📋 Tổng Quan

Khi một đơn hàng (Order) được tạo thành công, hệ thống cần assign đơn đó cho delivery staff để thực hiện giao hàng. Quá trình này được quản lý bởi **Delivery Service** thông qua bảng `delivery_assignments`.

---

## 🔄 Flow Hoạt Động

### **Bước 1: Request Assign Order**

**Endpoint:** `POST /api/delivery/assign`

**Authorization:** Required (Bearer Token)

**Roles:** `STAFF` hoặc `BRANCH_MANAGER` (chỉ quản lý mới có quyền assign)

**Request Body:**
```json
{
  "orderId": 123,
  "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
  "deliveryStaffId": "880c5184-668f-4b09-b9af-99b59803918d",  // Optional
  "estimatedDeliveryDate": "2025-11-15T10:00:00",
  "notes": "Giao hàng vào buổi sáng"
}
```

**Request Fields:**
- `orderId` (Long, required): ID của đơn hàng cần assign
- `storeId` (String, required): ID của cửa hàng
- `deliveryStaffId` (String, optional): ID của delivery staff (có thể assign sau)
- `estimatedDeliveryDate` (LocalDateTime, optional): Ngày dự kiến giao hàng
- `notes` (String, optional): Ghi chú

---

### **Bước 2: Validation & Processing**

#### **2.1. Kiểm Tra Order Đã Được Assign Chưa**

```java
deliveryAssignmentRepository.findByOrderIdAndIsDeletedFalse(request.getOrderId())
    .ifPresent(assignment -> {
        throw new AppException(ErrorCode.ASSIGNMENT_ALREADY_EXISTS);
    });
```

**Logic:**
- Tìm kiếm trong bảng `delivery_assignments` xem order đã có assignment chưa
- Nếu đã có → Throw error: `ASSIGNMENT_ALREADY_EXISTS`
- Nếu chưa có → Tiếp tục

**Lý do:** Một order chỉ có thể được assign một lần để tránh conflict.

---

#### **2.2. Verify Order Exists**

```java
ResponseEntity<ApiResponse<OrderResponse>> orderResponse = orderClient.getOrderById(request.getOrderId());
if (orderResponse.getBody() == null || orderResponse.getBody().getData() == null) {
    throw new AppException(ErrorCode.CODE_NOT_FOUND);
}
```

**Logic:**
- Gọi API `order-service` để kiểm tra order có tồn tại không
- Sử dụng **Feign Client** (`OrderClient`) để giao tiếp giữa các microservices
- Nếu order không tồn tại → Throw error: `CODE_NOT_FOUND`

**Lý do:** Đảm bảo order thực sự tồn tại trước khi assign.

---

#### **2.3. Verify Store Exists**

```java
ApiResponse<StoreResponse> storeResponse = storeClient.getStoreById(request.getStoreId());
if (storeResponse == null || storeResponse.getData() == null) {
    throw new AppException(ErrorCode.CODE_NOT_FOUND);
}
```

**Logic:**
- Gọi API `user-service` để kiểm tra store có tồn tại không
- Sử dụng **Feign Client** (`StoreClient`) để giao tiếp
- Nếu store không tồn tại → Throw error: `CODE_NOT_FOUND`

**Lý do:** Đảm bảo store thực sự tồn tại và hợp lệ.

---

#### **2.4. Lấy Thông Tin Người Assign**

```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String assignedBy = authentication.getName();
```

**Logic:**
- Lấy thông tin từ **Spring Security Context**
- `assignedBy` = ID của user đang thực hiện assign (STAFF hoặc BRANCH_MANAGER)
- Lưu lại để tracking ai đã assign order này

**Lý do:** Audit trail - biết ai đã assign order.

---

### **Bước 3: Tạo Delivery Assignment**

```java
DeliveryAssignment assignment = DeliveryAssignment.builder()
    .orderId(request.getOrderId())
    .storeId(request.getStoreId())
    .deliveryStaffId(request.getDeliveryStaffId())  // Có thể null
    .assignedBy(assignedBy)
    .assignedAt(LocalDateTime.now())
    .estimatedDeliveryDate(request.getEstimatedDeliveryDate())
    .status(DeliveryStatus.ASSIGNED)  // Trạng thái ban đầu
    .notes(request.getNotes())
    .invoiceGenerated(false)  // Chưa generate invoice
    .productsPrepared(false)  // Chưa prepare products
    .build();

DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
```

**Các Field Được Tạo:**

| Field | Giá Trị | Mô Tả |
|-------|---------|-------|
| `id` | Auto-generated | ID của assignment |
| `orderId` | Từ request | ID của order được assign |
| `storeId` | Từ request | ID của store |
| `deliveryStaffId` | Từ request (optional) | ID của delivery staff (có thể null) |
| `assignedBy` | Từ authentication | ID của người assign (STAFF/BRANCH_MANAGER) |
| `assignedAt` | `LocalDateTime.now()` | Thời điểm assign |
| `estimatedDeliveryDate` | Từ request (optional) | Ngày dự kiến giao hàng |
| `status` | `ASSIGNED` | Trạng thái ban đầu |
| `notes` | Từ request (optional) | Ghi chú |
| `invoiceGenerated` | `false` | Chưa generate invoice |
| `productsPrepared` | `false` | Chưa prepare products |

---

### **Bước 4: Response**

**Success Response (201 Created):**
```json
{
  "status": 201,
  "message": "Order assigned to delivery successfully",
  "data": {
    "id": 1,
    "orderId": 123,
    "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
    "deliveryStaffId": "880c5184-668f-4b09-b9af-99b59803918d",
    "assignedBy": "6537f984-7d41-43e8-9de1-f6834caa1049",
    "assignedAt": "2025-11-10T10:00:00",
    "estimatedDeliveryDate": "2025-11-15T10:00:00",
    "status": "ASSIGNED",
    "notes": "Giao hàng vào buổi sáng",
    "invoiceGenerated": false,
    "productsPrepared": false,
    "order": {
      "id": 123,
      "userId": "customer-uuid",
      "total": 1500000.0,
      "status": "CONFIRMED",
      // ... other order fields
    },
    "store": {
      "id": "8d46e317-0596-4413-81b6-1a526398b3d7",
      "name": "FurniMart Store 1",
      "addressLine": "123 Main Street",
      // ... other store fields
    }
  }
}
```

**Response bao gồm:**
- Thông tin `DeliveryAssignment` vừa tạo
- Thông tin `Order` (lấy từ order-service)
- Thông tin `Store` (lấy từ user-service)

---

## 📊 Database Schema

### **Bảng: `delivery_assignments`**

```sql
CREATE TABLE delivery_assignments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    store_id VARCHAR(255) NOT NULL,
    delivery_staff_id VARCHAR(255),  -- NULL nếu chưa assign
    assigned_by VARCHAR(255) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    estimated_delivery_date TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ASSIGNED',
    notes TEXT,
    invoice_generated BOOLEAN NOT NULL DEFAULT FALSE,
    invoice_generated_at TIMESTAMP,
    products_prepared BOOLEAN NOT NULL DEFAULT FALSE,
    products_prepared_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

**Mối Quan Hệ:**
- `order_id` → Foreign key đến bảng `orders` (order-service)
- `store_id` → Foreign key đến bảng `stores` (user-service)
- `delivery_staff_id` → Foreign key đến bảng `employees` (user-service)
- `assigned_by` → Foreign key đến bảng `employees` (user-service)

---

## 🔐 Security & Authorization

### **Roles Có Quyền Assign:**

1. **STAFF**
   - Có thể assign order cho delivery
   - Có thể generate invoice
   - Có thể prepare products

2. **BRANCH_MANAGER**
   - Có thể assign order cho delivery
   - Có thể monitor delivery progress
   - Có thể update delivery status

### **Roles KHÔNG Có Quyền Assign:**

- **DELIVERY**: Chỉ có thể nhận assignments và cập nhật trạng thái
- **CUSTOMER**: Không có quyền truy cập
- **ADMIN**: Có thể có quyền (tùy vào implementation)

---

## 🎯 Delivery Status Flow

Sau khi assign, delivery sẽ trải qua các trạng thái sau:

```
ASSIGNED → PREPARING → READY → IN_TRANSIT → DELIVERED
                                    ↓
                               CANCELLED
```

### **Các Trạng Thái:**

1. **ASSIGNED** (Mặc định khi assign)
   - Order đã được assign cho delivery
   - Chưa có delivery staff cụ thể (nếu `deliveryStaffId` = null)

2. **PREPARING**
   - Đang chuẩn bị sản phẩm
   - Staff đang prepare products

3. **READY**
   - Sản phẩm đã được prepare xong
   - Sẵn sàng để giao hàng

4. **IN_TRANSIT**
   - Đang trên đường giao hàng
   - Delivery staff đã nhận hàng và đang đi giao

5. **DELIVERED**
   - Đã giao hàng thành công
   - Customer đã nhận hàng

6. **CANCELLED**
   - Hủy giao hàng
   - Có thể do nhiều lý do (customer hủy, không liên lạc được, etc.)

---

## 🔄 Các Bước Tiếp Theo Sau Assign

### **1. Generate Invoice (Optional)**

**Endpoint:** `POST /api/delivery/generate-invoice/{orderId}`

**Role:** `STAFF`

**Logic:**
- Tìm `DeliveryAssignment` theo `orderId`
- Kiểm tra `invoiceGenerated` = false
- Set `invoiceGenerated` = true
- Set `invoiceGeneratedAt` = now()

---

### **2. Prepare Products**

**Endpoint:** `POST /api/delivery/prepare-products`

**Role:** `STAFF`

**Logic:**
- Tìm `DeliveryAssignment` theo `orderId`
- Kiểm tra `productsPrepared` = false
- Verify order exists
- **Kiểm tra stock availability** cho từng sản phẩm trong order
- Nếu stock đủ → Set `productsPrepared` = true, `status` = `READY`
- Nếu stock không đủ → Throw error: `INSUFFICIENT_STOCK`

---

### **3. Update Delivery Status**

**Endpoint:** `PUT /api/delivery/assignments/{assignmentId}/status?status=IN_TRANSIT`

**Roles:** `BRANCH_MANAGER` hoặc `DELIVERY`

**Logic:**
- Tìm `DeliveryAssignment` theo `assignmentId`
- Validate status (phải là một trong các giá trị hợp lệ)
- Update `status` = new status
- Save và return updated assignment

---

## 🚨 Error Handling

### **Các Lỗi Có Thể Xảy Ra:**

1. **ASSIGNMENT_ALREADY_EXISTS** (400)
   - Order đã được assign rồi
   - Message: "Order đã được assign. Assignment ID: {id}, Status: {status}"

2. **CODE_NOT_FOUND** (404)
   - Order không tồn tại
   - Store không tồn tại
   - Message: "Resource not found"

3. **INSUFFICIENT_STOCK** (400)
   - Stock không đủ khi prepare products
   - Message: "Stock không đủ cho các sản phẩm sau: ..."

4. **INVOICE_ALREADY_GENERATED** (400)
   - Invoice đã được generate rồi
   - Message: "Invoice đã được generate cho order này"

5. **PRODUCTS_ALREADY_PREPARED** (400)
   - Products đã được prepare rồi
   - Message: "Products đã được prepare cho order này"

6. **INVALID_STATUS** (400)
   - Status không hợp lệ khi update
   - Message: "Invalid status"

---

## 📝 Lưu Ý Quan Trọng

### **1. Delivery Staff ID là Optional**

- Có thể assign order mà không chỉ định delivery staff cụ thể
- Có thể assign delivery staff sau bằng cách update `deliveryStaffId`

### **2. Order Không Có Delivery ID**

- **Order entity KHÔNG có field `deliveryStaffId`**
- Thông tin delivery được lưu trong bảng `delivery_assignments` riêng biệt
- Để lấy thông tin delivery từ Order, cần query `DeliveryAssignment` theo `orderId`

### **3. Microservices Architecture**

- **Order Service**: Quản lý orders (không có thông tin delivery)
- **Delivery Service**: Quản lý delivery assignments (có `orderId` và `deliveryStaffId`)
- Giao tiếp giữa các services thông qua **Feign Client**

### **4. Transaction Management**

- Sử dụng `@Transactional` để đảm bảo tính nhất quán dữ liệu
- Nếu có lỗi xảy ra, tất cả thay đổi sẽ được rollback

---

## 🔍 Ví Dụ Thực Tế

### **Scenario: Assign Order cho Delivery**

1. **STAFF đăng nhập** và lấy JWT token
2. **Gọi API assign:**
   ```bash
   POST /api/delivery/assign
   Authorization: Bearer {STAFF_JWT_TOKEN}
   Content-Type: application/json
   
   {
     "orderId": 123,
     "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
     "deliveryStaffId": "880c5184-668f-4b09-b9af-99b59803918d",
     "estimatedDeliveryDate": "2025-11-15T10:00:00",
     "notes": "Giao hàng vào buổi sáng"
   }
   ```

3. **Hệ thống thực hiện:**
   - ✅ Kiểm tra order 123 chưa được assign
   - ✅ Verify order 123 tồn tại (gọi order-service)
   - ✅ Verify store tồn tại (gọi user-service)
   - ✅ Lấy ID của STAFF từ JWT token
   - ✅ Tạo `DeliveryAssignment` với status = `ASSIGNED`

4. **Response:**
   - ✅ Trả về `DeliveryAssignmentResponse` với đầy đủ thông tin

5. **Các bước tiếp theo:**
   - STAFF có thể generate invoice
   - STAFF có thể prepare products (kiểm tra stock)
   - DELIVERY staff có thể xem assignments của mình
   - DELIVERY staff có thể update status thành `IN_TRANSIT` → `DELIVERED`

---

## 📚 Tài Liệu Liên Quan

- [Delivery Workflow Explanation](./DELIVERY_WORKFLOW_EXPLANATION.md)
- [Delivery Workflow Simple Explanation](./DELIVERY_WORKFLOW_SIMPLE_EXPLANATION.md)
- [Test Scenarios](./ASSIGN_ORDER_DELIVERY_TEST_SCENARIOS.md)


