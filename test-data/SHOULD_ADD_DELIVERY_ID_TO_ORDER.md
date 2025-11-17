# Có Nên Thêm deliveryId Vào Order Entity Không?

## 📋 Câu Hỏi

Trong kiến trúc microservices hiện tại, Order Entity **KHÔNG có** field `deliveryId` (hoặc `deliveryStaffId`). Thông tin delivery được lưu trong bảng `delivery_assignments` riêng biệt (delivery-service).

**Câu hỏi:** Có nên thêm `deliveryId` vào Order Entity để dễ query và truy cập không?

---

## 🔍 Phân Tích Kiến Trúc Hiện Tại

### **Hiện Tại (Microservices Architecture):**

```
┌─────────────────┐         ┌──────────────────┐
│  Order Service  │         │ Delivery Service │
│                 │         │                  │
│  Order Entity   │         │ DeliveryAssignment│
│  - id           │         │  - id            │
│  - userId       │         │  - orderId       │
│  - storeId      │         │  - deliveryStaffId│
│  - total        │         │  - status        │
│  - status       │         │  - assignedAt    │
│  - ...          │         │  - ...           │
└─────────────────┘         └──────────────────┘
        │                            │
        └──────────┬──────────────────┘
                   │
            Feign Client
        (Inter-service call)
```

**Để lấy thông tin delivery từ Order:**
```java
// 1. Lấy orderId từ Order
Long orderId = order.getId();

// 2. Gọi delivery-service
DeliveryAssignment assignment = deliveryClient.getAssignmentByOrderId(orderId);

// 3. Lấy deliveryStaffId
String deliveryStaffId = assignment.getDeliveryStaffId();
```

---

## ✅ Ưu Điểm Của Việc THÊM deliveryId Vào Order

### **1. Dễ Query và Truy Cập**

**Hiện tại:**
```java
// Phải gọi 2 services
Order order = orderRepository.findById(orderId);
DeliveryAssignment assignment = deliveryClient.getAssignmentByOrderId(orderId);
String deliveryStaffId = assignment.getDeliveryStaffId();
```

**Nếu có deliveryId trong Order:**
```java
// Chỉ cần query 1 lần
Order order = orderRepository.findById(orderId);
String deliveryStaffId = order.getDeliveryStaffId(); // Trực tiếp
```

**Lợi ích:**
- ✅ Giảm số lượng API calls
- ✅ Giảm latency (không cần gọi delivery-service)
- ✅ Code đơn giản hơn
- ✅ Dễ filter orders theo delivery staff

---

### **2. Performance Tốt Hơn**

**Hiện tại:**
- Mỗi lần lấy Order → Phải gọi thêm delivery-service
- Network overhead (HTTP call giữa services)
- Nếu cần lấy nhiều orders → N+1 problem

**Nếu có deliveryId:**
- Query trực tiếp từ database
- Không cần network call
- Có thể JOIN trong SQL query

**Ví dụ:**
```sql
-- Hiện tại: Phải query 2 lần
SELECT * FROM orders WHERE id = 123;
SELECT * FROM delivery_assignments WHERE order_id = 123;

-- Nếu có deliveryId: Query 1 lần
SELECT * FROM orders WHERE id = 123;
-- deliveryStaffId đã có sẵn trong Order
```

---

### **3. Dễ Filter và Search**

**Hiện tại:**
```java
// Khó filter orders theo delivery staff
// Phải query delivery-service trước, rồi mới query orders
List<DeliveryAssignment> assignments = deliveryClient.getAssignmentsByStaff(deliveryStaffId);
List<Long> orderIds = assignments.stream().map(DeliveryAssignment::getOrderId).collect(Collectors.toList());
List<Order> orders = orderRepository.findAllById(orderIds);
```

**Nếu có deliveryId:**
```java
// Dễ dàng filter
List<Order> orders = orderRepository.findByDeliveryStaffId(deliveryStaffId);
```

---

### **4. Giảm Coupling Giữa Services**

**Hiện tại:**
- Order Service phụ thuộc vào Delivery Service (phải gọi API)
- Nếu Delivery Service down → Không lấy được thông tin delivery

**Nếu có deliveryId:**
- Order Service độc lập hơn
- Không cần gọi Delivery Service để lấy deliveryId cơ bản

---

## ❌ Nhược Điểm Của Việc THÊM deliveryId Vào Order

### **1. Vi Phạm Nguyên Tắc Microservices**

**Single Responsibility Principle:**
- Order Service: Quản lý orders
- Delivery Service: Quản lý delivery assignments
- Mỗi service nên độc lập và tự chứa dữ liệu của mình

**Nếu thêm deliveryId vào Order:**
- Order Service phải biết về delivery (vi phạm separation of concerns)
- Delivery Service mất quyền sở hữu dữ liệu delivery

---

### **2. Data Duplication và Inconsistency**

**Vấn đề:**
- `deliveryStaffId` sẽ tồn tại ở 2 nơi:
  - Order Entity (order-service)
  - DeliveryAssignment Entity (delivery-service)

**Rủi ro:**
- ❌ Data có thể không đồng bộ
- ❌ Nếu update deliveryStaffId ở delivery-service → Phải update cả order-service
- ❌ Nếu update deliveryStaffId ở order-service → Phải update cả delivery-service
- ❌ Khó maintain consistency

**Ví dụ:**
```java
// Update deliveryStaffId ở delivery-service
assignment.setDeliveryStaffId(newDeliveryStaffId);
deliveryService.update(assignment);

// Nhưng Order vẫn giữ deliveryStaffId cũ!
// → Data inconsistency!
```

---

### **3. Phức Tạp Hóa Update Logic**

**Hiện tại:**
```java
// Chỉ cần update 1 nơi
DeliveryAssignment assignment = deliveryRepository.findById(assignmentId);
assignment.setDeliveryStaffId(newDeliveryStaffId);
deliveryRepository.save(assignment);
```

**Nếu có deliveryId trong Order:**
```java
// Phải update 2 nơi (hoặc dùng distributed transaction)
// 1. Update DeliveryAssignment
DeliveryAssignment assignment = deliveryRepository.findById(assignmentId);
assignment.setDeliveryStaffId(newDeliveryStaffId);
deliveryRepository.save(assignment);

// 2. Update Order (phải gọi order-service)
orderClient.updateOrderDeliveryId(orderId, newDeliveryStaffId);

// → Phức tạp hơn, dễ lỗi hơn
```

---

### **4. Không Phù Hợp Với Business Logic**

**Delivery có thể thay đổi:**
- Một order có thể được reassign cho delivery staff khác
- Một order có thể không có delivery staff ngay từ đầu (optional)
- Một order có thể có nhiều delivery attempts (nếu giao thất bại)

**Nếu deliveryId trong Order:**
- Khó handle reassignment
- Khó handle trường hợp chưa assign
- Không thể track lịch sử thay đổi delivery staff

---

### **5. Khó Scale và Maintain**

**Microservices Benefits:**
- Mỗi service có thể scale độc lập
- Mỗi service có thể deploy độc lập
- Mỗi service có thể sử dụng database khác nhau

**Nếu có deliveryId trong Order:**
- Order Service và Delivery Service phải đồng bộ với nhau
- Khó scale độc lập
- Khó maintain

---

## 🎯 Best Practices Trong Microservices

### **1. Database Per Service Pattern**

Mỗi microservice nên có database riêng:
- Order Service → `orders` database
- Delivery Service → `delivery` database

**Nếu thêm deliveryId vào Order:**
- Vi phạm pattern này
- Order Service phải biết về delivery data

---

### **2. API Composition Pattern**

Thay vì denormalize data, nên compose data từ nhiều services:

**Hiện tại (Đúng):**
```java
// Compose data từ 2 services
Order order = orderClient.getOrderById(orderId);
DeliveryAssignment assignment = deliveryClient.getAssignmentByOrderId(orderId);

OrderWithDeliveryResponse response = OrderWithDeliveryResponse.builder()
    .order(order)
    .deliveryStaffId(assignment.getDeliveryStaffId())
    .deliveryStatus(assignment.getStatus())
    .build();
```

**Nếu có deliveryId trong Order (Sai):**
- Denormalize data
- Vi phạm API Composition pattern

---

### **3. Event-Driven Architecture (Alternative)**

Thay vì thêm deliveryId vào Order, có thể dùng events:

```java
// Khi assign delivery
@EventListener
public void onDeliveryAssigned(DeliveryAssignedEvent event) {
    // Có thể cache deliveryId trong Order Service
    // Nhưng không lưu vào database
    orderCache.putDeliveryId(event.getOrderId(), event.getDeliveryStaffId());
}
```

**Lợi ích:**
- ✅ Không vi phạm microservices principles
- ✅ Vẫn có thể cache để performance tốt
- ✅ Data vẫn được quản lý bởi Delivery Service

---

## 📊 So Sánh: Có vs Không Có deliveryId

| Tiêu Chí | **KHÔNG CÓ deliveryId** (Hiện tại) | **CÓ deliveryId** |
|----------|-------------------------------------|-------------------|
| **Microservices Principles** | ✅ Đúng | ❌ Vi phạm |
| **Data Consistency** | ✅ Dễ maintain | ❌ Khó maintain |
| **Performance** | ⚠️ Cần gọi 2 services | ✅ Query nhanh hơn |
| **Code Complexity** | ⚠️ Phức tạp hơn | ✅ Đơn giản hơn |
| **Scalability** | ✅ Scale độc lập | ❌ Phụ thuộc nhau |
| **Maintainability** | ✅ Dễ maintain | ❌ Khó maintain |
| **Business Logic** | ✅ Linh hoạt | ❌ Cứng nhắc |

---

## 💡 Khuyến Nghị

### **❌ KHÔNG NÊN thêm deliveryId vào Order Entity**

**Lý do:**

1. **Vi phạm Microservices Principles**
   - Order Service và Delivery Service nên độc lập
   - Mỗi service quản lý dữ liệu của riêng mình

2. **Data Consistency**
   - Khó maintain consistency giữa 2 services
   - Rủi ro data không đồng bộ

3. **Business Logic**
   - Delivery có thể thay đổi, reassign
   - Không phù hợp với business requirements

---

### **✅ Giải Pháp Thay Thế**

#### **1. API Composition (Hiện tại - Đúng)**

```java
// Compose data từ nhiều services
Order order = orderClient.getOrderById(orderId);
DeliveryAssignment assignment = deliveryClient.getAssignmentByOrderId(orderId);

// Combine vào response
OrderWithDeliveryResponse response = OrderWithDeliveryResponse.builder()
    .order(order)
    .deliveryStaffId(assignment.getDeliveryStaffId())
    .deliveryStatus(assignment.getStatus())
    .build();
```

**Lợi ích:**
- ✅ Giữ được separation of concerns
- ✅ Dễ maintain
- ✅ Data consistency

---

#### **2. Caching (Nếu cần Performance)**

```java
// Cache deliveryId trong Order Service (không lưu vào DB)
@Cacheable("order-delivery")
public String getDeliveryStaffId(Long orderId) {
    DeliveryAssignment assignment = deliveryClient.getAssignmentByOrderId(orderId);
    return assignment.getDeliveryStaffId();
}
```

**Lợi ích:**
- ✅ Performance tốt (cache)
- ✅ Không vi phạm microservices principles
- ✅ Data vẫn được quản lý bởi Delivery Service

---

#### **3. Event-Driven Architecture**

```java
// Khi assign delivery → Publish event
@EventListener
public void onDeliveryAssigned(DeliveryAssignedEvent event) {
    // Update cache hoặc local view
    orderCache.putDeliveryId(event.getOrderId(), event.getDeliveryStaffId());
}
```

**Lợi ích:**
- ✅ Loose coupling
- ✅ Eventual consistency
- ✅ Scalable

---

#### **4. Database View (Nếu cùng database)**

Nếu Order Service và Delivery Service dùng cùng database (không khuyến nghị):

```sql
CREATE VIEW order_with_delivery AS
SELECT 
    o.*,
    da.delivery_staff_id,
    da.status as delivery_status
FROM orders o
LEFT JOIN delivery_assignments da ON o.id = da.order_id;
```

**Lưu ý:** Chỉ áp dụng nếu 2 services dùng cùng database (vi phạm microservices principles).

---

## 🎯 Kết Luận

### **Câu Trả Lời: KHÔNG NÊN thêm deliveryId vào Order Entity**

**Lý do chính:**
1. ❌ Vi phạm Microservices Principles
2. ❌ Data Consistency khó maintain
3. ❌ Phức tạp hóa update logic
4. ❌ Không phù hợp với business logic

**Giải pháp tốt nhất:**
- ✅ Giữ nguyên kiến trúc hiện tại (API Composition)
- ✅ Sử dụng caching nếu cần performance
- ✅ Cân nhắc Event-Driven Architecture nếu scale lớn

---

## 📚 Tài Liệu Tham Khảo

- [Microservices Patterns - Database Per Service](https://microservices.io/patterns/data/database-per-service.html)
- [Microservices Patterns - API Composition](https://microservices.io/patterns/data/api-composition.html)
- [Domain-Driven Design - Bounded Context](https://martinfowler.com/bliki/BoundedContext.html)


