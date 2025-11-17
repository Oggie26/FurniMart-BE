# Vấn đề: `userId` vs `employeeId` trong Store/EmployeeStore

## Tổng quan

Có sự **không nhất quán** trong naming giữa các layer của ứng dụng:

- ✅ **Entity & Database**: Dùng `employeeId` (đúng)
- ✅ **Repository**: Dùng `employeeId` (đúng)  
- ⚠️ **Service/Controller**: Một số method vẫn dùng `userId` (không nhất quán)

---

## Chi tiết

### 1. Entity `EmployeeStore` - Dùng `employeeId` ✅

```java
@Entity
@Table(name = "employee_stores")
public class EmployeeStore {
    @Id
    @Column(name = "employee_id")  // Database column: employee_id
    private String employeeId;      // Java field: employeeId
    
    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;
}
```

**Lý do:** Đây là quan hệ giữa **Employee** và **Store**, không phải User và Store.

---

### 2. Repository - Dùng `employeeId` ✅

```java
@Query("SELECT es FROM EmployeeStore es WHERE es.employeeId = :employeeId")
Optional<EmployeeStore> findByEmployeeIdAndStoreId(String employeeId, String storeId);
```

**Lý do:** Query dựa trên field `employeeId` của entity.

---

### 3. Service/Controller - Dùng `userId` ⚠️

#### Service Interface:
```java
void removeUserFromStore(String userId, String storeId);  // ⚠️ userId
List<StoreResponse> getStoresByUserId(String userId);      // ⚠️ userId
```

#### Service Implementation:
```java
public void removeUserFromStore(String userId, String storeId) {
    // Nhưng lại gọi method với employeeId:
    employeeStoreRepository.findByEmployeeIdAndStoreIdAndIsDeletedFalse(userId, storeId);
    //                                                                    ^^^^^^
    //                                                                    userId được dùng như employeeId
}
```

#### Controller:
```java
@DeleteMapping("/users/{userId}/stores/{storeId}")  // ⚠️ Path dùng userId
public ApiResponse<Void> removeUserFromStore(@PathVariable String userId, ...) {
    storeService.removeUserFromStore(userId, storeId);
}
```

---

## Lý do thiết kế ban đầu

### 1. **Conceptual Overlap**
- Trong hệ thống, `Employee` có thể được coi là một loại `User`
- Có thể có inheritance: `Employee extends User` hoặc shared concept
- Do đó ban đầu dùng `userId` để generic hơn

### 2. **Evolution**
- Sau đó nhận ra chỉ **employees** mới được gán vào stores
- **Customers** không thể được gán vào stores
- Đổi sang `employeeId` để rõ ràng và type-safe hơn

### 3. **Backward Compatibility**
- Một số methods vẫn giữ `userId` để không breaking existing clients
- API paths như `/users/{userId}` đã được expose và có thể đang được sử dụng

---

## Vấn đề hiện tại

### 1. **Confusion**
- Developer mới có thể nhầm lẫn: `userId` có phải là `employeeId` không?
- Code không self-documenting

### 2. **Type Safety**
- `userId` có thể là bất kỳ user nào (customer, employee, admin)
- `employeeId` rõ ràng hơn: chỉ employees

### 3. **Maintenance**
- Phải nhớ rằng `userId` trong context này thực chất là `employeeId`
- Comment trong code: `// userId is actually employeeId in this context`

---

## Giải pháp đề xuất

### Option 1: Giữ Backward Compatibility (Recommended)

**Giữ API paths cũ** nhưng **đổi parameter names** trong code:

```java
// Controller - Giữ path cũ
@DeleteMapping("/users/{userId}/stores/{storeId}")
public ApiResponse<Void> removeUserFromStore(
    @PathVariable("userId") String employeeId,  // Rename parameter
    @PathVariable String storeId
) {
    storeService.removeEmployeeFromStore(employeeId, storeId);
}

// Service - Đổi tên method
void removeEmployeeFromStore(String employeeId, String storeId);
```

**Ưu điểm:**
- ✅ Không breaking existing clients
- ✅ Code rõ ràng hơn
- ✅ Self-documenting

**Nhược điểm:**
- ⚠️ Path vẫn dùng `/users/` có thể gây confusion

---

### Option 2: Refactor hoàn toàn

**Đổi tất cả sang `employeeId`:**

```java
// Controller
@DeleteMapping("/employees/{employeeId}/stores/{storeId}")
public ApiResponse<Void> removeEmployeeFromStore(
    @PathVariable String employeeId,
    @PathVariable String storeId
) {
    storeService.removeEmployeeFromStore(employeeId, storeId);
}

// Service
void removeEmployeeFromStore(String employeeId, String storeId);
```

**Ưu điểm:**
- ✅ Nhất quán hoàn toàn
- ✅ Rõ ràng và type-safe
- ✅ Self-documenting

**Nhược điểm:**
- ⚠️ Breaking change - phải update clients
- ⚠️ Có thể cần versioning API (v1, v2)

---

### Option 3: Dual Support (Deprecated)

**Giữ cả 2, mark cũ là deprecated:**

```java
// Old API - Deprecated
@Deprecated
@DeleteMapping("/users/{userId}/stores/{storeId}")
public ApiResponse<Void> removeUserFromStore(@PathVariable String userId, ...) {
    return removeEmployeeFromStore(userId, storeId);
}

// New API
@DeleteMapping("/employees/{employeeId}/stores/{storeId}")
public ApiResponse<Void> removeEmployeeFromStore(@PathVariable String employeeId, ...) {
    storeService.removeEmployeeFromStore(employeeId, storeId);
}
```

**Ưu điểm:**
- ✅ Backward compatible
- ✅ Có migration path
- ✅ Có thể remove deprecated sau

**Nhược điểm:**
- ⚠️ Code duplication
- ⚠️ Phải maintain 2 endpoints

---

## So sánh với các entity khác

### `FavoriteProduct` - Dùng `userId` ✅
```java
@Column(name = "user_id")
private String userId;  // Đúng vì cả customer và employee đều có thể favorite
```

### `Wallet` - Dùng `userId` ✅
```java
@Column(name = "user_id")
private String userId;  // Đúng vì cả customer và employee đều có wallet
```

### `EmployeeStore` - Dùng `employeeId` ✅
```java
@Column(name = "employee_id")
private String employeeId;  // Đúng vì chỉ employees mới được gán vào stores
```

---

## Kết luận

**Vấn đề:** Có sự không nhất quán giữa entity/repository (dùng `employeeId`) và service/controller (dùng `userId`).

**Nguyên nhân:** Evolution của codebase, từ generic `userId` sang specific `employeeId`.

**Giải pháp:** 
- **Ngắn hạn:** Giữ backward compatibility, đổi parameter names trong code
- **Dài hạn:** Refactor hoàn toàn sang `employeeId`, có thể dùng API versioning

**Recommendation:** Option 1 (giữ path, đổi parameter) để balance giữa clarity và backward compatibility.

