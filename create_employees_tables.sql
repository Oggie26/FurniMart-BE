-- Script to create employees and employee_stores tables manually
-- This is used when Hibernate hasn't created them yet
-- Database: user_db

-- Create employees table
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

-- Create employee_stores table
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

-- Verify tables created
SELECT 
    'employees' as table_name,
    EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employees') as exists;

SELECT 
    'employee_stores' as table_name,
    EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employee_stores') as exists;


