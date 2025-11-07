-- Migration Script: Rename user_stores table to employee_stores
-- Database: user_db
-- Date: 2025-01-XX
-- Note: This script is idempotent - safe to run multiple times

-- Step 1: Check if user_stores table exists and has data
DO $$
DECLARE
    table_exists BOOLEAN;
    employee_stores_exists BOOLEAN;
    record_count INTEGER;
BEGIN
    -- Check if user_stores table exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables WHERE table_name = 'user_stores'
    ) INTO table_exists;
    
    -- Check if employee_stores table already exists
    SELECT EXISTS (
        SELECT 1 FROM information_schema.tables WHERE table_name = 'employee_stores'
    ) INTO employee_stores_exists;
    
    IF NOT table_exists THEN
        RAISE NOTICE 'user_stores table does not exist - Nothing to rename.';
        RETURN;
    END IF;
    
    IF employee_stores_exists THEN
        RAISE NOTICE 'employee_stores table already exists - Skipping rename.';
        RETURN;
    END IF;
    
    -- Count records in user_stores
    EXECUTE 'SELECT COUNT(*) FROM user_stores WHERE is_deleted = false' INTO record_count;
    RAISE NOTICE 'Found % records in user_stores table', record_count;
END $$;

-- Step 2: Rename table from user_stores to employee_stores (only if conditions are met)
DO $$
DECLARE
    table_exists BOOLEAN;
    employee_stores_exists BOOLEAN;
BEGIN
    SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_stores') INTO table_exists;
    SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employee_stores') INTO employee_stores_exists;
    
    IF table_exists AND NOT employee_stores_exists THEN
        ALTER TABLE user_stores RENAME TO employee_stores;
        RAISE NOTICE 'Renamed user_stores to employee_stores';
    ELSE
        RAISE NOTICE 'Skipping rename - Conditions not met';
    END IF;
END $$;

-- Step 3: Rename columns (if needed)
-- Note: PostgreSQL will automatically rename columns if they match the new naming convention
-- But we'll explicitly rename to ensure consistency:
DO $$
DECLARE
    column_exists BOOLEAN;
BEGIN
    -- Check if employee_stores table exists and has user_id column
    SELECT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'employee_stores' AND column_name = 'user_id'
    ) INTO column_exists;
    
    IF column_exists THEN
        ALTER TABLE employee_stores RENAME COLUMN user_id TO employee_id;
        RAISE NOTICE 'Renamed column user_id to employee_id';
    ELSE
        RAISE NOTICE 'Column user_id does not exist or already renamed - Skipping';
    END IF;
END $$;

-- Step 4: Drop and recreate foreign key constraints
DO $$
DECLARE
    table_exists BOOLEAN;
    employees_table_exists BOOLEAN;
BEGIN
    SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employee_stores') INTO table_exists;
    SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employees') INTO employees_table_exists;
    
    IF NOT table_exists THEN
        RAISE NOTICE 'employee_stores table does not exist - Skipping foreign key updates';
        RETURN;
    END IF;
    
    -- Drop old foreign key constraint on employee_id (if exists)
    ALTER TABLE employee_stores DROP CONSTRAINT IF EXISTS fk_user_stores_user;
    ALTER TABLE employee_stores DROP CONSTRAINT IF EXISTS fk_employee_stores_employee;
    
    -- Add foreign key constraint on employee_id pointing to employees table (if employees table exists)
    IF employees_table_exists THEN
        ALTER TABLE employee_stores 
        ADD CONSTRAINT fk_employee_stores_employee 
        FOREIGN KEY (employee_id) REFERENCES employees(id);
        RAISE NOTICE 'Added foreign key constraint fk_employee_stores_employee';
    ELSE
        RAISE NOTICE 'employees table does not exist - Cannot add foreign key constraint';
    END IF;
    
    -- Drop and recreate foreign key constraint on store_id
    ALTER TABLE employee_stores DROP CONSTRAINT IF EXISTS fk_user_stores_store;
    ALTER TABLE employee_stores DROP CONSTRAINT IF EXISTS fk_employee_stores_store;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'stores') THEN
        ALTER TABLE employee_stores 
        ADD CONSTRAINT fk_employee_stores_store 
        FOREIGN KEY (store_id) REFERENCES stores(id);
        RAISE NOTICE 'Added foreign key constraint fk_employee_stores_store';
    END IF;
END $$;

-- Step 7: Update indexes
DO $$
DECLARE
    table_exists BOOLEAN;
BEGIN
    SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employee_stores') INTO table_exists;
    
    IF NOT table_exists THEN
        RAISE NOTICE 'employee_stores table does not exist - Skipping index updates';
        RETURN;
    END IF;
    
    -- Drop old indexes if they exist
    DROP INDEX IF EXISTS idx_user_stores_user_id;
    DROP INDEX IF EXISTS idx_user_stores_store_id;
    DROP INDEX IF EXISTS idx_employee_stores_employee_id;
    DROP INDEX IF EXISTS idx_employee_stores_store_id;
    
    -- Create new indexes
    CREATE INDEX IF NOT EXISTS idx_employee_stores_employee_id ON employee_stores(employee_id);
    CREATE INDEX IF NOT EXISTS idx_employee_stores_store_id ON employee_stores(store_id);
    
    RAISE NOTICE 'Updated indexes for employee_stores table';
END $$;

-- Step 8: Verify migration
DO $$
DECLARE
    table_exists BOOLEAN;
    record_count INTEGER;
BEGIN
    SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employee_stores') INTO table_exists;
    
    IF table_exists THEN
        EXECUTE 'SELECT COUNT(*) FROM employee_stores WHERE is_deleted = false' INTO record_count;
        RAISE NOTICE 'employee_stores table has % records', record_count;
    ELSE
        RAISE NOTICE 'employee_stores table does not exist';
    END IF;
END $$;

-- Step 9: Check table structure (if table exists)
SELECT 
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'employee_stores'
ORDER BY ordinal_position;

-- Migration complete!
-- Note: After migration, restart the application
-- so that Hibernate recognizes the new table name

