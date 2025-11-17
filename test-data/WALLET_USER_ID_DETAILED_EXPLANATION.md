# Giải Thích Chi Tiết: userId và user trong Wallet.java (dòng 36-41)

## 📋 Code Hiện Tại

```java
@Column(name = "user_id", nullable = false)
private String userId;

@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", insertable = false, updatable = false)
private User user;
```

---

## 🔍 Phân Tích Từng Dòng

### Dòng 36-37: `userId` (String)

```java
@Column(name = "user_id", nullable = false)
private String userId;
```

**Mục đích**: 
- Lưu trữ **foreign key** dưới dạng String (UUID của User)
- Được dùng để **persist** (insert/update) vào database

**Cách hoạt động**:
- Khi tạo Wallet: `Wallet.builder().userId("user-uuid-123").build()`
- JPA sẽ insert `user_id = "user-uuid-123"` vào bảng `wallets`
- Không cần load User entity, chỉ cần biết userId

**Ưu điểm**:
- ✅ Nhanh (không cần join với bảng users)
- ✅ Đơn giản (chỉ là String)
- ✅ Dễ query: `findByUserIdAndIsDeletedFalse(userId)`

**Ví dụ sử dụng**:
```java
// Tạo wallet mới
Wallet wallet = Wallet.builder()
    .code("WLT-ABC123")
    .balance(BigDecimal.ZERO)
    .status(WalletStatus.ACTIVE)
    .userId("user-uuid-123")  // ✅ Chỉ cần userId
    .build();
walletRepository.save(wallet);

// Query theo userId
Optional<Wallet> wallet = walletRepository.findByUserIdAndIsDeletedFalse("user-uuid-123");
```

---

### Dòng 39-41: `user` (User Entity)

```java
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", insertable = false, updatable = false)
private User user;
```

**Mục đích**:
- Lazy load **User entity** khi cần truy cập thông tin User
- **Read-only** (không dùng để persist)

**Cách hoạt động**:
- `@OneToOne`: Quan hệ 1-1 với User
- `fetch = FetchType.LAZY`: Chỉ load User khi được truy cập
- `@JoinColumn(name = "user_id")`: Map đến column `user_id` trong database
- `insertable = false, updatable = false`: **KHÔNG** dùng để insert/update

**Ưu điểm**:
- ✅ Lazy load: Chỉ load User khi cần (tiết kiệm memory)
- ✅ Truy cập thông tin User: `wallet.getUser().getFullName()`
- ✅ Read-only: Không conflict với `userId` khi persist

**Ví dụ sử dụng**:
```java
// Lazy load User khi cần
Wallet wallet = walletRepository.findById(walletId);
User user = wallet.getUser();  // ✅ Lazy load User từ database
String fullName = user.getFullName();  // Truy cập thông tin User
```

---

## 🤔 Tại Sao Cần Cả Hai?

### Scenario 1: Tạo Wallet (Cần `userId`)
```java
// Khi tạo wallet, chỉ cần userId (String)
Wallet wallet = Wallet.builder()
    .userId(savedUser.getId())  // ✅ Chỉ cần userId
    .build();
// Không cần load User entity → Nhanh hơn
```

### Scenario 2: Hiển Thị Thông Tin (Cần `user`)
```java
// Khi hiển thị wallet với thông tin user
Wallet wallet = walletRepository.findById(walletId);
WalletResponse response = WalletResponse.builder()
    .id(wallet.getId())
    .code(wallet.getCode())
    .balance(wallet.getBalance())
    .userFullName(wallet.getUser().getFullName())  // ✅ Cần User entity
    .build();
```

### Scenario 3: Query Theo userId (Cần `userId`)
```java
// Query wallet theo userId
Optional<Wallet> wallet = walletRepository.findByUserIdAndIsDeletedFalse(userId);
// Repository method sử dụng userId field, không cần load User
```

---

## ⚠️ Tại Sao Không Conflict?

### 1. `userId` - Owning Side (Có thể insert/update)
```java
@Column(name = "user_id", nullable = false)
private String userId;
```
- ✅ **Có thể** insert/update
- ✅ JPA sẽ persist giá trị này vào column `user_id`

### 2. `user` - Read-Only Side (Không insert/update)
```java
@JoinColumn(name = "user_id", insertable = false, updatable = false)
private User user;
```
- ❌ **Không thể** insert/update (`insertable = false, updatable = false`)
- ✅ Chỉ dùng để **đọc** (lazy load)
- ✅ JPA **không** persist giá trị này

### Kết Quả:
- Khi **persist**: Chỉ `userId` được dùng → Không conflict
- Khi **read**: Có thể dùng cả `userId` hoặc `user` → Không conflict
- Cả hai đều map đến cùng column nhưng **không cùng lúc** được persist

---

## 📊 So Sánh

| Tiêu Chí | `userId` (String) | `user` (User Entity) |
|----------|-------------------|----------------------|
| **Mục đích** | Persist foreign key | Lazy load User entity |
| **Insert/Update** | ✅ Có thể | ❌ Không thể (read-only) |
| **Performance** | ✅ Nhanh (không join) | ⚠️ Chậm hơn (cần join) |
| **Khi nào dùng** | Tạo/Update wallet | Hiển thị thông tin User |
| **Query** | ✅ Dùng trong repository | ❌ Không dùng trong repository |

---

## 🎯 Best Practices

### ✅ Nên Dùng `userId` Khi:
- Tạo wallet mới
- Update wallet
- Query wallet theo userId
- Chỉ cần biết userId (không cần thông tin User)

### ✅ Nên Dùng `user` Khi:
- Hiển thị thông tin User trong response
- Cần truy cập các field của User (fullName, phone, etc.)
- Mapping sang WalletResponse với userFullName

### ❌ Không Nên:
- Dùng `user` để persist (sẽ bị ignore vì `insertable = false`)
- Load `user` khi không cần (lãng phí performance)

---

## 💡 Ví Dụ Thực Tế

### Trong WalletServiceImpl.java:

```java
// ✅ Dùng userId để tạo wallet
Wallet wallet = Wallet.builder()
    .code(walletCode)
    .balance(BigDecimal.ZERO)
    .status(WalletStatus.ACTIVE)
    .userId(userId)  // ✅ Dùng userId
    .build();

// ✅ Dùng userId để query
User user = userRepository.findByIdAndIsDeletedFalse(wallet.getUserId())
    .orElse(null);

// ✅ Dùng user để map response
return WalletResponse.builder()
    .id(wallet.getId())
    .code(wallet.getCode())
    .balance(wallet.getBalance())
    .userId(wallet.getUserId())  // ✅ Từ userId field
    .userFullName(user != null ? user.getFullName() : null)  // ✅ Từ User entity
    .build();
```

---

## 🔧 Có Thể Tối Ưu Không?

### Option 1: Chỉ Dùng `user` Entity (Cần Sửa Nhiều Code)
```java
// Xóa userId field
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;

// Sửa code:
wallet.getUser().getId()  // Thay vì wallet.getUserId()
```

**Nhược điểm**:
- ❌ Phải load User entity mới lấy được userId
- ❌ Phải sửa tất cả code sử dụng `userId`
- ❌ Repository methods phải query theo `user.id`

### Option 2: Giữ Nguyên (KHUYẾN NGHỊ)
```java
// Giữ cả hai như hiện tại
@Column(name = "user_id", nullable = false)
private String userId;

@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", insertable = false, updatable = false)
private User user;
```

**Ưu điểm**:
- ✅ Linh hoạt: Dùng `userId` khi cần, `user` khi cần
- ✅ Performance tốt: Không cần load User khi không cần
- ✅ Code đã hoạt động tốt

---

## 📝 Kết Luận

**Đoạn code này là ĐÚNG và TỐI ƯU**:
- `userId` (String) → Persist foreign key (nhanh, đơn giản)
- `user` (User entity) → Lazy load User khi cần (linh hoạt)
- Không conflict vì `user` là read-only
- Mỗi field có mục đích riêng và bổ sung cho nhau

**Khuyến nghị**: **GIỮ NGUYÊN** cách hiện tại.

