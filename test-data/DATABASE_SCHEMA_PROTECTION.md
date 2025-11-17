# BẢO VỆ DATABASE SCHEMA - KHÔNG TỰ XÓA BẢNG

**Mục tiêu**: Đảm bảo database schema không bị tự động xóa bảng khi restart service.

---

## 🔒 CẤU HÌNH AN TOÀN

### 1. **Hibernate DDL Auto Modes**

| Mode | Mô tả | An toàn? | Hành động |
|------|-------|----------|-----------|
| `none` | Không làm gì | ✅ An toàn | Không thay đổi schema |
| `validate` | Chỉ validate | ✅ **AN TOÀN NHẤT** | Kiểm tra schema khớp với entities, không thay đổi gì |
| `update` | Tự động update | ⚠️ Tương đối an toàn | Tạo/update bảng, **KHÔNG XÓA** bảng |
| `create` | Tạo mới mỗi lần | ❌ **NGUY HIỂM** | Xóa và tạo lại schema → **MẤT DỮ LIỆU** |
| `create-drop` | Tạo khi start, xóa khi stop | ❌ **RẤT NGUY HIỂM** | Xóa schema khi stop → **MẤT DỮ LIỆU** |

### 2. **Cấu hình hiện tại**

#### ✅ Local (application.yml)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # ✅ AN TOÀN
    generate-ddl: false   # ✅ Đảm bảo không tự generate
```

#### ⚠️ Server (docker-compose.yml)
```yaml
environment:
  SPRING_JPA_HIBERNATE_DDL_AUTO: update  # ⚠️ Đã đổi từ validate để fix lỗi
```

**Lưu ý**: `update` không xóa bảng, nhưng `validate` an toàn hơn.

---

## 🛡️ QUY TẮC BẢO VỆ

### ✅ ĐƯỢC PHÉP:
1. ✅ `ddl-auto: validate` - Chỉ validate, không thay đổi
2. ✅ `ddl-auto: update` - Tạo/update bảng, không xóa
3. ✅ `ddl-auto: none` - Không làm gì

### ❌ KHÔNG ĐƯỢC PHÉP:
1. ❌ `ddl-auto: create` - **NGUY HIỂM** - Xóa và tạo lại schema
2. ❌ `ddl-auto: create-drop` - **RẤT NGUY HIỂM** - Xóa schema khi stop
3. ❌ `generate-ddl: true` - Có thể gây xung đột

---

## 📋 CHECKLIST TRƯỚC KHI DEPLOY

- [ ] Kiểm tra `application.yml` không có `ddl-auto: create` hoặc `create-drop`
- [ ] Kiểm tra `docker-compose.yml` không có `SPRING_JPA_HIBERNATE_DDL_AUTO: create` hoặc `create-drop`
- [ ] Đảm bảo `generate-ddl: false` trong `application.yml`
- [ ] Test trên môi trường dev trước khi deploy production
- [ ] Backup database trước khi thay đổi schema

---

## 🔍 KIỂM TRA CẤU HÌNH

### Local
```bash
# Kiểm tra application.yml
grep -r "ddl-auto" user-service/src/main/resources/application.yml

# Kết quả mong đợi:
# ddl-auto: validate  (hoặc update, nhưng KHÔNG phải create/create-drop)
```

### Server
```bash
# SSH vào server
ssh nam@152.53.227.115

# Kiểm tra docker-compose.yml
cd ~/FurniMart-BE
grep "SPRING_JPA_HIBERNATE_DDL_AUTO" docker-compose.yml

# Kết quả mong đợi:
# SPRING_JPA_HIBERNATE_DDL_AUTO: validate  (hoặc update, nhưng KHÔNG phải create/create-drop)
```

---

## 🚨 CẢNH BÁO

### Nếu thấy các cấu hình sau, **DỪNG LẠI NGAY**:

```yaml
# ❌ NGUY HIỂM - XÓA DỮ LIỆU
ddl-auto: create
ddl-auto: create-drop
SPRING_JPA_HIBERNATE_DDL_AUTO: create
SPRING_JPA_HIBERNATE_DDL_AUTO: create-drop
```

### Hành động khi phát hiện:
1. **DỪNG service ngay lập tức**
2. **Backup database** (nếu chưa mất dữ liệu)
3. **Đổi về `validate` hoặc `update`**
4. **Restart service**

---

## 📝 KHUYẾN NGHỊ

### Cho Production:
- ✅ **Dùng `validate`** sau khi đã có schema
- ✅ **Dùng migration tool** (Flyway/Liquibase) để quản lý schema changes
- ✅ **Backup database** định kỳ
- ✅ **Test schema changes** trên môi trường dev/staging trước

### Cho Development:
- ⚠️ Có thể dùng `update` để tự động tạo/update bảng
- ❌ **KHÔNG BAO GIỜ** dùng `create` hoặc `create-drop` trên production

---

## 🔧 CÁCH SỬA NẾU PHÁT HIỆN NGUY HIỂM

### 1. Sửa application.yml (Local)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Đổi từ create/create-drop
    generate-ddl: false
```

### 2. Sửa docker-compose.yml (Server)
```yaml
environment:
  SPRING_JPA_HIBERNATE_DDL_AUTO: validate  # Đổi từ create/create-drop
```

### 3. Commit và Push
```bash
git add .
git commit -m "fix: Change ddl-auto to validate to prevent data loss"
git push origin main
```

### 4. Rebuild và Restart trên Server
```bash
ssh nam@152.53.227.115
cd ~/FurniMart-BE
git pull origin main
docker compose build user-service
docker compose restart user-service
```

---

## 📊 TRẠNG THÁI HIỆN TẠI

### Local:
- ✅ `application.yml`: `ddl-auto: validate` - **AN TOÀN**
- ✅ `generate-ddl: false` - **AN TOÀN**

### Server:
- ⚠️ `docker-compose.yml`: `SPRING_JPA_HIBERNATE_DDL_AUTO: update` - **Tương đối an toàn** (không xóa bảng)
- 💡 **Khuyến nghị**: Đổi về `validate` sau khi đã có schema đầy đủ

---

## 🎯 KẾT LUẬN

1. ✅ **Hiện tại**: Cấu hình an toàn (validate/update)
2. ✅ **Không có**: create/create-drop (nguy hiểm)
3. ✅ **Đã có**: `generate-ddl: false` để đảm bảo
4. 💡 **Khuyến nghị**: Dùng `validate` cho production sau khi schema đã ổn định

---

**Tài liệu này đảm bảo database schema không bị tự động xóa bảng.**

