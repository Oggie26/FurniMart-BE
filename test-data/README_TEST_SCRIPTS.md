# Hướng Dẫn Sử Dụng Test Scripts

## 📁 Danh Sách Scripts

### 1. Tạo Tài Khoản Test
- **`create-test-accounts-simple.ps1`**: Tạo các tài khoản BRANCH_MANAGER, STAFF, DELIVERY
- **`create-test-accounts.sh`**: Phiên bản Bash cho Linux/Mac

### 2. Test Assign Order Delivery
- **`test-assign-order-delivery.ps1`**: Test chức năng assign order (STAFF/BRANCH_MANAGER)
- **`test-assign-order-delivery.sh`**: Phiên bản Bash

### 3. Test Theo Role
- **`test-staff-functions.ps1`**: Test các chức năng của STAFF
- **`test-delivery-functions.ps1`**: Test các chức năng của DELIVERY
- **`test-branch-manager-functions.ps1`**: Test các chức năng của BRANCH_MANAGER
- **`test-all-roles.ps1`**: Chạy tất cả các test theo role

## 🎯 Test Scenarios

### STAFF Role Tests
1. ✅ Login as STAFF
2. ✅ Get stores
3. ✅ Get orders
4. ⚠️ Generate invoice (lỗi 500)
5. ⚠️ Prepare products (lỗi 500)
6. ⚠️ Get assignments by store (lỗi 500)

### DELIVERY Role Tests
1. ✅ Login as DELIVERY
2. ⚠️ Get assignments by staff (lỗi 500)
3. ⚠️ Update delivery status (cần có assignment trước)
4. ✅ Test unauthorized endpoints (assign order - should fail)

### BRANCH_MANAGER Role Tests
1. ✅ Login as BRANCH_MANAGER
2. ✅ Get stores
3. ⚠️ Monitor delivery progress (lỗi 500)
4. ⚠️ Get assignments by store (lỗi 500)
5. ⚠️ Update delivery status (cần có assignment trước)

## 🔑 Tài Khoản Test

Xem file `TEST_ACCOUNTS.md` để biết thông tin đăng nhập của các tài khoản test.

## 📊 Kết Quả Test

Xem file `TEST_RESULTS.md` để biết chi tiết kết quả test và các lỗi gặp phải.

## 🐛 Troubleshooting

### Lỗi 500 Internal Server Error
1. Kiểm tra logs: `docker logs delivery-service --tail 50`
2. Kiểm tra service status: `docker ps | grep delivery-service`
3. Kiểm tra database connection
4. Kiểm tra xem có dữ liệu trong database không

### Lỗi 403 Forbidden
- Đây là lỗi đúng (unauthorized access)
- Script sẽ báo "Correctly rejected"

### Lỗi 404 Not Found
- Có thể do thiếu dữ liệu (orders, stores, assignments)
- Kiểm tra database hoặc tạo dữ liệu test trước

## 📝 Notes

- Tất cả scripts đều tự động lấy thông tin từ API (store ID, order ID, etc.)
- Scripts có fallback values nếu không tìm thấy dữ liệu
- Scripts hiển thị kết quả chi tiết với màu sắc để dễ đọc

