# Hướng Dẫn Test Đầy Đủ - DELIVERY và STAFF Functions

## 📋 Tổng Quan

Tài liệu này hướng dẫn test **tất cả** các chức năng còn lại của DELIVERY và STAFF roles.

---

## 🔴 Giải Thích Lỗi 400 Bad Request

### Nguyên Nhân Chính:

1. **CODE_EXISTED (400)**: 
   - Order đã được assign/prepare/generate invoice
   - **Giải pháp**: Kiểm tra trạng thái trước khi thực hiện

2. **INVALID_REQUEST (400)**:
   - Stock không đủ khi prepare products
   - Validation failed (thiếu required fields)

3. **CODE_NOT_FOUND (404)**:
   - Order/Assignment không tồn tại

**Xem chi tiết trong**: `ERROR_EXPLANATION.md`

---

## 🔴 Giải Thích Lỗi 500 từ Order-Service

### Nguyên Nhân Có Thể:

1. **Order-service không khởi động**
2. **Database connection issue**
3. **Feign Client timeout**
4. **NullPointerException hoặc Exception khác**

**Cách Debug:**
```bash
# Kiểm tra order-service
docker ps | grep order-service

# Xem logs
docker logs order-service --tail 100

# Test trực tiếp endpoint
curl -X GET "http://152.53.227.115:8087/api/orders/search?keyword=&page=0&size=10" \
  -H "Authorization: Bearer {TOKEN}"
```

**Xem chi tiết trong**: `ERROR_EXPLANATION.md`

---

## 🚀 Test DELIVERY Functions Còn Lại

### Script: `test-delivery-confirmation.ps1`

**Chức năng test:**

1. ✅ **Login as DELIVERY**
2. ✅ **Get Delivery Staff ID**
3. ✅ **Get Delivery Assignments by Staff**
4. ✅ **Create Delivery Confirmation** (MỚI)
   - Endpoint: `POST /api/delivery-confirmations`
   - Tạo confirmation với photos và notes
5. ✅ **Get Delivery Confirmations by Staff** (MỚI)
   - Endpoint: `GET /api/delivery-confirmations/staff/{deliveryStaffId}`
6. ✅ **Get Delivery Confirmation by Order ID** (MỚI)
   - Endpoint: `GET /api/delivery-confirmations/order/{orderId}`

**Cách chạy:**
```powershell
cd test-data
.\test-delivery-confirmation.ps1
```

**Lưu ý:**
- Cần có assignment được assign cho delivery staff trước
- Assignment phải ở trạng thái `IN_TRANSIT` hoặc `DELIVERED`

---

## 🚀 Test STAFF Functions Còn Lại

### Script: `test-staff-remaining-functions.ps1`

**Chức năng test:**

1. ✅ **Login as STAFF**
2. ✅ **Get Stores**
3. ✅ **Get Delivery Assignment by Order ID** (MỚI - Chi tiết hơn)
   - Endpoint: `GET /api/delivery/assignments/order/{orderId}`
   - Kiểm tra các flags: `invoiceGenerated`, `productsPrepared`
4. ✅ **Get Delivery Assignments by Store** (Đã test nhưng chi tiết hơn)
   - Endpoint: `GET /api/delivery/assignments/store/{storeId}`
5. ✅ **Generate Invoice** (Với kiểm tra trước)
   - Endpoint: `POST /api/delivery/generate-invoice/{orderId}`
   - Kiểm tra `invoiceGenerated` flag trước khi generate
6. ✅ **Prepare Products** (Với kiểm tra trước)
   - Endpoint: `POST /api/delivery/prepare-products`
   - Kiểm tra `productsPrepared` flag trước khi prepare

**Cách chạy:**
```powershell
cd test-data
.\test-staff-remaining-functions.ps1
```

**Cải tiến:**
- Script tự động kiểm tra trạng thái trước khi thực hiện
- Tránh lỗi 400 do duplicate operations
- Hiển thị thông tin chi tiết hơn

---

## 📊 Danh Sách Đầy Đủ Các Endpoints

### DELIVERY Role:

| Endpoint | Method | Mô tả | Đã Test |
|----------|--------|-------|---------|
| `/api/delivery/assignments/staff/{deliveryStaffId}` | GET | Lấy assignments của delivery staff | ✅ |
| `/api/delivery/assignments/{assignmentId}/status` | PUT | Update delivery status | ✅ |
| `/api/delivery-confirmations` | POST | Tạo delivery confirmation | ⏳ MỚI |
| `/api/delivery-confirmations/staff/{deliveryStaffId}` | GET | Lấy confirmations của staff | ⏳ MỚI |
| `/api/delivery-confirmations/order/{orderId}` | GET | Lấy confirmation theo order | ⏳ MỚI |

### STAFF Role:

| Endpoint | Method | Mô tả | Đã Test |
|----------|--------|-------|---------|
| `/api/delivery/assign` | POST | Assign order to delivery | ✅ |
| `/api/delivery/generate-invoice/{orderId}` | POST | Generate invoice | ✅ (Cải tiến) |
| `/api/delivery/prepare-products` | POST | Prepare products | ✅ (Cải tiến) |
| `/api/delivery/assignments/store/{storeId}` | GET | Lấy assignments theo store | ✅ (Cải tiến) |
| `/api/delivery/assignments/order/{orderId}` | GET | Lấy assignment theo order | ⏳ MỚI |

---

## 🎯 Workflow Test Hoàn Chỉnh

### 1. Setup (Chạy 1 lần):
```powershell
# Tạo test accounts
.\create-test-accounts-simple.ps1
```

### 2. Test STAFF Functions:
```powershell
# Test assign order
.\test-assign-order-delivery.ps1

# Test các chức năng STAFF còn lại
.\test-staff-remaining-functions.ps1
```

### 3. Test DELIVERY Functions:
```powershell
# Test các chức năng DELIVERY cơ bản
.\test-delivery-functions.ps1

# Test delivery confirmation (MỚI)
.\test-delivery-confirmation.ps1
```

### 4. Test BRANCH_MANAGER Functions:
```powershell
.\test-branch-manager-functions.ps1
```

---

## 📝 Lưu Ý Khi Test

### 1. Thứ Tự Test Quan Trọng:

1. **Assign Order** (STAFF) → Tạo assignment
2. **Generate Invoice** (STAFF) → Đánh dấu invoice đã generate
3. **Prepare Products** (STAFF) → Đánh dấu products đã prepare
4. **Update Status** (BRANCH_MANAGER/DELIVERY) → Chuyển sang IN_TRANSIT
5. **Create Confirmation** (DELIVERY) → Xác nhận đã giao hàng

### 2. Tránh Lỗi 400:

- **Luôn kiểm tra trạng thái** trước khi thực hiện operation
- **Sử dụng GET endpoints** để verify trạng thái
- **Kiểm tra flags**: `invoiceGenerated`, `productsPrepared`

### 3. Debug Tips:

- Xem logs của service khi có lỗi
- Kiểm tra response body để xem error message
- Sử dụng Swagger UI để test thủ công

---

## ✅ Checklist Test

### DELIVERY Functions:
- [x] Login as DELIVERY
- [x] Get assignments by staff
- [x] Update delivery status
- [ ] **Create delivery confirmation** ← MỚI
- [ ] **Get confirmations by staff** ← MỚI
- [ ] **Get confirmation by order** ← MỚI

### STAFF Functions:
- [x] Login as STAFF
- [x] Assign order
- [x] Generate invoice
- [x] Prepare products
- [x] Get assignments by store
- [ ] **Get assignment by order ID (chi tiết)** ← MỚI

---

## 🎉 Kết Luận

Sau khi chạy các script mới:
- ✅ Đã test **TẤT CẢ** các chức năng DELIVERY
- ✅ Đã test **TẤT CẢ** các chức năng STAFF
- ✅ Hiểu rõ nguyên nhân các lỗi 400 và 500
- ✅ Có workflow test hoàn chỉnh

**Tất cả các chức năng đã được cover!** 🚀

