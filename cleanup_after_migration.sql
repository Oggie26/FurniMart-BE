-- Cleanup Script: Remove user_stores table and employee records from users table
-- Database: user_db
-- Date: 2025-11-07
-- Note: Only run this AFTER verifying migration is complete and correct!

-- Step 1: Verify migration is complete
DO $$
DECLARE
    employees_count INTEGER;
    employee_stores_count INTEGER;
    users_employees_count INTEGER;
    user_stores_count INTEGER;
BEGIN
    -- Count employees in employees table
    SELECT COUNT(*) INTO employees_count FROM employees WHERE is_deleted = false;
    
    -- Count relationships in employee_stores
    SELECT COUNT(*) INTO employee_stores_count FROM employee_stores WHERE is_deleted = false;
    
    -- Count employees still in users table
    SELECT COUNT(*) INTO users_employees_count
    FROM users u
    INNER JOIN accounts a ON u.account_id = a.id
    WHERE a.role IN ('ADMIN', 'BRANCH_MANAGER', 'DELIVERY', 'STAFF')
    AND u.is_deleted = false;
    
    -- Count data in user_stores
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_stores') THEN
        EXECUTE 'SELECT COUNT(*) FROM user_stores WHERE is_deleted = false' INTO user_stores_count;
    ELSE
        user_stores_count := 0;
    END IF;
    
    RAISE NOTICE '=== Migration Verification ===';
    RAISE NOTICE 'Employees in employees table: %', employees_count;
    RAISE NOTICE 'Relationships in employee_stores: %', employee_stores_count;
    RAISE NOTICE 'Employees still in users table: %', users_employees_count;
    RAISE NOTICE 'Data in user_stores: %', user_stores_count;
    
    IF employees_count = 0 THEN
        RAISE EXCEPTION 'No employees found in employees table! Migration may have failed. Aborting cleanup.';
    END IF;
    
    IF users_employees_count = 0 THEN
        RAISE NOTICE 'No employees in users table - Nothing to clean up.';
    END IF;
    
    IF user_stores_count > 0 THEN
        RAISE WARNING 'user_stores still has % records! Make sure they are migrated to employee_stores before dropping the table.', user_stores_count;
    END IF;
END $$;

-- Step 2: Delete employee records from users table
-- Only delete if they exist in employees table (safety check)
DO $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM users u
    WHERE EXISTS (
        SELECT 1 FROM accounts a 
        WHERE a.id = u.account_id 
        AND a.role IN ('ADMIN', 'BRANCH_MANAGER', 'DELIVERY', 'STAFF')
    )
    AND EXISTS (
        SELECT 1 FROM employees e WHERE e.id = u.id
    );
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Deleted % employee records from users table', deleted_count;
END $$;

-- Step 3: Drop user_stores table (if it exists and is empty or data is migrated)
DO $$
DECLARE
    table_exists BOOLEAN;
    record_count INTEGER;
    employee_stores_count INTEGER;
BEGIN
    SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_stores') INTO table_exists;
    
    IF NOT table_exists THEN
        RAISE NOTICE 'user_stores table does not exist - Nothing to drop.';
        RETURN;
    END IF;
    
    -- Count records in user_stores
    EXECUTE 'SELECT COUNT(*) FROM user_stores WHERE is_deleted = false' INTO record_count;
    
    -- Count records in employee_stores
    SELECT COUNT(*) INTO employee_stores_count FROM employee_stores WHERE is_deleted = false;
    
    IF record_count > 0 THEN
        RAISE WARNING 'user_stores still has % records. Make sure they are migrated to employee_stores (which has % records) before dropping.', record_count, employee_stores_count;
        RAISE NOTICE 'Skipping drop of user_stores table - Please verify migration first.';
    ELSE
        DROP TABLE IF EXISTS user_stores CASCADE;
        RAISE NOTICE 'Dropped user_stores table';
    END IF;
END $$;

-- Step 4: Final verification
SELECT 
    'employees' as table_name,
    COUNT(*) as count
FROM employees
WHERE is_deleted = false
UNION ALL
SELECT 
    'employee_stores',
    COUNT(*)
FROM employee_stores
WHERE is_deleted = false
UNION ALL
SELECT 
    'users (customers only)',
    COUNT(*)
FROM users u
INNER JOIN accounts a ON u.account_id = a.id
WHERE a.role = 'CUSTOMER'
AND u.is_deleted = false
UNION ALL
SELECT 
    'users (employees - should be 0)',
    COUNT(*)
FROM users u
INNER JOIN accounts a ON u.account_id = a.id
WHERE a.role IN ('ADMIN', 'BRANCH_MANAGER', 'DELIVERY', 'STAFF')
AND u.is_deleted = false;

-- Check if user_stores still exists
SELECT 
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_stores') 
        THEN 'user_stores still exists'
        ELSE 'user_stores does not exist (dropped)'
    END as user_stores_status;

-- Cleanup complete!

