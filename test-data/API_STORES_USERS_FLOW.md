# Cách hoạt động của `/api/stores/users` - Flow đầy đủ

## Tổng quan

**Endpoint:** `POST /api/stores/users`  
**Mục đích:** Thêm một Employee vào một Store (tạo quan hệ many-to-many)  
**Quyền truy cập:** Cần authentication token (Bearer token)

---

## Flow hoạt động từ Client đến Database

```
Client
  │
  ├─ POST http://152.53.227.115:8080/api/stores/users
  │  └─ Headers: Authorization: Bearer <token>
  │  └─ Body: { "employeeId": "...", "storeId": "..." }
  │
  ▼
API Gateway (Port 8080)
  │
  ├─ Route matching: /api/stores/** → user-service
  ├─ Load balancing: lb://user-service
  ├─ Service discovery: Tìm user-service trong Eureka
  │
  ▼
User Service (Port 8086)
  │
  ├─ Controller: StoreController.addUserToStore()
  ├─ Validation: @Valid EmployeeStoreRequest
  │
  ▼
Service Layer: StoreServiceImpl.addUserToStore()
  │
  ├─ 1. Validate employeeId not empty
  ├─ 2. Find Employee by ID
  ├─ 3. Find Store by ID
  ├─ 4. Check relationship exists (duplicate check)
  ├─ 5. Create EmployeeStore entity
  ├─ 6. Save to database
  ├─ 7. Set relationships manually
  ├─ 8. Build response
  │
  ▼
Repository Layer: EmployeeStoreRepository
  │
  ├─ save(employeeStore)
  │
  ▼
Database: PostgreSQL (employee_stores table)
  │
  └─ INSERT INTO employee_stores
  │
  ▼
Response
  └─ EmployeeStoreResponse với đầy đủ thông tin
```

---

## Chi tiết từng bước

### Bước 1: Client gửi Request

**URL:** `http://152.53.227.115:8080/api/stores/users`

**Request:**
```http
POST /api/stores/users HTTP/1.1
Host: 152.53.227.115:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "employeeId": "0ed91bf2-6361-494a-baa1-a676213a9af0",
  "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7"
}
```

---

### Bước 2: API Gateway xử lý

**File:** `api-gateway/src/main/resources/application.yml`

**Route Configuration:**
```yaml
- id: user-service
  uri: lb://user-service
  predicates:
    - Path=/api/stores/**
```

**Quá trình:**
1. Gateway nhận request tại port `8080`
2. Kiểm tra path: `/api/stores/users` khớp với pattern `/api/stores/**`
3. Route đến service: `lb://user-service` (load balancer)
4. Service Discovery: Tìm `user-service` trong Eureka registry
5. Forward request đến `user-service` (port 8086)

**Load Balancing:**
- `lb://` = Load Balancer
- Nếu có nhiều instances của user-service, Gateway sẽ phân phối request

---

### Bước 3: User Service Controller

**File:** `user-service/.../controller/StoreController.java`

```java
@PostMapping("/users")
@Operation(summary = "Add user to store")
@ResponseStatus(HttpStatus.CREATED)
public ApiResponse<EmployeeStoreResponse> addUserToStore(
    @Valid @RequestBody EmployeeStoreRequest request
) {
    return ApiResponse.<EmployeeStoreResponse>builder()
            .status(HttpStatus.CREATED.value())
            .message("Employee added to store successfully")
            .data(storeService.addUserToStore(request))
            .build();
}
```

**Quá trình:**
1. Nhận HTTP POST request
2. Deserialize JSON → `EmployeeStoreRequest`
3. Validation: `@Valid` kiểm tra `@NotBlank` cho `employeeId` và `storeId`
4. Gọi `storeService.addUserToStore(request)`

---

### Bước 4: Service Layer - Validation cơ bản

**File:** `user-service/.../service/StoreServiceImpl.java`

```java
public EmployeeStoreResponse addUserToStore(EmployeeStoreRequest request) {
    String employeeId = request.getEmployeeId();
    
    // Validate employeeId not empty
    if (employeeId == null || employeeId.trim().isEmpty()) {
        throw new AppException(ErrorCode.INVALID_REQUEST);
    }
}
```

---

### Bước 5: Kiểm tra Employee tồn tại

```java
Employee employee = employeeRepository.findEmployeeById(employeeId)
    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
```

**Query thực thi:**
```sql
SELECT e FROM Employee e 
WHERE e.id = :id 
  AND e.account.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN') 
  AND e.isDeleted = false
```

**Lưu ý:**
- Chỉ tìm employees (không phải CUSTOMER)
- Phải không bị xóa (soft delete)
- Phải có role hợp lệ

**Đảm bảo Account được load:**
```java
employee.getAccount().getEmail();  // Trigger lazy loading
employee.getAccount().getRole();
```

---

### Bước 6: Kiểm tra Store tồn tại

```java
Store store = storeRepository.findByIdAndIsDeletedFalse(request.getStoreId())
    .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));
```

**Query thực thi:**
```sql
SELECT s FROM Store s 
WHERE s.id = :id AND s.isDeleted = false
```

---

### Bước 7: Kiểm tra Duplicate Relationship

```java
if (employeeStoreRepository.findByEmployeeIdAndStoreIdAndIsDeletedFalse(
        employeeId, request.getStoreId()).isPresent()) {
    throw new AppException(ErrorCode.USER_STORE_RELATIONSHIP_EXISTS);
}
```

**Query thực thi:**
```sql
SELECT es FROM EmployeeStore es 
WHERE es.employeeId = :employeeId 
  AND es.storeId = :storeId 
  AND es.isDeleted = false
```

**Mục đích:** Tránh tạo duplicate relationship

---

### Bước 8: Tạo EmployeeStore Entity

```java
EmployeeStore employeeStore = EmployeeStore.builder()
    .employeeId(employeeId)
    .storeId(request.getStoreId())
    .build();

EmployeeStore savedEmployeeStore = employeeStoreRepository.save(employeeStore);
```

**Database Insert:**
```sql
INSERT INTO employee_stores (
    employee_id, 
    store_id, 
    created_at, 
    updated_at, 
    is_deleted
) VALUES (
    :employeeId, 
    :storeId, 
    NOW(), 
    NOW(), 
    false
)
```

**Lưu ý:**
- Composite Primary Key: `(employee_id, store_id)`
- Extends `AbstractEntity` → có `createdAt`, `updatedAt`, `isDeleted`

---

### Bước 9: Set Relationships và Build Response

```java
// Set relationships manually (already loaded)
savedEmployeeStore.setEmployee(employee);
savedEmployeeStore.setStore(store);

// Build employee response
UserResponse employeeResponse = UserResponse.builder()
    .id(employee.getId())
    .email(employee.getAccount().getEmail())
    .role(employee.getAccount().getRole())
    .storeIds(storeIds)  // Load từ EmployeeStoreRepository
    // ... other fields
    .build();

// Build store response (simplified, không load all employees)
StoreResponse storeResponse = StoreResponse.builder()
    .id(store.getId())
    .name(store.getName())
    .users(null)  // Không load employees để tránh circular reference
    // ... other fields
    .build();

// Build final response
return EmployeeStoreResponse.builder()
    .employeeId(savedEmployeeStore.getEmployeeId())
    .storeId(savedEmployeeStore.getStoreId())
    .employee(employeeResponse)
    .store(storeResponse)
    .createdAt(savedEmployeeStore.getCreatedAt())
    .updatedAt(savedEmployeeStore.getUpdatedAt())
    .build();
```

---

### Bước 10: Response trả về

**Response Structure:**
```json
{
  "status": 201,
  "message": "Employee added to store successfully",
  "data": {
    "employeeId": "0ed91bf2-6361-494a-baa1-a676213a9af0",
    "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
    "employee": {
      "id": "0ed91bf2-6361-494a-baa1-a676213a9af0",
      "fullName": "Nguyễn Văn A",
      "email": "nguyenvana@furnimart.com",
      "role": "BRANCH_MANAGER",
      "storeIds": ["8d46e317-0596-4413-81b6-1a526398b3d7"],
      ...
    },
    "store": {
      "id": "8d46e317-0596-4413-81b6-1a526398b3d7",
      "name": "FurniMart - Nguyễn Kiệm",
      "users": null,  // Không load để tránh circular reference
      ...
    },
    "createdAt": "2025-11-07T10:00:00.000Z",
    "updatedAt": "2025-11-07T10:00:00.000Z"
  }
}
```

---

## Database Schema

### Bảng `employee_stores`

```sql
CREATE TABLE employee_stores (
    employee_id VARCHAR(36) NOT NULL,
    store_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    PRIMARY KEY (employee_id, store_id),
    FOREIGN KEY (employee_id) REFERENCES employees(id),
    FOREIGN KEY (store_id) REFERENCES stores(id)
);
```

**Đặc điểm:**
- Composite Primary Key: `(employee_id, store_id)`
- Một employee có thể có nhiều rows (mỗi row = một store)
- Soft delete: `is_deleted = true` thay vì xóa thật

---

## API Gateway Routing

### Route Configuration

```yaml
# api-gateway/src/main/resources/application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service  # Load balancer
          predicates:
            - Path=/api/stores/**  # Match /api/stores/users
```

### Request Flow qua Gateway

```
Client Request:
  POST http://152.53.227.115:8080/api/stores/users

Gateway Processing:
  1. Match path: /api/stores/users → /api/stores/**
  2. Route to: lb://user-service
  3. Service Discovery: Tìm user-service trong Eureka
  4. Forward to: http://user-service:8086/api/stores/users

User Service receives:
  POST /api/stores/users
  (Path được giữ nguyên, không bị rewrite)
```

---

## Transaction Management

**`@Transactional`** đảm bảo:
- Tất cả database operations trong một transaction
- Nếu có lỗi, tất cả thay đổi sẽ rollback
- Đảm bảo data consistency
- Lazy loading được thực hiện trong transaction

---

## Error Handling

### Các lỗi có thể xảy ra:

1. **400 Bad Request - INVALID_REQUEST**
   - `employeeId` hoặc `storeId` null/empty
   - Validation failed

2. **404 Not Found - USER_NOT_FOUND**
   - Employee không tồn tại
   - Employee đã bị xóa
   - Employee có role là CUSTOMER

3. **404 Not Found - STORE_NOT_FOUND**
   - Store không tồn tại
   - Store đã bị xóa

4. **400 Bad Request - USER_STORE_RELATIONSHIP_EXISTS**
   - Employee đã được gán vào Store này rồi

5. **500 Internal Server Error**
   - Lỗi khi save vào database
   - Lỗi khi map response
   - LazyInitializationException (đã được fix)

---

## Điểm quan trọng

### 1. Load Balancing
- API Gateway sử dụng `lb://user-service`
- Nếu có nhiều instances, request sẽ được phân phối

### 2. Service Discovery
- Gateway tìm service trong Eureka registry
- Service tự động register khi start

### 3. Path Matching
- Gateway match pattern: `/api/stores/**`
- Path được forward nguyên vẹn đến user-service

### 4. Authentication
- Token được forward từ Gateway đến User Service
- User Service validate token và check permissions

### 5. Response Building
- Không dùng `mapToStoreResponse()` (có thể load tất cả employees)
- Dùng `mapToStoreResponseWithoutUsers()` hoặc build trực tiếp
- Tránh circular reference và lazy loading issues

---

## Sequence Diagram

```
┌──────┐         ┌─────────────┐         ┌──────────────┐         ┌──────────┐
│Client│         │API Gateway  │         │User Service  │         │Database  │
└──┬───┘         └──────┬──────┘         └──────┬───────┘         └────┬─────┘
   │                    │                         │                      │
   │ POST /api/stores/  │                         │                      │
   │     users          │                         │                      │
   ├───────────────────>│                         │                      │
   │                    │                         │                      │
   │                    │ Route: /api/stores/**   │                      │
   │                    │ → lb://user-service     │                      │
   │                    │                         │                      │
   │                    │ POST /api/stores/users  │                      │
   │                    ├─────────────────────────>│                      │
   │                    │                         │                      │
   │                    │                         │ Validate employeeId  │
   │                    │                         │                      │
   │                    │                         │ Find Employee        │
   │                    │                         ├─────────────────────>│
   │                    │                         │<─────────────────────┤
   │                    │                         │                      │
   │                    │                         │ Find Store           │
   │                    │                         ├─────────────────────>│
   │                    │                         │<─────────────────────┤
   │                    │                         │                      │
   │                    │                         │ Check duplicate      │
   │                    │                         ├─────────────────────>│
   │                    │                         │<─────────────────────┤
   │                    │                         │                      │
   │                    │                         │ Save EmployeeStore   │
   │                    │                         ├─────────────────────>│
   │                    │                         │<─────────────────────┤
   │                    │                         │                      │
   │                    │                         │ Build Response       │
   │                    │                         │                      │
   │                    │ EmployeeStoreResponse  │                      │
   │                    │<─────────────────────────┤                      │
   │                    │                         │                      │
   │ Response           │                         │                      │
   │<───────────────────┤                         │                      │
   │                    │                         │                      │
```

---

## Ví dụ Request/Response

### Request

```bash
curl -X 'POST' \
  'http://152.53.227.115:8080/api/stores/users' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "employeeId": "0ed91bf2-6361-494a-baa1-a676213a9af0",
    "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7"
  }'
```

### Success Response (201)

```json
{
  "status": 201,
  "message": "Employee added to store successfully",
  "data": {
    "employeeId": "0ed91bf2-6361-494a-baa1-a676213a9af0",
    "storeId": "8d46e317-0596-4413-81b6-1a526398b3d7",
    "employee": { ... },
    "store": { ... },
    "createdAt": "2025-11-07T10:00:00.000Z",
    "updatedAt": "2025-11-07T10:00:00.000Z"
  }
}
```

### Error Response (400/404/500)

```json
{
  "status": 400,
  "message": "User store relationship already exists"
}
```

---

## Tóm tắt

1. **Client** → Gửi POST request đến API Gateway (port 8080)
2. **API Gateway** → Route request đến user-service qua Eureka
3. **User Service Controller** → Nhận request, validate
4. **Service Layer** → Validate, kiểm tra tồn tại, tạo relationship
5. **Repository Layer** → Save vào database
6. **Response** → Build và trả về EmployeeStoreResponse

**Đặc điểm:**
- ✅ Load balancing qua Gateway
- ✅ Service discovery qua Eureka
- ✅ Transaction management
- ✅ Error handling đầy đủ
- ✅ Tránh lazy loading issues
- ✅ Tránh circular reference

