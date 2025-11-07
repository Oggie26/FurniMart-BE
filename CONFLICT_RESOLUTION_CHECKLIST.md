# CHECKLIST RESOLVE CONFLICTS - branch/phong → main

**Ngày tạo:** $(Get-Date -Format "yyyy-MM-dd")  
**Trạng thái hiện tại:** ⚠️ 10 files bị conflict, tạm thời giữ version từ main

---

## 📋 TỔNG QUAN

- **Merge commit:** `344b61b` - "merge: Merge branch/phong into main - Conflicts temporarily resolved by keeping main version"
- **Files bị conflict:** 10 files
- **Thư mục backup:** `conflicts_backup/`
- **Báo cáo chi tiết:** `CONFLICT_EMAIL_REPORT.md`

---

## ✅ CHECKLIST RESOLVE CONFLICTS

### **1. EnumRole.java Files (2 files)**

#### **order-service/src/main/java/com/example/orderservice/enums/EnumRole.java**
- [ ] Review version từ `main` (có SELLER, DELIVERY_STAFF)
- [ ] Review version từ `branch/phong` (không có SELLER, có DELIVERY)
- [ ] **Action:** Giữ version từ `branch/phong`
- [ ] Update file với version từ `branch/phong`
- [ ] Kiểm tra tất cả references đến `SELLER` và `DELIVERY_STAFF` trong order-service
- [ ] Update các references nếu cần

#### **user-service/src/main/java/com/example/userservice/enums/EnumRole.java**
- [ ] Review version từ `main` (có MANAGER)
- [ ] Review version từ `branch/phong` (có BRANCH_MANAGER)
- [ ] **Action:** Giữ version từ `branch/phong`
- [ ] Update file với version từ `branch/phong`
- [ ] Kiểm tra tất cả references đến `MANAGER` trong user-service
- [ ] Update các references nếu cần

---

### **2. Controller Files (3 files)**

#### **user-service/src/main/java/com/example/userservice/controller/ChatController.java**
- [ ] Review version từ `main` (có thể có @PreAuthorize với SELLER/DELIVERER)
- [ ] Review version từ `branch/phong` (đã cập nhật @PreAuthorize với STAFF/DELIVERY)
- [ ] **Action:** Giữ version từ `branch/phong` cho @PreAuthorize
- [ ] Kiểm tra xem `main` có thay đổi gì khác không (logic, endpoints mới)
- [ ] Merge các thay đổi từ `main` nếu có (ngoài @PreAuthorize)
- [ ] Update file
- [ ] Test endpoints liên quan

#### **user-service/src/main/java/com/example/userservice/controller/ChatMessageController.java**
- [ ] Review version từ `main` (có thể có @PreAuthorize với SELLER/DELIVERER)
- [ ] Review version từ `branch/phong` (đã cập nhật @PreAuthorize với STAFF/DELIVERY)
- [ ] **Action:** Giữ version từ `branch/phong` cho @PreAuthorize
- [ ] Kiểm tra xem `main` có thay đổi gì khác không (logic, endpoints mới)
- [ ] Merge các thay đổi từ `main` nếu có (ngoài @PreAuthorize)
- [ ] Update file
- [ ] Test endpoints liên quan

#### **user-service/src/main/java/com/example/userservice/controller/EmployeeController.java**
- [ ] Review version từ `main` (có thể có endpoints với SELLER/DELIVERER)
- [ ] Review version từ `branch/phong` (đã cập nhật endpoints với STAFF/DELIVERY)
- [ ] **Action:** Giữ version từ `branch/phong` cho endpoints và @PreAuthorize
- [ ] Kiểm tra xem `main` có thay đổi gì khác không (logic, endpoints mới)
- [ ] Merge các thay đổi từ `main` nếu có (ngoài roles)
- [ ] Update file
- [ ] Test endpoints liên quan

---

### **3. Repository Files (1 file)**

#### **user-service/src/main/java/com/example/userservice/repository/EmployeeRepository.java**
- [ ] Review version từ `main` (có thể có queries với SELLER/DELIVERER)
- [ ] Review version từ `branch/phong` (đã cập nhật queries với STAFF/DELIVERY, sử dụng EmployeeStore)
- [ ] **Action:** Giữ logic từ `branch/phong` (queries với EmployeeStore, roles mới)
- [ ] Kiểm tra xem `main` có thay đổi gì khác không (new methods, bug fixes)
- [ ] Merge các thay đổi từ `main` nếu có (ngoài roles và EmployeeStore)
- [ ] Update file
- [ ] Test repository methods

---

### **4. Service Files (4 files)**

#### **user-service/src/main/java/com/example/userservice/service/EmployeeServiceImpl.java**
- [ ] Review version từ `main` (có thể có logic với SELLER/DELIVERER, UserStore)
- [ ] Review version từ `branch/phong` (đã refactor với STAFF/DELIVERY, EmployeeStore)
- [ ] **Action:** Giữ logic từ `branch/phong` (EmployeeStore, roles mới)
- [ ] Kiểm tra xem `main` có thay đổi gì khác không (bug fixes, new features)
- [ ] Merge các thay đổi từ `main` nếu có (ngoài roles và EmployeeStore)
- [ ] Update file
- [ ] Test service methods

#### **user-service/src/main/java/com/example/userservice/service/StoreServiceImpl.java**
- [ ] Review version từ `main` (có thể có logic với UserStore)
- [ ] Review version từ `branch/phong` (đã refactor với EmployeeStore)
- [ ] **Action:** Giữ logic từ `branch/phong` (EmployeeStore)
- [ ] Kiểm tra xem `main` có thay đổi gì khác không (bug fixes, new features)
- [ ] Merge các thay đổi từ `main` nếu có (ngoài EmployeeStore)
- [ ] Update file
- [ ] Test service methods

#### **user-service/src/main/java/com/example/userservice/service/UserServiceImpl.java**
- [ ] Review version từ `main` (có thể có logic tạo employees)
- [ ] Review version từ `branch/phong` (chỉ tạo CUSTOMER, redirect employees sang EmployeeService)
- [ ] **Action:** Giữ logic từ `branch/phong` (chỉ CUSTOMER, redirect employees)
- [ ] Kiểm tra xem `main` có thay đổi gì khác không (bug fixes, new features cho CUSTOMER)
- [ ] Merge các thay đổi từ `main` nếu có (cho CUSTOMER logic)
- [ ] Update file
- [ ] Test service methods

#### **user-service/src/main/java/com/example/userservice/service/inteface/EmployeeService.java**
- [ ] Review version từ `main` (có thể có methods với SELLER/DELIVERER)
- [ ] Review version từ `branch/phong` (đã cập nhật với STAFF/DELIVERY)
- [ ] **Action:** Giữ version từ `branch/phong` (roles mới)
- [ ] Kiểm tra xem `main` có thay đổi gì khác không (new methods)
- [ ] Merge các thay đổi từ `main` nếu có (new methods)
- [ ] Update file
- [ ] Verify implementation matches interface

---

## 🧪 TESTING CHECKLIST

### **Compile & Build**
- [ ] Compile user-service thành công
- [ ] Compile order-service thành công
- [ ] Compile tất cả services thành công
- [ ] Build Docker images thành công (nếu cần)

### **Unit Tests**
- [ ] Chạy unit tests cho EmployeeService
- [ ] Chạy unit tests cho UserService
- [ ] Chạy unit tests cho StoreService
- [ ] Chạy unit tests cho Controllers
- [ ] Tất cả unit tests pass

### **Integration Tests**
- [ ] Test tạo Employee với các roles mới (STAFF, DELIVERY, BRANCH_MANAGER)
- [ ] Test tạo CUSTOMER (không được tạo Employee)
- [ ] Test EmployeeStore relationships
- [ ] Test @PreAuthorize với roles mới
- [ ] Test endpoints với roles mới
- [ ] Test không thể tạo SELLER hoặc DELIVERY_STAFF (nếu còn code cũ)

### **Manual Testing**
- [ ] Test tạo employee qua EmployeeService
- [ ] Test tạo customer qua UserService
- [ ] Test assign employee to store
- [ ] Test remove employee from store
- [ ] Test get employees by store
- [ ] Test get stores by employee
- [ ] Test authentication với roles mới
- [ ] Test authorization với @PreAuthorize

---

## 📝 COMMIT & PUSH

### **Sau khi resolve conflicts:**
- [ ] Review tất cả thay đổi: `git diff`
- [ ] Add các file đã resolve: `git add .`
- [ ] Commit với message rõ ràng: `git commit -m "resolve: Resolve merge conflicts from branch/phong, apply refactored roles and EmployeeStore"`
- [ ] Test lại một lần nữa
- [ ] Push lên remote: `git push origin main`

---

## ⚠️ LƯU Ý QUAN TRỌNG

1. **Không mất thay đổi từ main:**
   - Main có nhiều commits về COD payment và ai-service
   - Cần đảm bảo không mất các thay đổi này khi resolve conflicts

2. **Giữ logic từ branch/phong:**
   - Branch/phong đã refactor lớn về roles và EmployeeStore
   - Cần đảm bảo tất cả logic mới được giữ lại

3. **Test kỹ sau khi resolve:**
   - Đặc biệt test các chức năng liên quan đến roles
   - Test store relationships (EmployeeStore)
   - Test authentication và authorization

4. **Backup trước khi thay đổi:**
   - Các file conflict đã được lưu trong `conflicts_backup/`
   - Có thể tham khảo khi resolve

---

## 📊 TRẠNG THÁI

- [ ] **Chưa bắt đầu**
- [ ] **Đang resolve conflicts**
- [ ] **Đã resolve xong, đang test**
- [ ] **Đã test xong, sẵn sàng commit**
- [ ] **Đã commit và push**

---

**Cập nhật lần cuối:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

