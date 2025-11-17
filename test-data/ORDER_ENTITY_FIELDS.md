# Order Entity - Các Field

## 📋 Tổng Quan

Order entity là entity chính trong `order-service`, đại diện cho một đơn hàng trong hệ thống FurniMart.

---

## 🗂️ Cấu Trúc Entity

```java
@Entity
@Table(name = "orders")
public class Order extends AbstractEntity {
    // Các field được định nghĩa bên dưới
}
```

**Kế thừa từ:** `AbstractEntity` (có các field: `createdAt`, `updatedAt`, `isDeleted`)

---

## 📊 Danh Sách Các Field

### **1. Primary Key**

| Field | Type | Mô Tả | Constraints |
|-------|------|-------|-------------|
| `id` | `Long` | ID duy nhất của order | `@Id`, `@GeneratedValue(strategy = GenerationType.AUTO)` |

---

### **2. Thông Tin Khách Hàng & Địa Chỉ**

| Field | Type | Mô Tả | Constraints |
|-------|------|-------|-------------|
| `userId` | `String` | ID của khách hàng (CUSTOMER) | `@Column(nullable = false)` |
| `addressId` | `Long` | ID của địa chỉ giao hàng | `@Column(nullable = false)` |
| `storeId` | `String` | ID của cửa hàng (optional) | `@Column` (nullable = true) |

**Lưu ý:**
- `userId` là ID của CUSTOMER (không phải employee)
- `storeId` có thể null (chưa assign store)
- `addressId` là ID của địa chỉ giao hàng trong bảng `addresses`

---

### **3. Thông Tin Đơn Hàng**

| Field | Type | Mô Tả | Constraints |
|-------|------|-------|-------------|
| `total` | `Double` | Tổng tiền của đơn hàng | `@Column(nullable = false)` |
| `status` | `EnumProcessOrder` | Trạng thái của đơn hàng | `@Enumerated(EnumType.STRING)` |
| `orderDate` | `Date` | Ngày đặt hàng | `@Column(nullable = false)`, `@Temporal(TemporalType.TIMESTAMP)` |
| `reason` | `String` | Lý do hủy/từ chối đơn hàng | `@Column` (nullable = true) |
| `note` | `String` | Ghi chú của khách hàng | `@Column` (nullable = true) |

**EnumProcessOrder có các giá trị:**
- `PRE_ORDER` - Đơn hàng trước
- `PENDING` - Đang chờ xử lý
- `PAYMENT` - Đang thanh toán
- `ASSIGN_ORDER_STORE` - Đã assign cho store
- `MANAGER_ACCEPT` - Manager đã chấp nhận
- `MANAGER_REJECT` - Manager đã từ chối
- `CONFIRMED` - Đã xác nhận
- `PACKAGED` - Đã đóng gói
- `SHIPPING` - Đang vận chuyển
- `DELIVERED` - Đã giao hàng
- `FINISHED` - Hoàn thành
- `CANCELLED` - Đã hủy

---

### **4. QR Code**

| Field | Type | Mô Tả | Constraints |
|-------|------|-------|-------------|
| `qrCode` | `String` | Mã QR code của đơn hàng | `@Column(name = "qr_code", unique = true)` |
| `qrCodeGeneratedAt` | `Date` | Thời điểm generate QR code | `@Column(name = "qr_code_generated_at")`, `@Temporal(TemporalType.TIMESTAMP)` |

**Lưu ý:**
- `qrCode` là unique (mỗi order có một QR code duy nhất)
- Có thể null nếu chưa generate QR code

---

### **5. Relationships (JPA)**

#### **5.1. OrderDetails (One-to-Many)**

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderDetail> orderDetails;
```

**Mô tả:**
- Một Order có nhiều OrderDetail (chi tiết sản phẩm trong đơn)
- Cascade: Khi xóa Order → xóa tất cả OrderDetail
- Orphan removal: Khi remove OrderDetail khỏi list → tự động xóa khỏi DB

---

#### **5.2. ProcessOrders (One-to-Many)**

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ProcessOrder> processOrders;
```

**Mô tả:**
- Một Order có nhiều ProcessOrder (lịch sử thay đổi trạng thái)
- Cascade: Khi xóa Order → xóa tất cả ProcessOrder
- Orphan removal: Khi remove ProcessOrder khỏi list → tự động xóa khỏi DB

---

#### **5.3. Payment (One-to-One)**

```java
@OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
private Payment payment;
```

**Mô tả:**
- Một Order có một Payment (thanh toán)
- Cascade: Khi xóa Order → xóa Payment
- MappedBy: Payment là owner của relationship

---

#### **5.4. Vouchers (One-to-Many)**

```java
@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
private List<Voucher> vouchers;
```

**Mô tả:**
- Một Order có thể có nhiều Voucher (mã giảm giá)
- FetchType.LAZY: Chỉ load khi cần thiết
- Không có cascade: Voucher có thể tồn tại độc lập

---

#### **5.5. Warranties (One-to-Many)**

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Warranty> warranties;
```

**Mô tả:**
- Một Order có thể có nhiều Warranty (bảo hành)
- Cascade: Khi xóa Order → xóa tất cả Warranty
- Orphan removal: Khi remove Warranty khỏi list → tự động xóa khỏi DB

---

### **6. Fields Kế Thừa Từ AbstractEntity**

| Field | Type | Mô Tả | Constraints |
|-------|------|-------|-------------|
| `createdAt` | `Date` | Thời điểm tạo order | `@CreationTimestamp` (tự động) |
| `updatedAt` | `Date` | Thời điểm cập nhật order | `@UpdateTimestamp` (tự động) |
| `isDeleted` | `Boolean` | Đánh dấu đã xóa (soft delete) | Default: `false` |

**Lưu ý:**
- `createdAt` và `updatedAt` được tự động set bởi Hibernate
- `isDeleted` dùng cho soft delete (không xóa thật khỏi DB)

---

## 🚫 Field KHÔNG Có Trong Order

### **Delivery-Related Fields:**

Order entity **KHÔNG có** các field sau:
- ❌ `deliveryStaffId` - ID của delivery staff
- ❌ `deliveryStatus` - Trạng thái delivery
- ❌ `assignedAt` - Thời điểm assign cho delivery
- ❌ `estimatedDeliveryDate` - Ngày dự kiến giao hàng

**Lý do:**
- Thông tin delivery được lưu trong bảng `delivery_assignments` (delivery-service)
- Kiến trúc microservices: Order và Delivery là 2 service riêng biệt
- Để lấy thông tin delivery → Query `DeliveryAssignment` theo `orderId`

---

## 📋 Database Schema

```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    store_id VARCHAR(255),
    address_id BIGINT NOT NULL,
    total DOUBLE PRECISION NOT NULL,
    status VARCHAR(50),
    reason VARCHAR(255),
    note VARCHAR(255),
    order_date TIMESTAMP NOT NULL,
    qr_code VARCHAR(255) UNIQUE,
    qr_code_generated_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Foreign Keys (không có trong DB, chỉ là reference)
    -- user_id → users.id (user-service)
    -- store_id → stores.id (user-service)
    -- address_id → addresses.id (order-service hoặc user-service)
);
```

---

## 📤 OrderResponse DTO

Khi trả về API, Order được map sang `OrderResponse`:

```java
public class OrderResponse {
    private Long id;
    private UserResponse user;              // Thông tin user (expanded)
    private String storeId;
    private AddressResponse address;        // Thông tin địa chỉ (expanded)
    private Double total;
    private String note;
    private Date orderDate;
    private EnumProcessOrder status;
    private String reason;
    private List<OrderDetailResponse> orderDetails;  // Chi tiết sản phẩm
    private List<ProcessOrderResponse> processOrders; // Lịch sử trạng thái
    private PaymentResponse payment;        // Thông tin thanh toán
    private String qrCode;
    private Date qrCodeGeneratedAt;
}
```

**Khác biệt với Entity:**
- `userId` → `user` (UserResponse object)
- `addressId` → `address` (AddressResponse object)
- Các relationship được expand thành DTO objects

---

## 🔍 Ví Dụ Sử Dụng

### **Tạo Order:**

```java
Order order = Order.builder()
    .userId("customer-uuid")
    .storeId("store-uuid")
    .addressId(1L)
    .total(1500000.0)
    .status(EnumProcessOrder.PENDING)
    .orderDate(new Date())
    .note("Giao hàng vào buổi sáng")
    .build();

orderRepository.save(order);
```

### **Query Order:**

```java
// Tìm order theo ID
Optional<Order> order = orderRepository.findById(orderId);

// Tìm orders của một customer
List<Order> orders = orderRepository.findByUserIdAndIsDeletedFalse(userId);

// Tìm orders của một store
List<Order> orders = orderRepository.findByStoreIdAndIsDeletedFalse(storeId);
```

### **Lấy Thông Tin Delivery Từ Order:**

```java
// Order không có deliveryStaffId
// Cần gọi delivery-service để lấy thông tin

// 1. Lấy orderId từ Order
Long orderId = order.getId();

// 2. Gọi delivery-service
DeliveryAssignment assignment = deliveryClient.getAssignmentByOrderId(orderId);

// 3. Lấy deliveryStaffId từ assignment
String deliveryStaffId = assignment.getDeliveryStaffId();
```

---

## 📝 Lưu Ý Quan Trọng

1. **Soft Delete:**
   - Order không bị xóa thật khỏi DB
   - Chỉ set `isDeleted = true`
   - Luôn query với điều kiện `isDeleted = false`

2. **Relationships:**
   - Các relationship được load LAZY (trừ khi chỉ định EAGER)
   - Cần chú ý LazyInitializationException khi truy cập ngoài transaction

3. **Status Flow:**
   - Status của Order khác với DeliveryStatus
   - Order status: `PENDING` → `CONFIRMED` → `SHIPPING` → `DELIVERED`
   - Delivery status: `ASSIGNED` → `PREPARING` → `READY` → `IN_TRANSIT` → `DELIVERED`

4. **Microservices:**
   - Order không có thông tin delivery
   - Cần gọi delivery-service để lấy thông tin delivery
   - Sử dụng Feign Client để giao tiếp giữa các services

---

## 📚 Tài Liệu Liên Quan

- [Assign Order Delivery Flow](./ASSIGN_ORDER_DELIVERY_FLOW.md)
- [Delivery Workflow Explanation](./DELIVERY_WORKFLOW_EXPLANATION.md)


