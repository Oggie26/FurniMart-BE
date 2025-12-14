-- Warehouse
INSERT INTO warehouse (id, warehouse_name, capacity, store_id, status, created_at, updated_at)
VALUES 
('wh-001', 'Main Warehouse', 10000, 'store-001', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Zone
INSERT INTO zone (id, zone_name, description, status, zone_code, quantity, warehouse_id, created_at, updated_at)
VALUES 
('zone-001', 'Zone A', 'General Storage', 'ACTIVE', 'ZONE_A', 1000, 'wh-001', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Location Item
INSERT INTO location_item (id, row_label, column_number, code, description, status, quantity, zone_id, created_at, updated_at)
VALUES 
('loc-001', 1, 1, 'ZONE_A-R1-C1', 'Shelf 1', 'ACTIVE', 100, 'zone-001', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Inventory (Import Ticket)
INSERT INTO inventory (id, employee_id, type, purpose, date, note, code, transfer_status, warehouse_id, created_at, updated_at)
VALUES 
(1, 'emp-staff-001', 'IMPORT', 'IMPORT_FROM_SUPPLIER', NOW(), 'Initial Import', 'INV-INIT-001', 'COMPLETED', 'wh-001', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Inventory Item (Stock)
INSERT INTO inventory_item (id, quantity, product_color_id, reserved_quantity, location_item_id, inventory_id)
VALUES 
(1, 50, 'pc-sofa-red', 0, 'loc-001', 1),
(2, 30, 'pc-sofa-black', 0, 'loc-001', 1)
ON CONFLICT (id) DO NOTHING;
