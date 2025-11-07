# BÁO CÁO XUNG ĐỘT KHI MERGE branch/phong VÀO main

**Ngày:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")  
**Người thực hiện:** Git Merge Process  
**Repository:** FurniMart-BE  
**Branch nguồn:** branch/phong  
**Branch đích:** main  

---

## 📊 TÓM TẮT

Khi merge `branch/phong` vào `main`, phát hiện **10 files bị conflict**. Các conflicts đã được tạm thời resolve bằng cách giữ nguyên version từ `main`. Các file conflict đã được lưu vào thư mục `conflicts_backup/` để review sau.

**Trạng thái hiện tại:** ✅ Merge đã hoàn thành, các conflicts tạm thời được giữ nguyên từ main

---

## 🔍 CHI TIẾT CÁC FILES BỊ CONFLICT

### **1. EnumRole.java Files (2 files)**

#### **order-service/src/main/java/com/example/orderservice/enums/EnumRole.java**

**Version trong main:**
```java
public enum EnumRole {
    ADMIN,
    CUSTOMER,
    BRANCH_MANAGER,
    SELLER,
    DELIVERY_STAFF,
    STAFF
}
```

**Version trong branch/phong:**
```java
public enum EnumRole {
    ADMIN,
    CUSTOMER,
    BRANCH_MANAGER,
    STAFF,
    DELIVERY
}
```

**Khác biệt:**
- ❌ Main còn có `SELLER` và `DELIVERY_STAFF`
- ✅ Branch/phong đã loại bỏ `SELLER` và đổi `DELIVERY_STAFF` → `DELIVERY`

**Giải pháp đề xuất:** Giữ version từ `branch/phong` (loại bỏ SELLER, đổi DELIVERY_STAFF → DELIVERY)

---

#### **user-service/src/main/java/com/example/userservice/enums/EnumRole.java**

**Version trong main:**
```java
public enum EnumRole {
    ADMIN,
    CUSTOMER,
    DELIVERY,
    MANAGER,
    STAFF,
}
```

**Version trong branch/phong:**
```java
public enum EnumRole {
    ADMIN,
    CUSTOMER,
    DELIVERY,
    BRANCH_MANAGER,
    STAFF,
}
```

**Khác biệt:**
- ❌ Main có `MANAGER`
- ✅ Branch/phong có `BRANCH_MANAGER` (thay thế MANAGER)

**Giải pháp đề xuất:** Giữ version từ `branch/phong` (MANAGER → BRANCH_MANAGER)

---

### **2. Controller Files (3 files)**

#### **user-service/src/main/java/com/example/userservice/controller/ChatController.java**
- **Nguyên nhân:** Branch/phong đã cập nhật `@PreAuthorize` annotations từ `SELLER` → `STAFF` và `DELIVERER/DELIVERY_STAFF` → `DELIVERY`
- **Giải pháp đề xuất:** Giữ version từ `branch/phong`, nhưng merge các thay đổi khác từ `main` nếu có

#### **user-service/src/main/java/com/example/userservice/controller/ChatMessageController.java**
- **Nguyên nhân:** Tương tự ChatController
- **Giải pháp đề xuất:** Giữ version từ `branch/phong`, nhưng merge các thay đổi khác từ `main` nếu có

#### **user-service/src/main/java/com/example/userservice/controller/EmployeeController.java**
- **Nguyên nhân:** Branch/phong đã cập nhật endpoints và annotations liên quan đến roles
- **Giải pháp đề xuất:** Giữ version từ `branch/phong`, nhưng merge các thay đổi khác từ `main` nếu có

---

### **3. Service & Repository Files (5 files)**

#### **user-service/src/main/java/com/example/userservice/repository/EmployeeRepository.java**
- **Nguyên nhân:** Branch/phong đã refactor queries để sử dụng `EmployeeStore` thay vì `UserStore`, và cập nhật role filters
- **Giải pháp đề xuất:** Giữ logic từ `branch/phong`, nhưng merge các thay đổi từ `main` nếu có

#### **user-service/src/main/java/com/example/userservice/service/EmployeeServiceImpl.java**
- **Nguyên nhân:** Branch/phong đã refactor toàn bộ logic để sử dụng `Employee` entity và `EmployeeStore`
- **Giải pháp đề xuất:** Giữ logic từ `branch/phong`, nhưng merge các thay đổi từ `main` nếu có

#### **user-service/src/main/java/com/example/userservice/service/StoreServiceImpl.java**
- **Nguyên nhân:** Branch/phong đã refactor để sử dụng `EmployeeStore` thay vì `UserStore`
- **Giải pháp đề xuất:** Giữ logic từ `branch/phong`, nhưng merge các thay đổi từ `main` nếu có

#### **user-service/src/main/java/com/example/userservice/service/UserServiceImpl.java**
- **Nguyên nhân:** Branch/phong đã refactor để chỉ xử lý CUSTOMER users, redirect employee operations sang EmployeeService
- **Giải pháp đề xuất:** Giữ logic từ `branch/phong`, nhưng merge các thay đổi từ `main` nếu có

#### **user-service/src/main/java/com/example/userservice/service/inteface/EmployeeService.java**
- **Nguyên nhân:** Branch/phong đã cập nhật interface để phản ánh các thay đổi về roles
- **Giải pháp đề xuất:** Giữ version từ `branch/phong`, nhưng merge các thay đổi từ `main` nếu có

---

## 📈 PHÂN TÍCH NGUYÊN NHÂN

### **Thay đổi trong branch/phong:**
1. ✅ Loại bỏ `SELLER` role (thay thế bằng `STAFF`)
2. ✅ Đổi `DELIVERER` và `DELIVERY_STAFF` thành `DELIVERY`
3. ✅ Đổi `MANAGER` thành `BRANCH_MANAGER`
4. ✅ Refactor từ `UserStore` → `EmployeeStore`
5. ✅ Tách biệt `User` (CUSTOMER) và `Employee` entities

### **Thay đổi trong main:**
1. ✅ Nhiều commits về COD payment fixes
2. ✅ Nhiều commits về ai-service
3. ✅ Có thể có các thay đổi khác trong cùng các files bị conflict

### **Nguyên nhân conflict:**
- Branch/phong đã refactor lớn về roles và entities
- Main có các thay đổi khác trong cùng các files
- Cả hai branches đều thay đổi cùng các files nhưng theo hướng khác nhau

---

## 💡 GIẢI PHÁP ĐỀ XUẤT

### **Bước 1: Review các file conflict**
- Xem các file đã được lưu trong `conflicts_backup/`
- So sánh version từ `main` và `branch/phong`

### **Bước 2: Resolve conflicts thủ công**
- **EnumRole.java:** Giữ version từ `branch/phong` (đã loại bỏ SELLER, đổi DELIVERY_STAFF → DELIVERY, MANAGER → BRANCH_MANAGER)
- **Controllers:** Giữ version từ `branch/phong` (đã cập nhật @PreAuthorize), nhưng merge các thay đổi khác từ `main` nếu có
- **Services/Repositories:** Giữ logic từ `branch/phong` (đã refactor EmployeeStore), nhưng merge các thay đổi từ `main` nếu có

### **Bước 3: Test sau khi resolve**
- Compile tất cả services
- Chạy unit tests
- Test integration với các services khác
- Đặc biệt test các chức năng liên quan đến:
  - Roles (SELLER, DELIVERY_STAFF, DELIVERY, MANAGER, BRANCH_MANAGER)
  - Store relationships (EmployeeStore)
  - Employee management

---

## 📋 CHECKLIST RESOLVE CONFLICTS

### **EnumRole.java Files:**
- [ ] Review version từ `branch/phong` và `main`
- [ ] Giữ version từ `branch/phong` (không có SELLER, có DELIVERY, có BRANCH_MANAGER)
- [ ] Đảm bảo thứ tự enum values nhất quán
- [ ] Update tất cả references đến các roles đã thay đổi

### **Controller Files:**
- [ ] Review version từ `branch/phong` và `main`
- [ ] Giữ version từ `branch/phong` (đã cập nhật @PreAuthorize)
- [ ] Kiểm tra xem `main` có thay đổi gì khác không (như logic, endpoints mới)
- [ ] Merge các thay đổi từ `main` nếu có

### **Service/Repository Files:**
- [ ] Review version từ `branch/phong` và `main`
- [ ] Giữ logic EmployeeStore từ `branch/phong`
- [ ] Merge các thay đổi từ `main` nếu có (như bug fixes, new features)
- [ ] Đảm bảo không mất các thay đổi quan trọng từ `main`

### **Sau khi resolve:**
- [ ] Compile và test tất cả services
- [ ] Chạy unit tests
- [ ] Test integration với các services khác
- [ ] Review code một lần nữa
- [ ] Commit và push

---

## ⚠️ LƯU Ý QUAN TRỌNG

1. **Main có nhiều commits về COD payment và ai-service** - Cần đảm bảo không mất các thay đổi này khi resolve conflicts
2. **Branch/phong đã refactor lớn** - Cần đảm bảo tất cả logic mới được giữ lại
3. **Nên test kỹ sau khi resolve conflicts** - Đặc biệt là các chức năng liên quan đến roles và store relationships
4. **Các file conflict đã được lưu trong `conflicts_backup/`** - Có thể tham khảo khi resolve

---

## 📁 FILES ĐÃ ĐƯỢC LƯU

Các file conflict đã được lưu vào thư mục `conflicts_backup/`:
- `order-service_EnumRole_main.java` - Version từ main
- `order-service_EnumRole_branch-phong.java` - Version từ branch/phong
- `order-service_EnumRole_diff.txt` - Diff giữa 2 versions
- (Các file khác sẽ được lưu tương tự)

---

## ✅ KẾT LUẬN

- **Có 10 files bị conflict** khi merge branch/phong vào main
- **Nguyên nhân chính:** Branch/phong đã refactor lớn (loại bỏ SELLER, đổi DELIVERER → DELIVERY, thay UserStore → EmployeeStore) trong khi main có các thay đổi khác
- **Trạng thái hiện tại:** Merge đã hoàn thành, các conflicts tạm thời được giữ nguyên từ main
- **Cần action:** Review và resolve conflicts thủ công theo hướng dẫn trên

---

**Liên hệ:** Nếu có thắc mắc về các conflicts này, vui lòng liên hệ team phát triển.

