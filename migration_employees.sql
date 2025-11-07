-- Migration Script: Move Employee data from users table to employees table
-- Database: user_db
-- Date: 2025-01-XX
-- Note: This script is idempotent - safe to run multiple times

-- Step 0: Check if database is empty (no users table = empty database)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        RAISE NOTICE 'Database is empty - No users table found. Hibernate will create employees table on startup.';
        RETURN;
    END IF;
END $$;

-- Step 1: Create employees table (if not exists - Hibernate will create it automatically)
-- This script assumes Hibernate will create the table structure
-- But we provide the SQL for reference:

/*
CREATE TABLE IF NOT EXISTS employees (
    id VARCHAR(255) PRIMARY KEY,
    code VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255),
    phone VARCHAR(255) UNIQUE,
    birthday DATE,
    gender BOOLEAN,
    status VARCHAR(50),
    avatar VARCHAR(255),
    cccd VARCHAR(20) UNIQUE,
    department VARCHAR(255),
    position VARCHAR(255),
    salary DECIMAL(15, 2),
    account_id VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX IF NOT EXISTS idx_employees_account_id ON employees(account_id);
CREATE INDEX IF NOT EXISTS idx_employees_code ON employees(code);
CREATE INDEX IF NOT EXISTS idx_employees_phone ON employees(phone);
*/

-- Step 2: Create employee_stores table (if not exists)
/*
CREATE TABLE IF NOT EXISTS employee_stores (
    employee_id VARCHAR(255) NOT NULL,
    store_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (employee_id, store_id),
    FOREIGN KEY (employee_id) REFERENCES employees(id),
    FOREIGN KEY (store_id) REFERENCES stores(id)
);

CREATE INDEX IF NOT EXISTS idx_employee_stores_employee_id ON employee_stores(employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_stores_store_id ON employee_stores(store_id);
*/

-- Step 3: Generate employee codes for existing employees
-- First, create a function to generate employee code based on role
CREATE OR REPLACE FUNCTION generate_employee_code(role_name VARCHAR, emp_id VARCHAR) 
RETURNS VARCHAR AS $$
DECLARE
    prefix VARCHAR(3);
    code VARCHAR(50);
BEGIN
    CASE role_name
        WHEN 'ADMIN' THEN prefix := 'ADM';
        WHEN 'BRANCH_MANAGER' THEN prefix := 'MGR';
        WHEN 'DELIVERY' THEN prefix := 'DLV';
        WHEN 'STAFF' THEN prefix := 'STF';
        ELSE prefix := 'EMP';
    END CASE;
    
    -- Use first 8 characters of UUID + timestamp
    code := prefix || '-' || SUBSTRING(emp_id FROM 1 FOR 8) || '-' || EXTRACT(EPOCH FROM NOW())::BIGINT;
    RETURN code;
END;
$$ LANGUAGE plpgsql;

-- Step 4: Migrate Employee data from users to employees table
-- Only migrate users with employee roles (BRANCH_MANAGER, DELIVERY, STAFF, ADMIN)
-- Exclude CUSTOMER role
-- This step is idempotent - will skip if employees already exist

-- Check if employees table exists, if not, Hibernate will create it on startup
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employees') THEN
        RAISE NOTICE 'employees table does not exist - Hibernate will create it on startup. Skipping data migration.';
        RAISE NOTICE 'Please restart user-service to create the table, then run this script again to migrate data.';
        RETURN;
    END IF;
END $$;

-- Only proceed if employees table exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employees') THEN
        RETURN;
    END IF;
    
    -- Insert employees data
    EXECUTE '
    INSERT INTO employees (
    id,
    code,
    full_name,
    phone,
    birthday,
    gender,
    status,
    avatar,
    cccd,
    department,
    position,
    salary,
    account_id,
    created_at,
    updated_at,
    is_deleted
)
SELECT 
    u.id,
    generate_employee_code(a.role::VARCHAR, u.id::VARCHAR) as code,
    u.full_name,
    u.phone,
    u.birthday,
    u.gender,
    u.status,
    u.avatar,
    u.cccd,
    u.department,
    u.position,
    u.salary,
    u.account_id,
    u.created_at,
    u.updated_at,
    u.is_deleted
    FROM users u
    INNER JOIN accounts a ON u.account_id = a.id
    WHERE a.role IN (''BRANCH_MANAGER'', ''DELIVERY'', ''STAFF'', ''ADMIN'')
      AND u.is_deleted = false
      AND NOT EXISTS (
          SELECT 1 FROM employees e WHERE e.id = u.id
      )
    ON CONFLICT (id) DO NOTHING';
    
    RAISE NOTICE 'Migrated employee data from users to employees table';
END $$;

-- Step 5: Migrate user_stores relationships to employee_stores
-- Only for employees that were migrated
-- This step is idempotent - will skip if relationships already exist

-- Check if employee_stores table exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employee_stores') THEN
        RAISE NOTICE 'employee_stores table does not exist - Hibernate will create it on startup. Skipping relationship migration.';
        RETURN;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_stores') THEN
        RAISE NOTICE 'user_stores table does not exist - No relationships to migrate.';
        RETURN;
    END IF;
    
    -- Only proceed if both tables exist
    EXECUTE '
    INSERT INTO employee_stores (
    employee_id,
    store_id,
    created_at,
    updated_at,
    is_deleted
)
SELECT 
    us.user_id as employee_id,
    us.store_id,
    us.created_at,
    us.updated_at,
    us.is_deleted
    FROM user_stores us
    INNER JOIN employees e ON us.user_id = e.id
    WHERE us.is_deleted = false
      AND NOT EXISTS (
          SELECT 1 FROM employee_stores es 
          WHERE es.employee_id = us.user_id AND es.store_id = us.store_id
      )
    ON CONFLICT (employee_id, store_id) DO NOTHING';
    
    RAISE NOTICE 'Migrated employee-store relationships from user_stores to employee_stores';
END $$;

-- Step 6: Verify migration
-- Count employees in users table (should be 0 for employee roles)
SELECT 
    COUNT(*) as employees_still_in_users,
    'Employees still in users table' as status
FROM users u
INNER JOIN accounts a ON u.account_id = a.id
WHERE a.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN')
  AND u.is_deleted = false;

-- Count employees in employees table
SELECT 
    COUNT(*) as total_employees,
    'Total employees in employees table' as status
FROM employees
WHERE is_deleted = false;

-- Count by role
SELECT 
    a.role,
    COUNT(*) as count
FROM employees e
INNER JOIN accounts a ON e.account_id = a.id
WHERE e.is_deleted = false
GROUP BY a.role
ORDER BY count DESC;

-- Step 7: Clean up (OPTIONAL - Only run after verifying migration is successful)
-- DO NOT run this until you have verified the migration is complete and correct!
-- 
-- Delete employee records from users table (after migration is verified)
-- DELETE FROM users u
-- WHERE EXISTS (
--     SELECT 1 FROM accounts a 
--     WHERE a.id = u.account_id 
--     AND a.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN')
-- )
-- AND EXISTS (
--     SELECT 1 FROM employees e WHERE e.id = u.id
-- );

-- Step 8: Clean up function
DROP FUNCTION IF EXISTS generate_employee_code(VARCHAR, VARCHAR);

-- Migration complete!
-- Note: After migration, you should restart the application
-- so that Hibernate recognizes the new table structure

