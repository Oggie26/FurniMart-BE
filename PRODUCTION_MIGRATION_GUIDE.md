# Hướng Dẫn Migration Production Server

## Thông tin Server
- **SSH**: `nam@152.53.227.115`
- **Password**: `Namnam123@`
- **Database**: `user_db` (PostgreSQL trong Docker container `user-db`)

## Files cần upload lên server

1. `migration_employees.sql`
2. `migration_rename_user_stores_to_employee_stores.sql`
3. `migration_remove_user_columns.sql`
4. `check_database_state.sql`
5. `production_migration.sh` (script tự động)

## Cách thực hiện

### Option 1: Sử dụng script tự động (Khuyến nghị)

1. **SSH vào server:**
   ```bash
   ssh nam@152.53.227.115
   ```

2. **Upload các files lên server:**
   ```bash
   # Từ máy local (PowerShell)
   scp migration_*.sql check_database_state.sql production_migration.sh nam@152.53.227.115:~/FurniMart-BE/
   ```

3. **SSH vào server và chạy script:**
   ```bash
   ssh nam@152.53.227.115
   cd ~/FurniMart-BE  # hoặc thư mục chứa project
   chmod +x production_migration.sh
   ./production_migration.sh
   ```

### Option 2: Chạy từng bước thủ công

1. **SSH vào server:**
   ```bash
   ssh nam@152.53.227.115
   cd ~/FurniMart-BE  # hoặc thư mục chứa project
   ```

2. **Kiểm tra database state:**
   ```bash
   docker exec -i user-db psql -U postgres -d user_db < check_database_state.sql
   ```

3. **Backup database (QUAN TRỌNG):**
   ```bash
   mkdir -p backups
   docker exec user-db pg_dump -U postgres user_db > backups/backup_user_db_$(date +%Y%m%d_%H%M%S).sql
   ls -lh backups/
   ```

4. **Chạy migration employees:**
   ```bash
   docker exec -i user-db psql -U postgres -d user_db < migration_employees.sql
   ```

5. **Chạy migration rename (nếu cần):**
   ```bash
   # Kiểm tra xem user_stores có tồn tại không
   docker exec user-db psql -U postgres -d user_db -c "\dt user_stores"
   
   # Nếu có, chạy rename
   docker exec -i user-db psql -U postgres -d user_db < migration_rename_user_stores_to_employee_stores.sql
   ```

6. **Verify migration:**
   ```bash
   docker exec user-db psql -U postgres -d user_db -c "
   SELECT 
       'employees' as table_name, COUNT(*) as count 
   FROM employees 
   WHERE is_deleted = false
   UNION ALL
   SELECT 
       'employee_stores' as table_name, COUNT(*) as count 
   FROM employee_stores 
   WHERE is_deleted = false;
   "
   ```

7. **Xóa các cột không cần thiết:**
   ```bash
   docker exec -i user-db psql -U postgres -d user_db < migration_remove_user_columns.sql
   ```

8. **Rebuild Docker (KHÔNG xóa volumes):**
   ```bash
   # QUAN TRỌNG: KHÔNG dùng -v flag!
   docker compose down
   docker compose build --no-cache user-service
   docker compose up -d
   ```

9. **Kiểm tra services:**
   ```bash
   docker ps --filter "name=user"
   docker logs user-service --tail 50
   ```

10. **Test API:**
    - Swagger UI: `http://152.53.227.115:8086/swagger-ui.html`
    - Test endpoints: GET `/api/employees`, GET `/api/users`

## Lưu ý quan trọng

1. **KHÔNG dùng `docker compose down -v`** - Sẽ xóa volumes chứa database!
2. **Backup trước khi migration** - Luôn tạo backup trước khi thay đổi database
3. **Verify sau mỗi bước** - Kiểm tra kết quả sau mỗi bước migration
4. **Kiểm tra logs** - Xem logs của user-service sau khi rebuild

## Rollback (nếu cần)

Nếu có vấn đề, restore từ backup:
```bash
docker exec -i user-db psql -U postgres -d user_db < backups/backup_user_db_YYYYMMDD_HHMMSS.sql
docker compose restart user-service
```

