# Giải Thích Về userId và user trong Wallet Entity

## ⚠️ Vấn Đề: user_id Bị Double Mapping

Hiện tại trong `Wallet.java`:
```java
@Column(name = "user_id", nullable = false)
private String userId;  // ✅ Map đến column user_id

@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", insertable = false, updatable = false)
private User user;  // ⚠️ Cũng map đến column user_id
```

**Vấn đề**: Cả hai đều map đến cùng column `user_id` trong database.

---

## 🤔 Tại Sao Cần userId?

### Lý do 1: Code hiện tại sử dụng userId để persist
```java
// WalletServiceImpl.java
Wallet wallet = Wallet.builder()
    .userId(userId)  // ✅ Sử dụng userId (String)
    .build();
```

### Lý do 2: Repository methods query theo userId
```java
// WalletRepository.java
Optional<Wallet> findByUserIdAndIsDeletedFalse(String userId);
boolean existsByUserIdAndIsDeletedFalse(String userId);
```

### Lý do 3: Dễ dàng truy cập foreign key mà không cần load User entity
```java
String userId = wallet.getUserId();  // ✅ Không cần lazy load
```

---

## 🤔 Tại Sao Cần user Entity?

### Lý do: Lazy load User khi cần
```java
User user = wallet.getUser();  // ✅ Lazy load User entity
String fullName = user.getFullName();
```

---

## ✅ Giải Pháp: Cả Hai Đều Cần Nhưng Phải Đúng Cách

### Cách 1: Giữ cả hai (HIỆN TẠI - ĐÚNG)
```java
@Column(name = "user_id", nullable = false)
private String userId;  // ✅ Để persist foreign key

@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", insertable = false, updatable = false)
private User user;  // ✅ Để lazy load (read-only)
```

**Ưu điểm**:
- ✅ `userId` để persist (insertable, updatable)
- ✅ `user` để lazy load (insertable = false, updatable = false)
- ✅ Không conflict vì `user` là read-only

**Nhược điểm**:
- ⚠️ Cả hai đều map đến cùng column (nhưng OK vì `user` là read-only)

### Cách 2: Chỉ dùng user Entity (CẦN SỬA CODE)
```java
// Xóa userId field
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;  // ✅ JPA tự động tạo column user_id

// Sửa code để dùng:
wallet.getUser().getId()  // Thay vì wallet.getUserId()
```

**Ưu điểm**:
- ✅ Không có double mapping
- ✅ Cleaner code

**Nhược điểm**:
- ❌ Phải sửa tất cả code sử dụng `userId`
- ❌ Phải load User entity mới lấy được userId

---

## 🎯 Khuyến Nghị

**GIỮ NGUYÊN CÁCH HIỆN TẠI** vì:
1. ✅ Code đã hoạt động tốt
2. ✅ `user` có `insertable = false, updatable = false` → không conflict
3. ✅ `userId` để persist, `user` để lazy load → mỗi cái có mục đích riêng
4. ✅ Không cần sửa nhiều code

**Lưu ý**: 
- `user` field là **read-only** (insertable = false, updatable = false)
- Chỉ `userId` được dùng để persist
- `user` chỉ được dùng để lazy load khi cần

---

## 📋 Kết Luận

**Không có vấn đề với double mapping** vì:
- `userId` (String) → persist foreign key
- `user` (User entity) → lazy load (read-only)
- Cả hai đều map đến cùng column nhưng không conflict vì `user` là read-only

**Có thể giữ nguyên** hoặc **sửa để chỉ dùng user entity** (nhưng cần sửa nhiều code).

