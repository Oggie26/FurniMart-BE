# Phân Tích Quan Hệ Wallet ↔ User

## Tổng Quan

Quan hệ giữa `Wallet` và `User` là **One-to-One (1:1)**, với mỗi User chỉ có thể có một Wallet và mỗi Wallet chỉ thuộc về một User.

---

## Cấu Trúc Entity

### 1. Wallet Entity (Owning Side)

```java
@Entity
@Table(name = "wallets", uniqueConstraints = {
    @UniqueConstraint(columnNames = "code"),
    @UniqueConstraint(columnNames = "user_id")  // ✅ Đảm bảo 1 user chỉ có 1 wallet
})
public class Wallet extends AbstractEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "user_id", nullable = false)  // ✅ Foreign key column
    private String userId;  // ✅ Lưu trữ user_id dưới dạng String
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;  // ✅ Entity reference (read-only)
    
    // ... other fields
}
```

**Đặc điểm**:
- ✅ Wallet là **owning side** (có foreign key `user_id` trong database)
- ✅ Có constraint `UNIQUE` trên `user_id` → đảm bảo 1 user chỉ có 1 wallet
- ✅ Có cả `userId` (String) và `user` (User entity)
- ✅ `user` field có `insertable = false, updatable = false` → chỉ dùng để đọc, không dùng để persist

### 2. User Entity (Inverse Side)

```java
@Entity
@Table(name = "users")
public class User extends AbstractEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Wallet wallet;  // ✅ Inverse side (không có foreign key)
    
    // ... other fields
}
```

**Đặc điểm**:
- ✅ User là **inverse side** (không có foreign key)
- ✅ Sử dụng `mappedBy = "user"` → tham chiếu đến field `user` trong Wallet
- ✅ Fetch type là `LAZY` → chỉ load khi cần

---

## Phân Tích Chi Tiết

### ✅ Điểm Đúng

1. **Quan hệ One-to-One được thiết lập đúng**:
   - Wallet có foreign key `user_id`
   - User có `mappedBy` để tham chiếu ngược lại
   - Constraint `UNIQUE` trên `user_id` đảm bảo 1:1

2. **Cách sử dụng trong code**:
   ```java
   // Trong WalletServiceImpl
   Wallet wallet = Wallet.builder()
       .code(walletCode)
       .balance(BigDecimal.ZERO)
       .status(WalletStatus.ACTIVE)
       .userId(userId)  // ✅ Sử dụng userId (String) để set foreign key
       .build();
   ```

3. **Auto-create wallet khi tạo user**:
   ```java
   // Trong UserServiceImpl và AuthServiceImpl
   User savedUser = userRepository.save(user);
   
   // Auto-create wallet for new customer
   walletService.createWalletForUser(savedUser.getId());  // ✅ Sử dụng userId
   ```

### ⚠️ Vấn Đề Tiềm Ẩn

1. **Mapping có thể gây nhầm lẫn**:
   - Trong Wallet: `@JoinColumn(name = "user_id")` tham chiếu đến field `user`
   - Trong User: `mappedBy = "user"` tham chiếu đến field `user` trong Wallet
   - Nhưng khi persist, code sử dụng `userId` (String) thay vì `user` (User entity)
   - Điều này là **đúng** vì `user` field có `insertable = false, updatable = false`

2. **Có thể cải thiện**:
   - Có thể thêm validation để đảm bảo `userId` tồn tại trong bảng `users`
   - Có thể thêm `@ForeignKey` annotation để rõ ràng hơn về foreign key constraint

---

## Database Schema

### wallets table
```sql
CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    status VARCHAR NOT NULL,
    user_id UUID NOT NULL,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_wallet_user_id UNIQUE (user_id)  -- ✅ Đảm bảo 1:1
);
```

### users table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    full_name VARCHAR,
    phone VARCHAR UNIQUE,
    -- ... other fields
    account_id UUID NOT NULL,
    
    CONSTRAINT fk_user_account FOREIGN KEY (account_id) REFERENCES accounts(id)
    -- ✅ Không có foreign key đến wallets (inverse side)
);
```

---

## Cách Hoạt Động

### 1. Tạo Wallet cho User

```java
// Step 1: Tạo User
User user = User.builder()
    .fullName("John Doe")
    .account(account)
    .build();
User savedUser = userRepository.save(user);

// Step 2: Tạo Wallet (auto hoặc manual)
Wallet wallet = Wallet.builder()
    .code("WLT-ABC123")
    .balance(BigDecimal.ZERO)
    .status(WalletStatus.ACTIVE)
    .userId(savedUser.getId())  // ✅ Set foreign key
    .build();
Wallet savedWallet = walletRepository.save(wallet);

// Step 3: Khi query, có thể access user từ wallet
Wallet walletWithUser = walletRepository.findById(walletId);
User user = wallet.getUser();  // ✅ Lazy load user
```

### 2. Query Wallet từ User

```java
// Từ User entity
User user = userRepository.findById(userId);
Wallet wallet = user.getWallet();  // ✅ Lazy load wallet

// Hoặc query trực tiếp
Wallet wallet = walletRepository.findByUserIdAndIsDeletedFalse(userId);
```

---

## Best Practices

### ✅ Đúng

1. **Sử dụng `userId` (String) để persist**:
   ```java
   Wallet wallet = Wallet.builder()
       .userId(userId)  // ✅ Đúng
       .build();
   ```

2. **Sử dụng `user` (User entity) để đọc**:
   ```java
   Wallet wallet = walletRepository.findById(walletId);
   String userName = wallet.getUser().getFullName();  // ✅ Đúng (lazy load)
   ```

3. **Kiểm tra user tồn tại trước khi tạo wallet**:
   ```java
   User user = userRepository.findByIdAndIsDeletedFalse(userId)
       .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
   ```

### ❌ Sai

1. **Không sử dụng `user` entity để persist**:
   ```java
   // ❌ SAI - user field có insertable = false
   Wallet wallet = Wallet.builder()
       .user(user)  // ❌ Không hoạt động
       .build();
   ```

2. **Không set `userId` và `user` cùng lúc**:
   ```java
   // ⚠️ Có thể gây nhầm lẫn
   Wallet wallet = Wallet.builder()
       .userId(userId)
       .user(user)  // ⚠️ Không cần thiết khi persist
       .build();
   ```

---

## Kết Luận

### ✅ Quan Hệ Đúng

1. **One-to-One relationship được thiết lập đúng**:
   - Wallet là owning side (có foreign key)
   - User là inverse side (có mappedBy)
   - Constraint UNIQUE đảm bảo 1:1

2. **Cách sử dụng trong code là đúng**:
   - Sử dụng `userId` (String) để persist
   - Sử dụng `user` (User entity) để đọc (lazy load)
   - Auto-create wallet khi tạo user mới

### 🔧 Có Thể Cải Thiện

1. **Thêm validation**:
   - Validate `userId` tồn tại trước khi tạo wallet
   - Validate không có wallet nào khác cho user đó (đã có trong code)

2. **Thêm documentation**:
   - Comment rõ ràng về việc sử dụng `userId` vs `user`
   - Document về lazy loading behavior

3. **Có thể thêm cascade** (nếu cần):
   - Hiện tại không có cascade từ User → Wallet
   - Nếu muốn xóa wallet khi xóa user, có thể thêm cascade

---

## Recommendations

### ✅ Giữ Nguyên (Đang Đúng)

1. **Cấu trúc hiện tại là đúng**:
   - Wallet là owning side
   - User là inverse side
   - Constraint UNIQUE đảm bảo 1:1

2. **Cách sử dụng trong service là đúng**:
   - Sử dụng `userId` để persist
   - Sử dụng `user` để đọc

### 🔧 Có Thể Cải Thiện (Optional)

1. **Thêm validation**:
   ```java
   @Column(name = "user_id", nullable = false)
   @NotNull(message = "User ID is required")
   private String userId;
   ```

2. **Thêm comment**:
   ```java
   /**
    * Foreign key to users table.
    * Use this field to set the user when creating/updating wallet.
    */
   @Column(name = "user_id", nullable = false)
   private String userId;
   
   /**
    * User entity reference (read-only).
    * This field is for lazy loading user data, not for persistence.
    */
   @OneToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "user_id", insertable = false, updatable = false)
   private User user;
   ```

3. **Có thể thêm cascade** (nếu muốn xóa wallet khi xóa user):
   ```java
   // Trong User entity
   @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
   private Wallet wallet;
   ```
   **Lưu ý**: Hiện tại không có cascade, nên khi xóa user, wallet vẫn tồn tại (soft delete). Điều này có thể là mong muốn để giữ lại lịch sử.

---

## Tổng Kết

✅ **Quan hệ Wallet ↔ User là ĐÚNG và hoạt động tốt**:
- One-to-One relationship được thiết lập đúng
- Constraint UNIQUE đảm bảo 1 user chỉ có 1 wallet
- Cách sử dụng trong code là đúng (userId để persist, user để đọc)
- Auto-create wallet khi tạo user mới hoạt động tốt

🔧 **Có thể cải thiện** (optional):
- Thêm validation và documentation
- Cân nhắc cascade nếu cần

