-- Accounts
INSERT INTO accounts (id, email, password, role, status, enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, updated_at)
VALUES 
('acc-admin-001', 'admin@furnimart.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRkgVduVfzCore3fakZ.OD.1t3e', 'ADMIN', 'ACTIVE', true, true, true, true, NOW(), NOW()),
('acc-staff-001', 'staff@furnimart.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRkgVduVfzCore3fakZ.OD.1t3e', 'STAFF', 'ACTIVE', true, true, true, true, NOW(), NOW()),
('acc-customer-001', 'customer@furnimart.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRkgVduVfzCore3fakZ.OD.1t3e', 'CUSTOMER', 'ACTIVE', true, true, true, true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Users
INSERT INTO users (id, full_name, phone, gender, status, point, account_id, created_at, updated_at)
VALUES 
('user-admin-001', 'Admin User', '0900000001', true, 'ACTIVE', 0, 'acc-admin-001', NOW(), NOW()),
('user-customer-001', 'John Doe', '0900000003', true, 'ACTIVE', 100, 'acc-customer-001', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Stores
INSERT INTO stores (id, name, city, district, ward, street, address_line, status, created_at, updated_at)
VALUES 
('store-001', 'FurniMart Main Store', 'Ho Chi Minh', 'District 1', 'Ben Nghe', 'Le Duan', '123 Le Duan', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Employees
INSERT INTO employees (id, code, full_name, phone, gender, status, department, position, salary, account_id, created_at, updated_at)
VALUES 
('emp-staff-001', 'EMP001', 'Staff Member', '0900000002', true, 'ACTIVE', 'Sales', 'Staff', 10000000, 'acc-staff-001', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Employee Stores
INSERT INTO employee_stores (employee_id, store_id, created_at, updated_at)
VALUES 
('emp-staff-001', 'store-001', NOW(), NOW())
ON CONFLICT (employee_id, store_id) DO NOTHING;
