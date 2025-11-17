# Tổng Hợp Các API Đã Sử Dụng

## 📋 Tổng Quan

Tài liệu này liệt kê tất cả các API endpoints đã được sử dụng trong dự án FurniMart-BE, bao gồm:
- API gọi giữa các microservices (Feign Clients)
- API endpoints chính của từng service

---

## 🔄 API Gọi Giữa Các Services (Feign Clients)

### 1. Delivery Service → Order Service

**OrderClient** (`delivery-service` → `order-service`):
- ✅ `GET /api/orders/{id}` - Lấy thông tin đơn hàng theo ID
- ✅ `PUT /api/orders/status/{id}?status={status}` - Cập nhật trạng thái đơn hàng (SHIPPING, DELIVERED, FINISHED)

**Sử dụng trong:**
- `DeliveryServiceImpl.assignOrderToDelivery()` - Lấy thông tin order và cập nhật status thành SHIPPING
- `DeliveryServiceImpl.prepareProducts()` - Lấy thông tin order để kiểm tra stock
- `DeliveryConfirmationServiceImpl` - Cập nhật status thành DELIVERED và FINISHED

---

### 2. Delivery Service → User Service (Store)

**StoreClient** (`delivery-service` → `user-service`):
- ✅ `GET /api/stores/{id}` - Lấy thông tin cửa hàng theo ID
- ✅ `GET /api/stores` - Lấy danh sách tất cả cửa hàng
- ✅ `GET /api/stores/nearest/list?lat={lat}&lon={lon}&limit={limit}` - Lấy danh sách cửa hàng gần nhất

**Sử dụng trong:**
- `DeliveryServiceImpl.assignOrderToDelivery()` - Verify store exists
- `DeliveryServiceImpl.getStoreBranchInfo()` - Lấy thông tin cửa hàng
- `DeliveryServiceImpl.getDeliveryProgressByStore()` - Verify store exists

---

### 3. Delivery Service → Inventory Service

**InventoryClient** (`delivery-service` → `inventory-service`):
- ✅ `GET /api/inventory/product/{productColorId}` - Lấy thông tin inventory theo product color
- ✅ `GET /api/inventories/stock/total-available?productColorId={id}` - Lấy tổng số stock khả dụng

**Sử dụng trong:**
- `DeliveryServiceImpl.prepareProducts()` - Kiểm tra stock availability trước khi prepare products

---

### 4. Order Service → User Service

**UserClient** (`order-service` → `user-service`):
- ✅ `GET /api/addresses/{id}` - Lấy thông tin địa chỉ theo ID
- ✅ `GET /api/users/{id}` - Lấy thông tin user theo ID
- ✅ `GET /api/users/account/{accountId}` - Lấy thông tin user theo account ID

**Sử dụng trong:**
- `OrderServiceImpl` - Lấy thông tin user và address khi tạo order

**StoreClient** (`order-service` → `user-service`):
- ✅ `GET /api/stores/{id}` - Lấy thông tin cửa hàng
- ✅ `GET /api/stores/nearest/list?lat={lat}&lon={lon}&limit={limit}` - Lấy cửa hàng gần nhất

**Sử dụng trong:**
- `AssignOrderServiceImpl.assignOrderToStore()` - Tìm cửa hàng gần nhất để assign order

---

### 5. Order Service → Inventory Service

**InventoryClient** (`order-service` → `inventory-service`):
- ✅ `GET /api/inventories/product/{productId}` - Lấy inventory theo product ID
- ✅ `GET /api/inventories/stock/check-global?productColorId={id}&requiredQty={qty}` - Kiểm tra stock đủ không
- ✅ `GET /api/inventories/stock/total-available?productColorId={id}` - Lấy tổng stock khả dụng

**Sử dụng trong:**
- `OrderServiceImpl` - Kiểm tra stock khi tạo order

---

### 6. Order Service → Product Service

**ProductClient** (`order-service` → `product-service`):
- ✅ `GET /api/products/{id}` - Lấy thông tin sản phẩm
- ✅ `GET /api/product-colors/{id}` - Lấy thông tin product color

**Sử dụng trong:**
- `OrderServiceImpl` - Lấy thông tin sản phẩm khi tạo order

---

### 7. Notification Service → Order Service

**OrderClient** (`notification-service` → `order-service`):
- ✅ `GET /api/orders/{id}` - Lấy thông tin đơn hàng

**Sử dụng trong:**
- Notification service - Lấy thông tin order để gửi notification

---

### 8. Notification Service → User Service

**UserClient** (`notification-service` → `user-service`):
- ✅ `GET /api/users/{id}` - Lấy thông tin user
- ✅ `GET /api/users/account/{accountId}` - Lấy thông tin user theo account ID

**Sử dụng trong:**
- Notification service - Lấy thông tin user để gửi notification

---

## 🎯 API Endpoints Chính Đã Sử Dụng

### Delivery Service APIs

**Base URL**: `http://152.53.227.115:8089/api/delivery`

#### 1. Assign Order to Delivery
- ✅ `POST /api/delivery/assign`
- **Vai trò**: STAFF, BRANCH_MANAGER
- **Chức năng**: Phân công đơn hàng cho delivery staff
- **Đã sử dụng**: ✅ (Đã test và fix bug)

#### 2. Get Delivery Assignment by Order ID
- ✅ `GET /api/delivery/assignments/order/{orderId}`
- **Vai trò**: STAFF, BRANCH_MANAGER
- **Chức năng**: Kiểm tra đơn hàng đã được assign chưa
- **Đã sử dụng**: ✅ (Đã test)

#### 3. Get Delivery Assignments by Store
- ✅ `GET /api/delivery/assignments/store/{storeId}`
- **Vai trò**: STAFF, BRANCH_MANAGER
- **Chức năng**: Lấy danh sách assignments của một cửa hàng
- **Đã sử dụng**: ✅

#### 4. Generate Invoice
- ✅ `POST /api/delivery/generate-invoice/{orderId}`
- **Vai trò**: STAFF
- **Chức năng**: Tạo hóa đơn cho đơn hàng
- **Đã sử dụng**: ✅

#### 5. Prepare Products
- ✅ `POST /api/delivery/prepare-products`
- **Vai trò**: STAFF
- **Chức năng**: Chuẩn bị sản phẩm cho delivery
- **Đã sử dụng**: ✅

#### 6. Get Delivery Progress
- ✅ `GET /api/delivery/progress/store/{storeId}`
- **Vai trò**: BRANCH_MANAGER
- **Chức năng**: Theo dõi tiến độ delivery của cửa hàng
- **Đã sử dụng**: ✅

#### 7. Update Delivery Status
- ✅ `PUT /api/delivery/assignments/{assignmentId}/status?status={status}`
- **Vai trò**: BRANCH_MANAGER, DELIVERY
- **Chức năng**: Cập nhật trạng thái delivery
- **Đã sử dụng**: ✅

#### 8. Get Store Branch Info
- ✅ `GET /api/delivery/stores/{storeId}/branch-info`
- **Vai trò**: Public (Guest)
- **Chức năng**: Lấy thông tin cửa hàng và stock
- **Đã sử dụng**: ✅

---

### Order Service APIs

**Base URL**: `http://152.53.227.115:8088/api/orders`

#### 1. Create Order
- ✅ `POST /api/orders`
- **Chức năng**: Tạo đơn hàng mới
- **Đã sử dụng**: ✅

#### 2. Get Order by ID
- ✅ `GET /api/orders/{id}`
- **Chức năng**: Lấy thông tin đơn hàng
- **Đã sử dụng**: ✅ (Được gọi từ delivery-service)

#### 3. Update Order Status
- ✅ `PUT /api/orders/status/{id}?status={status}`
- **Chức năng**: Cập nhật trạng thái đơn hàng
- **Đã sử dụng**: ✅ (Được gọi từ delivery-service khi assign)

#### 4. Get Orders by Store
- ✅ `GET /api/orders/store/{storeId}`
- **Chức năng**: Lấy danh sách đơn hàng của cửa hàng
- **Đã sử dụng**: ✅

---

### User Service APIs

**Base URL**: `http://152.53.227.115:8081/api`

#### 1. Get User by ID
- ✅ `GET /api/users/{id}`
- **Chức năng**: Lấy thông tin user
- **Đã sử dụng**: ✅ (Được gọi từ order-service, notification-service)

#### 2. Get Address by ID
- ✅ `GET /api/addresses/{id}`
- **Chức năng**: Lấy thông tin địa chỉ
- **Đã sử dụng**: ✅ (Được gọi từ order-service)

#### 3. Get Store by ID
- ✅ `GET /api/stores/{id}`
- **Chức năng**: Lấy thông tin cửa hàng
- **Đã sử dụng**: ✅ (Được gọi từ delivery-service, order-service)

#### 4. Get Nearest Stores
- ✅ `GET /api/stores/nearest/list?lat={lat}&lon={lon}&limit={limit}`
- **Chức năng**: Lấy danh sách cửa hàng gần nhất
- **Đã sử dụng**: ✅ (Được gọi từ delivery-service, order-service)

---

### Inventory Service APIs

**Base URL**: `http://152.53.227.115:8082/api/inventories`

#### 1. Get Inventory by Product
- ✅ `GET /api/inventories/product/{productId}`
- **Chức năng**: Lấy thông tin inventory theo product
- **Đã sử dụng**: ✅ (Được gọi từ order-service)

#### 2. Check Global Stock
- ✅ `GET /api/inventories/stock/check-global?productColorId={id}&requiredQty={qty}`
- **Chức năng**: Kiểm tra stock đủ không
- **Đã sử dụng**: ✅ (Được gọi từ order-service)

#### 3. Get Total Available Stock
- ✅ `GET /api/inventories/stock/total-available?productColorId={id}`
- **Chức năng**: Lấy tổng stock khả dụng
- **Đã sử dụng**: ✅ (Được gọi từ delivery-service, order-service)

---

## 📊 Thống Kê

### Theo Service

| Service | Số API Endpoints | Số Feign Client Calls |
|---------|------------------|----------------------|
| Delivery Service | 8+ | 3 (Order, Store, Inventory) |
| Order Service | 4+ | 4 (User, Store, Inventory, Product) |
| User Service | 4+ | 0 (Không gọi service khác) |
| Inventory Service | 3+ | 1 (User) |
| Notification Service | N/A | 2 (Order, User) |

### Theo Loại API

| Loại | Số lượng |
|------|----------|
| GET | 15+ |
| POST | 5+ |
| PUT | 2+ |
| DELETE | 0 |

---

## 🔍 API Đã Test Thực Tế

### Delivery Service
- ✅ `POST /api/delivery/assign` - Đã test và fix bug (cập nhật order status)
- ✅ `GET /api/delivery/assignments/order/{orderId}` - Đã test

### Order Service
- ✅ `GET /api/orders/{id}` - Đã test (qua delivery-service)
- ✅ `PUT /api/orders/status/{id}` - Đã test (qua delivery-service khi assign)

---

## 📝 Ghi Chú

1. **API Gateway**: Tất cả các API đều đi qua API Gateway tại port 8080
2. **Authentication**: Hầu hết các API yêu cầu JWT token (Bearer token)
3. **Service Discovery**: Sử dụng Eureka Server để service discovery
4. **Feign Clients**: Sử dụng OpenFeign để gọi API giữa các services

---

**Ngày cập nhật**: 2025-11-14
**Version**: 1.0

