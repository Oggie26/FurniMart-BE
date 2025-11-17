# Endpoint: GET /api/employees/email/{email}

## 📋 Mục Đích

Endpoint này được tạo để **lấy thông tin employee (nhân viên) bằng email**.

---

## 🎯 Tại Sao Cần Endpoint Này?

### 1. **Test Scripts Cần Email Thay Vì ID**

Khi test các chức năng DELIVERY và STAFF, test scripts thường:
- ✅ Có **email** của employee (ví dụ: `delivery@furnimart.com`)
- ❌ **Không có** employee ID (UUID dài và khó nhớ)

**Ví dụ trong test script:**
```powershell
$DELIVERY_EMAIL = "delivery@furnimart.com"

# Cần lấy employee ID từ email để test các chức năng khác
$userResponse = Invoke-RestMethod -Uri "$USER_SERVICE_URL/api/employees/email/$DELIVERY_EMAIL"
$DELIVERY_STAFF_ID = $userResponse.data.id  # Lấy ID từ email
```

### 2. **So Sánh Với Các Endpoint Khác**

| Endpoint | Input | Khi Nào Dùng |
|----------|-------|--------------|
| `GET /api/employees/{id}` | Employee ID (UUID) | Khi đã có ID |
| `GET /api/employees/email/{email}` | Email | Khi chỉ có email (thường gặp hơn) |
| `GET /api/employees/profile` | Token (tự động) | Lấy thông tin của chính mình |
| `GET /api/employees/account/{accountId}` | Account ID | Khi có account ID |

**Lý do:** Email dễ nhớ và thường có sẵn hơn UUID!

---

## 💡 Use Cases (Trường Hợp Sử Dụng)

### 1. **Test Scripts**
```powershell
# Test DELIVERY functions
$DELIVERY_EMAIL = "delivery@furnimart.com"

# Lấy employee ID từ email
$employee = Invoke-RestMethod -Uri "/api/employees/email/$DELIVERY_EMAIL"
$DELIVERY_STAFF_ID = $employee.data.id

# Sau đó dùng ID để test các chức năng khác
Invoke-RestMethod -Uri "/api/delivery/assignments/staff/$DELIVERY_STAFF_ID"
```

### 2. **Inter-Service Communication**
```java
// Service khác cần lấy thông tin employee từ email
// Ví dụ: delivery-service cần thông tin delivery staff
@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/api/employees/email/{email}")
    ApiResponse<UserResponse> getEmployeeByEmail(@PathVariable String email);
}
```

### 3. **Admin Tools / Management Dashboard**
- Admin có thể tìm kiếm employee bằng email
- Dễ dàng hơn việc phải nhớ UUID

### 4. **Integration với External Systems**
- Khi nhận email từ hệ thống bên ngoài
- Cần lấy thông tin employee để xử lý

---

## 📝 API Specification

### Endpoint
```
GET /api/employees/email/{email}
```

### Authorization
- **Required:** ✅ Yes (Bearer Token)
- **Roles:** `ADMIN`, `BRANCH_MANAGER`, `STAFF`, `DELIVERY`

### Path Parameters
- `email` (String, required): Email của employee cần tìm

### Response (200 OK)
```json
{
  "status": 200,
  "message": "Employee retrieved successfully",
  "data": {
    "id": "880c5184-668f-4b09-b9af-99b59803918d",
    "fullName": "Le Van Giao Hang",
    "email": "delivery@furnimart.com",
    "phone": "0933333333",
    "role": "DELIVERY",
    "status": "ACTIVE",
    "storeIds": ["8d46e317-0596-4413-81b6-1a526398b3d7"],
    "createdAt": "2025-11-10T10:00:00.000Z",
    "updatedAt": "2025-11-10T10:00:00.000Z"
  }
}
```

### Error Responses

#### 404 Not Found
```json
{
  "status": 404,
  "message": "Employee not found",
  "timestamp": "2025-11-10T10:00:00.000Z"
}
```
**Nguyên nhân:**
- Email không tồn tại
- Email thuộc về CUSTOMER (không phải employee)
- Employee đã bị xóa (soft delete)

#### 401 Unauthorized
```json
{
  "status": 401,
  "message": "Unauthorized",
  "timestamp": "2025-11-10T10:00:00.000Z"
}
```
**Nguyên nhân:** Không có token hoặc token không hợp lệ

#### 403 Forbidden
```json
{
  "status": 403,
  "message": "Forbidden",
  "timestamp": "2025-11-10T10:00:00.000Z"
}
```
**Nguyên nhân:** Role không có quyền truy cập (chỉ ADMIN, BRANCH_MANAGER, STAFF, DELIVERY)

---

## 🔍 Implementation Details

### Service Layer
```java
@Override
public UserResponse getEmployeeByEmail(String email) {
    log.info("Fetching employee by email: {}", email);
    
    Employee employee = employeeRepository.findByEmailAndIsDeletedFalse(email)
            .orElseThrow(() -> {
                log.error("Employee not found for email: {}", email);
                return new AppException(ErrorCode.USER_NOT_FOUND);
            });

    return toEmployeeResponse(employee);
}
```

### Repository Query
```java
@Query("SELECT e FROM Employee e WHERE e.account.email = :email " +
       "AND e.account.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN') " +
       "AND e.isDeleted = false")
Optional<Employee> findByEmailAndIsDeletedFalse(@Param("email") String email);
```

**Lưu ý:**
- Chỉ tìm employees (không bao gồm CUSTOMER)
- Chỉ tìm employees chưa bị xóa (soft delete)

---

## ✅ Ví Dụ Sử Dụng

### PowerShell
```powershell
$BASE_URL = "http://152.53.227.115:8086"
$EMAIL = "delivery@furnimart.com"
$TOKEN = "your-jwt-token"

$response = Invoke-RestMethod -Uri "$BASE_URL/api/employees/email/$EMAIL" `
    -Method GET `
    -Headers @{"Authorization" = "Bearer $TOKEN"}

Write-Host "Employee ID: $($response.data.id)"
Write-Host "Full Name: $($response.data.fullName)"
Write-Host "Role: $($response.data.role)"
```

### cURL
```bash
curl -X GET "http://152.53.227.115:8086/api/employees/email/delivery@furnimart.com" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### JavaScript/TypeScript
```javascript
const response = await fetch(
  'http://152.53.227.115:8086/api/employees/email/delivery@furnimart.com',
  {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
);

const data = await response.json();
console.log('Employee ID:', data.data.id);
```

---

## 🆚 So Sánh Với Endpoint Tương Tự

### `GET /api/users/email/{email}` (UserController)
- **Dành cho:** CUSTOMER (người dùng thông thường)
- **Không cần authorization** (public endpoint)
- **Khác:** Endpoint này dành cho employees, cần authorization

### `GET /api/employees/profile` (EmployeeController)
- **Dành cho:** Employee hiện tại (từ token)
- **Không cần email** (tự động lấy từ token)
- **Khác:** Endpoint này cho phép lấy thông tin employee khác bằng email

---

## 📊 Tóm Tắt

| Thuộc Tính | Giá Trị |
|------------|---------|
| **Mục đích** | Lấy thông tin employee bằng email |
| **Input** | Email (String) |
| **Output** | UserResponse (thông tin employee đầy đủ) |
| **Authorization** | Required (ADMIN, BRANCH_MANAGER, STAFF, DELIVERY) |
| **Use Case Chính** | Test scripts, inter-service communication |
| **Lợi ích** | Dễ sử dụng hơn UUID, email thường có sẵn |

---

## ✅ Kết Luận

Endpoint `GET /api/employees/email/{email}` được tạo để:
1. ✅ **Giải quyết vấn đề test scripts** - Cần lấy employee ID từ email
2. ✅ **Hỗ trợ inter-service communication** - Dễ dàng lấy thông tin employee
3. ✅ **Cải thiện UX** - Email dễ nhớ hơn UUID
4. ✅ **Đảm bảo tính nhất quán** - Tương tự như endpoint `/api/users/email/{email}` cho CUSTOMER

**Đây là endpoint quan trọng để test và sử dụng các chức năng DELIVERY và STAFF!**

