# Quick Start - Production Migration

## Bước 1: Upload Files lên Server

### Cách 1: Sử dụng PowerShell script (Windows)
```powershell
.\upload_migration_files.ps1
```

### Cách 2: Sử dụng SCP thủ công
```powershell
scp migration_*.sql check_database_state.sql production_migration.sh nam@152.53.227.115:~/FurniMart-BE/
```

### Cách 3: Copy nội dung files và paste vào server
- SSH vào server
- Tạo files và paste nội dung

## Bước 2: SSH vào Server và Chạy Migration

```bash
# SSH vào server
ssh nam@152.53.227.115
# Password: Namnam123@

# Di chuyển vào thư mục project
cd ~/FurniMart-BE

# Cho phép script chạy
chmod +x production_migration.sh

# Chạy migration script
./production_migration.sh
```

## Script sẽ tự động:

1. ✓ Kiểm tra database state
2. ✓ Backup database
3. ✓ Migrate employees từ users → employees
4. ✓ Rename user_stores → employee_stores (nếu cần)
5. ✓ Verify migration
6. ✓ Xóa các cột department, position, salary khỏi users
7. ✓ Rebuild Docker (KHÔNG xóa volumes)
8. ✓ Kiểm tra services

## Sau khi hoàn tất:

1. Kiểm tra logs: `docker logs user-service --tail 50`
2. Test API: `http://152.53.227.115:8086/swagger-ui.html`
3. Verify endpoints:
   - GET `/api/employees` - Phải trả về từ bảng employees
   - GET `/api/users` - Phải chỉ trả về CUSTOMER

## Nếu có lỗi:

1. Xem logs: `docker logs user-service`
2. Kiểm tra database: `docker exec -i user-db psql -U postgres -d user_db`
3. Restore từ backup nếu cần (xem PRODUCTION_MIGRATION_GUIDE.md)

