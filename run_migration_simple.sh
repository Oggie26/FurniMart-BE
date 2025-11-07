#!/bin/bash
# Simple migration script - Run this directly on server with sudo access

set -e

echo "=========================================="
echo "Running Migration Step by Step"
echo "=========================================="
echo ""

cd ~/FurniMart-BE

echo "Step 1: Backup database..."
sudo docker exec user-db pg_dump -U postgres user_db > backups/backup_$(date +%Y%m%d_%H%M%S).sql
echo "✓ Backup created"

echo ""
echo "Step 2: Running migration_employees.sql..."
sudo docker exec -i user-db psql -U postgres -d user_db < migration_employees.sql
echo "✓ Employee migration completed"

echo ""
echo "Step 3: Checking if user_stores needs rename..."
USER_STORES_EXISTS=$(sudo docker exec user-db psql -U postgres -d user_db -t -c "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_stores');" | tr -d ' ')

if [ "$USER_STORES_EXISTS" = "t" ]; then
    echo "Renaming user_stores to employee_stores..."
    sudo docker exec -i user-db psql -U postgres -d user_db < migration_rename_user_stores_to_employee_stores.sql
    echo "✓ Table renamed"
else
    echo "✓ user_stores does not exist, skipping"
fi

echo ""
echo "Step 4: Removing unnecessary columns..."
sudo docker exec -i user-db psql -U postgres -d user_db < migration_remove_user_columns.sql
echo "✓ Columns removed"

echo ""
echo "Step 5: Verifying migration..."
sudo docker exec user-db psql -U postgres -d user_db -c "
SELECT 
    'employees' as table_name, COUNT(*) as count 
FROM employees 
WHERE is_deleted = false
UNION ALL
SELECT 
    'employee_stores' as table_name, COUNT(*) as count 
FROM employee_stores 
WHERE is_deleted = false;
"

echo ""
echo "Step 6: Rebuilding Docker services..."
sudo docker compose down
sudo docker compose build --no-cache user-service
sudo docker compose up -d

echo ""
echo "Waiting 30 seconds for services to start..."
sleep 30

echo ""
echo "Step 7: Checking services..."
sudo docker ps --filter name=user --format "table {{.Names}}\t{{.Status}}"

echo ""
echo "=========================================="
echo "Migration completed!"
echo "=========================================="

