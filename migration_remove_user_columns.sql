-- Migration Script: Remove department, position, salary columns from users table
-- Database: user_db
-- Date: 2025-01-XX
-- Note: These columns are only for employees, not customers
-- IMPORTANT: Only run this AFTER verifying migration is complete!

-- Step 1: Verify migration is complete before dropping columns
DO $$
DECLARE
    employees_count INTEGER;
    users_employee_count INTEGER;
    customers_count INTEGER;
BEGIN
    -- Count employees in employees table
    SELECT COUNT(*) INTO employees_count 
    FROM employees 
    WHERE is_deleted = false;
    
    -- Count users with employee roles (should be 0)
    SELECT COUNT(*) INTO users_employee_count
    FROM users u
    INNER JOIN accounts a ON u.account_id = a.id
    WHERE a.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN')
    AND u.is_deleted = false;
    
    -- Count customers (should remain)
    SELECT COUNT(*) INTO customers_count
    FROM users u
    INNER JOIN accounts a ON u.account_id = a.id
    WHERE a.role = 'CUSTOMER'
    AND u.is_deleted = false;
    
    -- Only proceed if migration is complete
    IF users_employee_count > 0 THEN
        RAISE EXCEPTION 'Migration chưa hoàn tất! Vẫn còn % employees trong bảng users. Không thể xóa cột!', users_employee_count;
    END IF;
    
    IF employees_count = 0 AND users_employee_count = 0 THEN
        RAISE NOTICE 'Cảnh báo: Không có employees nào trong database. Kiểm tra lại migration.';
    END IF;
    
    RAISE NOTICE 'Verification: % employees trong employees table, % customers trong users table', employees_count, customers_count;
    RAISE NOTICE 'An toàn để xóa các cột department, position, salary khỏi bảng users';
END $$;

-- Step 2: Check if columns exist before dropping
DO $$
DECLARE
    dept_exists BOOLEAN;
    pos_exists BOOLEAN;
    sal_exists BOOLEAN;
BEGIN
    -- Check if columns exist
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'department'
    ) INTO dept_exists;
    
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'position'
    ) INTO pos_exists;
    
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'salary'
    ) INTO sal_exists;
    
    -- Drop columns if they exist
    IF dept_exists THEN
        ALTER TABLE users DROP COLUMN department;
        RAISE NOTICE 'Đã xóa cột department khỏi bảng users';
    ELSE
        RAISE NOTICE 'Cột department không tồn tại - Bỏ qua';
    END IF;
    
    IF pos_exists THEN
        ALTER TABLE users DROP COLUMN position;
        RAISE NOTICE 'Đã xóa cột position khỏi bảng users';
    ELSE
        RAISE NOTICE 'Cột position không tồn tại - Bỏ qua';
    END IF;
    
    IF sal_exists THEN
        ALTER TABLE users DROP COLUMN salary;
        RAISE NOTICE 'Đã xóa cột salary khỏi bảng users';
    ELSE
        RAISE NOTICE 'Cột salary không tồn tại - Bỏ qua';
    END IF;
END $$;

-- Step 3: Verify columns have been removed
SELECT 
    column_name,
    data_type
FROM information_schema.columns
WHERE table_name = 'users'
AND column_name IN ('department', 'position', 'salary')
ORDER BY column_name;

-- If query returns 0 rows, columns have been successfully removed

