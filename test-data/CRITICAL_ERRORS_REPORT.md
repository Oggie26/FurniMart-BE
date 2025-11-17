# Báo Cáo Các Lỗi Nghiêm Trọng Cần Sửa

## 📋 Tổng Quan

Sau khi kiểm tra code, phát hiện **NHIỀU LỖI NGHIÊM TRỌNG** có thể gây:
- ❌ **Từ chối tạo data** (User, Wallet)
- ❌ **Không thể đăng nhập**
- ❌ **Lỗi runtime** khi đăng ký
- ❌ **Lỗi mapping** JPA/Hibernate

---

## 🔴 LỖI NGHIÊM TRỌNG - CẦN SỬA NGAY

### 1. ❌ WalletService Bị Comment Hoàn Toàn

**File**: `WalletServiceImpl.java`, `WalletController.java`, `WalletRepository.java`

**Vấn đề**:
- Tất cả code Wallet service bị comment (//)
- WalletService interface vẫn tồn tại và được inject
- Các service khác vẫn gọi `walletService.createWalletForUser()`

**Ảnh hưởng**:
- ❌ **Runtime Error**: `NoSuchBeanDefinitionException` hoặc `NullPointerException`
- ❌ **Đăng ký thất bại**: User được tạo nhưng wallet không được tạo
- ❌ **Đăng nhập có thể lỗi**: Nếu code phụ thuộc vào wallet

**Nơi bị ảnh hưởng**:
```java
// AuthServiceImpl.java - line 84
walletService.createWalletForUser(savedUser.getId());  // ❌ Sẽ lỗi

// UserServiceImpl.java - line 89
walletService.createWalletForUser(savedUser.getId());  // ❌ Sẽ lỗi

// GoogleOAuth2Service.java - line 151
walletService.createWalletForUser(savedUser.getId());  // ❌ Sẽ lỗi
```

**Giải pháp**: 
- [ ] Uncomment tất cả Wallet service files
- [ ] Hoặc implement lại WalletService
- [ ] Hoặc remove dependency nếu không cần wallet

---

### 2. ❌ Wallet Entity Mapping Sai

**File**: `Wallet.java`

**Vấn đề**:
```java
// ❌ HIỆN TẠI (SAI):
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")  // ⚠️ Không có insertable=false, updatable=false
private User user;  // ⚠️ Bây giờ là owning side

// ❌ KHÔNG CÒN:
// private String userId;  // Đã bị xóa
```

**Vấn đề với mapping mới**:
1. **Wallet là owning side** với `@JoinColumn(name = "user_id")`
2. **User là inverse side** với `mappedBy = "user"`
3. **Nhưng**: Khi persist Wallet, cần set `user` entity (không phải `userId` string)
4. **Code cũ** (đã comment) vẫn sử dụng `userId` string → **Không tương thích**

**Ảnh hưởng**:
- ❌ **Không thể tạo Wallet**: Code cũ dùng `Wallet.builder().userId(userId)` → **Lỗi compile**
- ❌ **Không thể query**: Repository methods dùng `findByUserId()` → **Lỗi compile**
- ❌ **Mapping conflict**: Wallet và User đều có thể là owning side → **Lỗi runtime**

**Giải pháp**:
- [ ] **Option 1**: Thêm lại field `userId` (String) và giữ `user` (User entity) với `insertable=false, updatable=false`
- [ ] **Option 2**: Sửa tất cả code để dùng `user` entity thay vì `userId` string

---

### 3. ❌ WalletRepository Bị Comment

**File**: `WalletRepository.java`

**Vấn đề**:
- Tất cả methods bị comment
- WalletServiceImpl (đã comment) vẫn sử dụng các methods này

**Methods bị ảnh hưởng**:
```java
// ❌ Tất cả đều bị comment:
// findByCodeAndIsDeletedFalse(String code)
// findByUserIdAndIsDeletedFalse(String userId)  // ⚠️ Dùng userId nhưng entity không có
// findByIdAndIsDeletedFalse(String id)
// existsByCodeAndIsDeletedFalse(String code)
// existsByUserIdAndIsDeletedFalse(String userId)  // ⚠️ Dùng userId nhưng entity không có
// findByUserId(String userId)  // ⚠️ Dùng userId nhưng entity không có
```

**Ảnh hưởng**:
- ❌ **Không thể query wallet**: Tất cả repository methods không hoạt động
- ❌ **Lỗi compile**: Nếu uncomment WalletServiceImpl, sẽ lỗi vì methods không tồn tại

**Giải pháp**:
- [ ] Uncomment WalletRepository
- [ ] Sửa methods để query theo `user.id` thay vì `userId` (nếu giữ mapping mới)
- [ ] Hoặc thêm lại field `userId` vào Wallet entity

---

### 4. ⚠️ Wallet-User Relationship Mapping Conflict

**Vấn đề**:
```java
// Wallet.java
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")  // ⚠️ Wallet là owning side
private User user;

// User.java
@OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
private Wallet wallet;  // ✅ User là inverse side
```

**Vấn đề tiềm ẩn**:
1. **Wallet có `@JoinColumn`** → Wallet là owning side
2. **User có `mappedBy`** → User là inverse side
3. **Nhưng**: Khi tạo Wallet, cần set `user` entity (phải load User trước)
4. **Constraint**: `UNIQUE(user_id)` trong Wallet table → Đúng
5. **Nhưng**: Nếu Wallet không có `insertable=false, updatable=false`, có thể gây vấn đề khi persist

**Ảnh hưởng**:
- ⚠️ **Có thể lỗi khi persist**: Nếu User chưa được persist trước Wallet
- ⚠️ **Lazy loading issues**: Nếu không set user đúng cách

**Giải pháp**:
- [ ] Thêm `insertable = false, updatable = false` cho `user` field trong Wallet
- [ ] Thêm lại field `userId` (String) để persist foreign key
- [ ] Hoặc đảm bảo User được persist trước Wallet

---

### 5. ⚠️ Account-User-Employee Relationships

**Vấn đề tiềm ẩn**:
```java
// Account.java
@OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
private User user;

@OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
private Employee employee;

// User.java
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "account_id", nullable = false)
private Account account;  // ✅ User là owning side

// Employee.java
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "account_id", nullable = false)
private Account account;  // ✅ Employee là owning side
```

**Vấn đề**:
- Account có **2 inverse sides** (User và Employee)
- User và Employee đều là **owning side** với `@JoinColumn(name = "account_id")`
- **Cascade**: Account có `cascade = CascadeType.ALL` cho cả User và Employee
- **Nhưng**: User và Employee không thể cùng tồn tại cho 1 Account (theo logic)

**Ảnh hưởng**:
- ⚠️ **Có thể gây confusion**: Account có 2 relationships nhưng chỉ 1 active
- ✅ **Mapping đúng**: User và Employee đều có foreign key đến Account

**Giải pháp**:
- [ ] **Giữ nguyên** (mapping đúng, chỉ cần đảm bảo logic đúng)

---

## 📊 Tổng Hợp Các Lỗi

### 🔴 Lỗi Nghiêm Trọng (Phải sửa ngay):

1. **WalletService bị comment** → Gây lỗi runtime khi đăng ký/đăng nhập
2. **Wallet entity không có userId** → Code cũ không tương thích
3. **WalletRepository bị comment** → Không thể query wallet
4. **Wallet-User mapping conflict** → Có thể lỗi khi persist

### ⚠️ Lỗi Tiềm Ẩn (Nên sửa):

5. **Account-User-Employee relationships** → Có thể gây confusion nhưng mapping đúng

---

## 🔧 Checklist Sửa Lỗi

### Bước 1: Sửa Wallet Entity
- [ ] **Option A**: Thêm lại field `userId` (String)
  ```java
  @Column(name = "user_id", nullable = false)
  private String userId;
  
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private User user;
  ```

- [ ] **Option B**: Giữ mapping mới và sửa tất cả code
  - Sửa WalletServiceImpl để dùng `user` entity
  - Sửa WalletRepository methods
  - Sửa tất cả code sử dụng `userId` → `user.id`

### Bước 2: Uncomment Wallet Service
- [ ] Uncomment `WalletRepository.java`
- [ ] Uncomment `WalletServiceImpl.java`
- [ ] Uncomment `WalletController.java`
- [ ] Sửa code để tương thích với mapping mới

### Bước 3: Test
- [ ] Test đăng ký user mới
- [ ] Test đăng nhập
- [ ] Test tạo wallet
- [ ] Test wallet operations (deposit, withdraw, transfer)

---

## 💥 Kịch Bản Lỗi Nếu Không Sửa

### Kịch Bản 1: Đăng Ký User Mới
```
1. User gọi POST /api/auth/register
2. AuthServiceImpl.register() được gọi
3. Account và User được tạo thành công
4. walletService.createWalletForUser() được gọi
5. ❌ LỖI: WalletService không tồn tại (bị comment)
6. Exception được catch, user được tạo nhưng không có wallet
7. ⚠️ User có thể đăng nhập nhưng không có wallet
```

### Kịch Bản 2: Đăng Nhập
```
1. User gọi POST /api/auth/login
2. AuthServiceImpl.login() được gọi
3. ✅ Đăng nhập thành công (không phụ thuộc wallet)
4. ⚠️ Nhưng nếu code khác cần wallet → Lỗi
```

### Kịch Bản 3: Tạo Wallet Thủ Công
```
1. Admin gọi POST /api/wallets
2. ❌ LỖI: WalletController bị comment → Endpoint không tồn tại
3. 404 Not Found
```

---

## 🎯 Ưu Tiên Sửa Lỗi

### 🔴 Ưu Tiên 1 (Ngay lập tức):
1. **Uncomment WalletService** hoặc implement lại
2. **Sửa Wallet entity mapping** (thêm lại userId hoặc sửa code)

### ⚠️ Ưu Tiên 2 (Sớm):
3. **Test đăng ký/đăng nhập**
4. **Test wallet operations**

### ✅ Ưu Tiên 3 (Sau):
5. **Review Account-User-Employee relationships** (nếu cần)

---

## 📝 Kết Luận

**Trạng thái hiện tại**: 
- ❌ **Wallet Service KHÔNG HOẠT ĐỘNG** (bị comment)
- ❌ **Wallet Entity mapping KHÔNG TƯƠNG THÍCH** với code cũ
- ⚠️ **Đăng ký/Đăng nhập CÓ THỂ HOẠT ĐỘNG** nhưng wallet không được tạo

**Hành động cần thiết**:
1. **Quyết định**: Giữ mapping mới hay revert về cũ
2. **Uncomment** hoặc **implement lại** Wallet service
3. **Test** toàn bộ flow đăng ký/đăng nhập/wallet

