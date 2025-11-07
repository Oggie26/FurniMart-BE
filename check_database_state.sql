-- Script to check database state before migration
-- Database: user_db
-- Purpose: Determine if database is empty, has data, or tables already exist

-- Step 1: Check if employees table exists
SELECT 
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employees')
        THEN 'EXISTS'
        ELSE 'NOT_EXISTS'
    END as employees_table_status,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employees')
        THEN (SELECT COUNT(*) FROM employees WHERE is_deleted = false)
        ELSE 0
    END as employees_count;

-- Step 2: Check if employee_stores table exists
SELECT 
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employee_stores')
        THEN 'EXISTS'
        ELSE 'NOT_EXISTS'
    END as employee_stores_table_status,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employee_stores')
        THEN (SELECT COUNT(*) FROM employee_stores WHERE is_deleted = false)
        ELSE 0
    END as employee_stores_count;

-- Step 3: Check if users table exists and has employee data
SELECT 
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users')
        THEN 'EXISTS'
        ELSE 'NOT_EXISTS'
    END as users_table_status,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users')
        THEN (
            SELECT COUNT(*) 
            FROM users u
            INNER JOIN accounts a ON u.account_id = a.id
            WHERE a.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN')
            AND u.is_deleted = false
        )
        ELSE 0
    END as users_with_employee_roles_count;

-- Step 4: Check if user_stores table exists
SELECT 
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_stores')
        THEN 'EXISTS'
        ELSE 'NOT_EXISTS'
    END as user_stores_table_status,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_stores')
        THEN (SELECT COUNT(*) FROM user_stores WHERE is_deleted = false)
        ELSE 0
    END as user_stores_count;

-- Step 5: Summary
SELECT 
    'Database State Summary' as summary,
    CASE 
        WHEN NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employees')
        AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users')
        THEN 'EMPTY_DATABASE'
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employees')
        AND (SELECT COUNT(*) FROM employees WHERE is_deleted = false) > 0
        THEN 'MIGRATION_COMPLETE'
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users')
        AND EXISTS (
            SELECT 1 
            FROM users u
            INNER JOIN accounts a ON u.account_id = a.id
            WHERE a.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN')
            AND u.is_deleted = false
        )
        THEN 'NEEDS_MIGRATION'
        ELSE 'UNKNOWN_STATE'
    END as migration_status;


