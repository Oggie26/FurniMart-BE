-- Categories
INSERT INTO categories (id, category_name, description, image, status, created_at, updated_at)
VALUES 
(1, 'Living Room', 'Furniture for living room', 'living_room.jpg', 'ACTIVE', NOW(), NOW()),
(2, 'Bedroom', 'Furniture for bedroom', 'bedroom.jpg', 'ACTIVE', NOW(), NOW()),
(3, 'Kitchen', 'Furniture for kitchen', 'kitchen.jpg', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Materials
INSERT INTO materials (id, material_name, description, status, image, created_at, updated_at)
VALUES 
(1, 'Wood', 'High quality wood', 'ACTIVE', 'wood.jpg', NOW(), NOW()),
(2, 'Leather', 'Genuine leather', 'ACTIVE', 'leather.jpg', NOW(), NOW()),
(3, 'Metal', 'Stainless steel', 'ACTIVE', 'metal.jpg', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Colors
INSERT INTO colors (id, color_name, hex_code, created_at, updated_at)
VALUES 
('color-red', 'Red', '#FF0000', NOW(), NOW()),
('color-blue', 'Blue', '#0000FF', NOW(), NOW()),
('color-black', 'Black', '#000000', NOW(), NOW()),
('color-white', 'White', '#FFFFFF', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Products
INSERT INTO products (id, code, name, description, sell_price, thumbnail_image, weight, height, width, length, status, slug, category_id, created_at, updated_at)
VALUES 
('prod-sofa-001', 'P001', 'Luxury Sofa', 'Comfortable luxury sofa', 500.0, 'sofa.jpg', 50.0, 100.0, 200.0, 80.0, 'ACTIVE', 'living-room/luxury-sofa', 1, NOW(), NOW()),
('prod-table-001', 'P002', 'Wooden Table', 'Solid wood table', 200.0, 'table.jpg', 30.0, 75.0, 150.0, 90.0, 'ACTIVE', 'kitchen/wooden-table', 3, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Product Materials
INSERT INTO product_materials (product_id, material_id)
VALUES 
('prod-sofa-001', 2), -- Sofa - Leather
('prod-sofa-001', 1), -- Sofa - Wood
('prod-table-001', 1) -- Table - Wood
ON CONFLICT DO NOTHING;

-- Product Colors
INSERT INTO product_colors (id, product_id, color_id, status, created_at, updated_at)
VALUES 
('pc-sofa-red', 'prod-sofa-001', 'color-red', 'ACTIVE', NOW(), NOW()),
('pc-sofa-black', 'prod-sofa-001', 'color-black', 'ACTIVE', NOW(), NOW()),
('pc-table-wood', 'prod-table-001', 'color-white', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
