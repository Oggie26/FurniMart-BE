# BÁO CÁO XUNG ĐỘT GIỮA branch/phong VÀ main

## 📊 TÓM TẮT

### ⚠️ **CÓ XUNG ĐỘT**

Khi merge `origin/main` vào `branch/phong`, phát hiện **10 files bị conflict**.

---

## 🔍 CHI TIẾT XUNG ĐỘT

### **Files bị Conflict:**

1. ✅ `order-service/src/main/java/com/example/orderservice/enums/EnumRole.java`
2. ✅ `user-service/src/main/java/com/example/userservice/controller/ChatController.java`
3. ✅ `user-service/src/main/java/com/example/userservice/controller/ChatMessageController.java`
4. ✅ `user-service/src/main/java/com/example/userservice/controller/EmployeeController.java`
5. ✅ `user-service/src/main/java/com/example/userservice/enums/EnumRole.java`
6. ✅ `user-service/src/main/java/com/example/userservice/repository/EmployeeRepository.java`
7. ✅ `user-service/src/main/java/com/example/userservice/service/EmployeeServiceImpl.java`
8. ✅ `user-service/src/main/java/com/example/userservice/service/StoreServiceImpl.java`
9. ✅ `user-service/src/main/java/com/example/userservice/service/UserServiceImpl.java`
10. ✅ `user-service/src/main/java/com/example/userservice/service/inteface/EmployeeService.java`

---

## 📈 PHÂN TÍCH

### **Commits trong branch/phong (không có trong main):**
- `0b7f0bd` - refactor: Remove SELLER role, rename DELIVERER/DELIVERY_STAFF to DELIVERY, replace UserStore with EmployeeStore
- `6d09ca1` - new commit

### **Commits trong main (không có trong branch/phong):**
- `672a92b` - Fix COD payment
- `a713a77` - Fix COD payment
- `8d0d2be` - fix ai-service
- `66b5d74` - Fix COD payment
- `2c037a7` - Fix COD payment
- `5def21f` - Fix COD payment
- `7e113ba` - fix ai-service
- ... (nhiều commits khác về ai-service và COD payment)

### **Common Ancestor:**
- `bedf9c5bc75193e8dfee0c7acad6527c83ff229c`

---

## 🔎 NGUYÊN NHÂN XUNG ĐỘT

### **1. EnumRole.java Conflicts**

**Nguyên nhân:**
- `branch/phong` đã loại bỏ `SELLER` và đổi `DELIVERER/DELIVERY_STAFF` thành `DELIVERY`, có `BRANCH_MANAGER`
- `main` vẫn còn:
  - `order-service`: `SELLER`, `DELIVERY_STAFF`, `STAFF` (có cả 3)
  - `user-service`: `DELIVERY`, `MANAGER` (không có `BRANCH_MANAGER`)

**Chi tiết conflicts:**

**order-service EnumRole:**
- **Main:** `ADMIN, CUSTOMER, BRANCH_MANAGER, SELLER, DELIVERY_STAFF, STAFF`
- **Branch/phong:** `ADMIN, CUSTOMER, BRANCH_MANAGER, STAFF, DELIVERY`
- **Giải pháp:** Giữ version từ `branch/phong` (loại bỏ SELLER, đổi DELIVERY_STAFF → DELIVERY)

**user-service EnumRole:**
- **Main:** `ADMIN, CUSTOMER, DELIVERY, MANAGER, STAFF`
- **Branch/phong:** `ADMIN, CUSTOMER, DELIVERY, BRANCH_MANAGER, STAFF`
- **Giải pháp:** Giữ version từ `branch/phong` (MANAGER → BRANCH_MANAGER)

**Files bị ảnh hưởng:**
- `order-service/src/main/java/com/example/orderservice/enums/EnumRole.java`
- `user-service/src/main/java/com/example/userservice/enums/EnumRole.java`

### **2. Controller Conflicts**

**Nguyên nhân:**
- `branch/phong` đã cập nhật `@PreAuthorize` annotations từ `SELLER` → `STAFF` và `DELIVERER/DELIVERY_STAFF` → `DELIVERY`
- `main` có thể vẫn còn các annotations cũ

**Files bị ảnh hưởng:**
- `ChatController.java`
- `ChatMessageController.java`
- `EmployeeController.java`

### **3. Service & Repository Conflicts**

**Nguyên nhân:**
- `branch/phong` đã refactor toàn bộ logic từ `UserStore` → `EmployeeStore`
- `branch/phong` đã cập nhật tất cả queries và methods liên quan đến roles
- `main` có thể có các thay đổi khác trong cùng các files này

**Files bị ảnh hưởng:**
- `EmployeeRepository.java`
- `EmployeeServiceImpl.java`
- `StoreServiceImpl.java`
- `UserServiceImpl.java`
- `EmployeeService.java` (interface)

---

## 💡 GIẢI PHÁP

### **Option 1: Merge main vào branch/phong (Khuyến nghị)**

**Bước 1:** Merge main vào branch/phong
```bash
git checkout branch/phong
git merge origin/main
```

**Bước 2:** Resolve conflicts
- Với `EnumRole.java`: Giữ version từ `branch/phong` (đã loại bỏ SELLER, đổi DELIVERER → DELIVERY)
- Với Controllers: Giữ version từ `branch/phong` (đã cập nhật @PreAuthorize)
- Với Services/Repositories: Cần merge thủ công, giữ logic từ `branch/phong` nhưng merge các thay đổi từ `main` nếu có

**Bước 3:** Test và commit
```bash
# Sau khi resolve conflicts
git add .
git commit -m "merge: Merge main into branch/phong, resolve conflicts"
```

### **Option 2: Rebase branch/phong lên main**

**Bước 1:** Rebase
```bash
git checkout branch/phong
git rebase origin/main
```

**Bước 2:** Resolve conflicts tương tự như Option 1

**Bước 3:** Force push (nếu đã push trước đó)
```bash
git push origin branch/phong --force-with-lease
```

⚠️ **Lưu ý:** Rebase sẽ rewrite history, cần cẩn thận nếu có người khác đang làm việc trên branch này.

---

## 📋 CHECKLIST RESOLVE CONFLICTS

### **EnumRole.java Files:**
- [ ] Giữ version từ `branch/phong` (không có SELLER, có DELIVERY)
- [ ] Đảm bảo thứ tự enum values nhất quán

### **Controller Files:**
- [ ] Giữ version từ `branch/phong` (đã cập nhật @PreAuthorize)
- [ ] Kiểm tra xem `main` có thay đổi gì khác không (như logic, endpoints mới)

### **Service/Repository Files:**
- [ ] Giữ logic EmployeeStore từ `branch/phong`
- [ ] Merge các thay đổi từ `main` nếu có (như bug fixes, new features)
- [ ] Đảm bảo không mất các thay đổi quan trọng từ `main`

### **Sau khi resolve:**
- [ ] Compile và test tất cả services
- [ ] Chạy unit tests
- [ ] Test integration với các services khác
- [ ] Review code một lần nữa

---

## ⚠️ LƯU Ý QUAN TRỌNG

1. **Main có nhiều commits về COD payment và ai-service** - Cần đảm bảo không mất các thay đổi này khi merge
2. **Branch/phong đã refactor lớn** - Cần đảm bảo tất cả logic mới được giữ lại
3. **Nên test kỹ sau khi resolve conflicts** - Đặc biệt là các chức năng liên quan đến roles và store relationships

---

## ✅ KẾT LUẬN

- **Có 10 files bị conflict** khi merge main vào branch/phong
- **Nguyên nhân chính:** Branch/phong đã refactor lớn (loại bỏ SELLER, đổi DELIVERER → DELIVERY, thay UserStore → EmployeeStore) trong khi main có các thay đổi khác
- **Giải pháp:** Merge main vào branch/phong và resolve conflicts thủ công, ưu tiên giữ logic từ branch/phong nhưng merge các thay đổi từ main nếu có

