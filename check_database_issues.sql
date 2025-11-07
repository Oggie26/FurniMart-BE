-- Check database issues
-- 1. Check if user_stores still exists
SELECT 
    'user_stores' as table_name,
    EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_stores') as exists;

-- 2. Check if employee_stores exists
SELECT 
    'employee_stores' as table_name,
    EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employee_stores') as exists;

-- 3. Check employee in users table
SELECT 
    u.id,
    u.full_name,
    a.role,
    a.email,
    u.is_deleted
FROM users u
INNER JOIN accounts a ON u.account_id = a.id
WHERE a.role IN ('ADMIN', 'BRANCH_MANAGER', 'DELIVERY', 'STAFF')
AND u.is_deleted = false;

-- 4. Check if this employee exists in employees table
SELECT 
    e.id,
    e.full_name,
    a.role,
    a.email,
    e.is_deleted
FROM employees e
INNER JOIN accounts a ON e.account_id = a.id
WHERE e.is_deleted = false;

-- 5. Check data in user_stores (if exists)
SELECT 
    'user_stores data' as info,
    COUNT(*) as count
FROM user_stores
WHERE is_deleted = false;

-- 6. Check data in employee_stores
SELECT 
    'employee_stores data' as info,
    COUNT(*) as count
FROM employee_stores
WHERE is_deleted = false;

