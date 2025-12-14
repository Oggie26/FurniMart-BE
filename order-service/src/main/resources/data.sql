-- Vouchers
INSERT INTO vouchers (id, name, code, start_date, end_date, amount, description, point, type, status, usage_limit, used_count, minimum_order_amount, created_at, updated_at)
VALUES 
(1, 'Welcome Voucher', 'WELCOME2024', NOW(), NOW() + INTERVAL '1 year', 10.0, 'Welcome discount', 0, 'PERCENTAGE', true, 1000, 0, 100.0, NOW(), NOW()),
(2, 'Summer Sale', 'SUMMER50', NOW(), NOW() + INTERVAL '3 months', 50.0, 'Summer sale discount', 0, 'FIXED_AMOUNT', true, 500, 0, 200.0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
