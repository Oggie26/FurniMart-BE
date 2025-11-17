# BÁO CÁO LỖI: USER KHÔNG TÌM THẤY ACCOUNT

## 🔴 VẤN ĐỀ

Có người báo service bị lỗi "user không tìm thấy account". Sau khi kiểm tra code, phát hiện **nhiều nơi truy cập `user.getAccount()` mà không kiểm tra null**, có thể gây ra `NullPointerException`.

---

## 📍 CÁC VỊ TRÍ CÓ VẤN ĐỀ

### 1. **UserServiceImpl.java**

#### ❌ `deleteUser()` - Line 227, 231, 234
```java
log.info("Found user to delete: {} (email: {})", user.getFullName(), user.getAccount().getEmail());
// ...
user.getAccount().setIsDeleted(true);
// ...
Account savedAccount = accountRepository.save(user.getAccount());
```
**Vấn đề**: Không kiểm tra `user.getAccount()` có null không.

#### ❌ `disableUser()` - Line 253, 256
```java
user.getAccount().setStatus(EnumStatus.INACTIVE);
// ...
accountRepository.save(user.getAccount());
```
**Vấn đề**: Không kiểm tra `user.getAccount()` có null không.

#### ❌ `enableUser()` - Line 268, 271
```java
user.getAccount().setStatus(EnumStatus.ACTIVE);
// ...
accountRepository.save(user.getAccount());
```
**Vấn đề**: Không kiểm tra `user.getAccount()` có null không.

#### ❌ `changePassword()` - Line 320
```java
Account account = user.getAccount();
if (!passwordEncoder.matches(changePassword.getOldPassword(), account.getPassword())) {
```
**Vấn đề**: Không kiểm tra `user.getAccount()` có null không trước khi sử dụng.

#### ❌ `updateUserRole()` - Line 414, 422, 423
```java
if (user.getAccount().getRole() == EnumRole.CUSTOMER) {
    throw new AppException(ErrorCode.CANNOT_UPDATE_CUSTOMER_ROLE);
}
// ...
user.getAccount().setRole(newRole);
accountRepository.save(user.getAccount());
```
**Vấn đề**: Không kiểm tra `user.getAccount()` có null không.

---

## ✅ CÁC VỊ TRÍ ĐÃ CÓ NULL CHECK

### 1. **UserServiceImpl.java - `toUserResponse()`** - Line 365-366
```java
.email(user.getAccount() != null ? user.getAccount().getEmail() : null)
.role(user.getAccount() != null ? user.getAccount().getRole() : null)
```
✅ **Đã có null check**

### 2. **StoreServiceImpl.java - `mapEmployeeToUserResponse()`** - Line 429-432
```java
if (employee.getAccount() == null) {
    log.warn("Employee {} has null account", employee.getId());
    return null;
}
```
✅ **Đã có null check**

---

## 🔍 NGUYÊN NHÂN CÓ THỂ XẢY RA

1. **Lazy Loading Fail**: 
   - User có `@OneToOne(fetch = FetchType.LAZY)` với Account
   - Nếu Account bị xóa nhưng User vẫn còn, lazy load sẽ fail
   - Hoặc Account bị soft delete nhưng User chưa được cập nhật

2. **Dữ liệu không nhất quán**:
   - Có thể có User không có Account (do bug hoặc manual delete trong database)
   - Mặc dù entity có `nullable = false`, nhưng trong thực tế có thể vi phạm constraint

3. **Transaction rollback**:
   - Nếu có lỗi trong transaction, Account có thể không được tạo nhưng User đã được tạo

4. **Concurrent access**:
   - Nếu có nhiều thread cùng truy cập, có thể xảy ra race condition

---

## 🛠️ GIẢI PHÁP

### Option 1: Thêm null check ở tất cả các method (Recommended)
Thêm null check trước khi truy cập `user.getAccount()` và throw exception rõ ràng.

### Option 2: Sử dụng Optional hoặc validation
Kiểm tra User có Account trước khi lưu vào database.

### Option 3: Sử dụng @NotNull annotation
Thêm validation ở entity level.

---

## 📝 RECOMMENDATIONS

1. **Thêm null check** ở tất cả các method truy cập `user.getAccount()`
2. **Thêm logging** để track các trường hợp User không có Account
3. **Thêm validation** khi tạo User để đảm bảo luôn có Account
4. **Thêm database constraint** để đảm bảo User luôn có Account
5. **Thêm unit tests** để test các trường hợp null

---

## 🎯 PRIORITY

**HIGH** - Cần fix ngay vì có thể gây crash service khi gặp dữ liệu không nhất quán.

---

**Báo cáo được tạo tự động bởi AI Assistant**

