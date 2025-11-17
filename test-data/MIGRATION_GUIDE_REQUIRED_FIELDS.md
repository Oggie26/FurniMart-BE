# Migration Guide: Make Required Fields

## Tổng Quan

Migration này làm cho các trường sau trở thành bắt buộc:
1. `delivery_staff_id` trong bảng `delivery_assignments`
2. `quantity` và `total` trong `OrderRequest` (đã được validate ở application level)

---

## 1. Database Migration

### 1.1. Kiểm Tra Dữ Liệu Hiện Tại

Trước khi chạy migration, kiểm tra xem có assignment nào có `delivery_staff_id = NULL` không:

```sql
-- Kiểm tra số lượng assignments có delivery_staff_id = NULL
SELECT COUNT(*) 
FROM delivery_assignments 
WHERE delivery_staff_id IS NULL 
AND is_deleted = false;
```

### 1.2. Xử Lý Dữ Liệu NULL (Nếu Có)

**Option 1: Xóa các assignment có NULL delivery_staff_id**
```sql
-- ⚠️ CẢNH BÁO: Chỉ chạy nếu chắc chắn muốn xóa
DELETE FROM delivery_assignments 
WHERE delivery_staff_id IS NULL 
AND is_deleted = false;
```

**Option 2: Gán delivery_staff_id mặc định (nếu có logic phù hợp)**
```sql
-- Ví dụ: Gán cho một delivery staff mặc định
-- Thay 'default-delivery-staff-id' bằng ID thực tế
UPDATE delivery_assignments 
SET delivery_staff_id = 'default-delivery-staff-id'
WHERE delivery_staff_id IS NULL 
AND is_deleted = false;
```

### 1.3. Chạy Migration

**Cách 1: Sử dụng migration script tự động (nếu có Flyway)**
```bash
# Migration script đã được tạo tại:
# delivery-service/src/main/resources/db/migration/V999__make_delivery_staff_id_required.sql
```

**Cách 2: Chạy thủ công trên database**
```sql
-- Chạy trực tiếp trên PostgreSQL
ALTER TABLE delivery_assignments 
MODIFY COLUMN delivery_staff_id VARCHAR(255) NOT NULL;
```

**Cách 3: Sử dụng Hibernate DDL Auto (nếu dùng `ddl-auto: update`)**
- Hibernate sẽ tự động cập nhật schema khi service khởi động
- ⚠️ **Lưu ý**: Cần đảm bảo không có data NULL trước khi service khởi động

---

## 2. Application Changes

### 2.1. Code Changes (Đã được commit)

✅ **AssignOrderRequest.java**
- Thêm `@NotNull(message = "Delivery staff ID is required")` cho `deliveryStaffId`

✅ **DeliveryAssignment.java**
- Thêm `nullable = false` cho `delivery_staff_id` column

✅ **OrderRequest.java**
- Thêm `@NotNull(message = "Quantity is required")` cho `quantity`
- Thêm `@NotNull(message = "Total is required")` cho `total`

✅ **DeliveryController.java**
- Cập nhật Swagger documentation

### 2.2. Rebuild Services

Cần rebuild các service sau:
- ✅ `delivery-service` (có thay đổi entity và request)
- ✅ `order-service` (có thay đổi request)

---

## 3. Deployment Steps

### 3.1. Trên Server Production

```bash
# 1. Pull code mới
cd /path/to/FurniMart-BE
git pull origin main

# 2. Kiểm tra và xử lý data NULL (nếu có)
# Kết nối vào database và chạy các query ở mục 1.2

# 3. Rebuild delivery-service
docker compose build --no-cache delivery-service

# 4. Rebuild order-service
docker compose build --no-cache order-service

# 5. Restart services
docker compose up -d delivery-service order-service

# 6. Kiểm tra logs
docker logs delivery-service --tail 50
docker logs order-service --tail 50
```

### 3.2. Kiểm Tra Sau Khi Deploy

1. **Kiểm tra API validation:**
   ```bash
   # Test POST /api/delivery/assign với deliveryStaffId = null
   # Phải trả về 400 Bad Request
   ```

2. **Kiểm tra database constraint:**
   ```sql
   -- Thử insert với delivery_staff_id = NULL (phải fail)
   INSERT INTO delivery_assignments (order_id, store_id, delivery_staff_id, ...)
   VALUES (999, 'test-store', NULL, ...);
   -- Phải trả về lỗi: NOT NULL constraint violation
   ```

3. **Kiểm tra Swagger UI:**
   - Mở `http://152.53.227.115:8089/swagger-ui/index.html`
   - Kiểm tra `POST /api/delivery/assign`
   - `deliveryStaffId` phải có dấu * (required)

---

## 4. Rollback Plan (Nếu Cần)

Nếu cần rollback, thực hiện các bước sau:

### 4.1. Revert Code Changes
```bash
git revert <commit-hash>
git push origin main
```

### 4.2. Revert Database Changes
```sql
-- Chuyển lại thành nullable
ALTER TABLE delivery_assignments 
MODIFY COLUMN delivery_staff_id VARCHAR(255) NULL;
```

### 4.3. Rebuild và Restart
```bash
docker compose build --no-cache delivery-service order-service
docker compose up -d delivery-service order-service
```

---

## 5. Testing Checklist

- [ ] API `POST /api/delivery/assign` từ chối request thiếu `deliveryStaffId`
- [ ] API `POST /api/orders` từ chối request thiếu `quantity` hoặc `total`
- [ ] Database không cho phép insert `delivery_staff_id = NULL`
- [ ] Swagger UI hiển thị các trường là required
- [ ] Existing assignments vẫn hoạt động bình thường
- [ ] Không có lỗi trong logs sau khi deploy

---

## 6. Notes

- ⚠️ **Quan trọng**: Đảm bảo không có data NULL trước khi chạy migration
- ⚠️ **Backup**: Nên backup database trước khi chạy migration
- ✅ **Validation**: Application-level validation sẽ bắt lỗi trước khi đến database
- ✅ **Backward Compatible**: Các API cũ vẫn hoạt động, chỉ thêm validation

---

**Ngày Tạo**: 2025-11-13
**Version**: 1.0

