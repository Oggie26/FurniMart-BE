#!/bin/bash

# Production Migration Script
# Server: nam@152.53.227.115
# Database: user_db
# Purpose: Migrate User/Employee separation and remove unnecessary columns

set -e  # Exit on error

echo "=========================================="
echo "PRODUCTION MIGRATION SCRIPT"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
DB_CONTAINER="user-db"
DB_NAME="user_db"
DB_USER="postgres"
BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
# Try docker without sudo first, fallback to sudo if needed
if docker ps > /dev/null 2>&1; then
    DOCKER_CMD="docker"
else
    DOCKER_CMD="sudo docker"
fi

# Create backup directory
mkdir -p $BACKUP_DIR

echo -e "${GREEN}Step 1: Checking database state...${NC}"
$DOCKER_CMD exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -f check_database_state.sql

echo ""
echo -e "${GREEN}Step 2: Creating database backup...${NC}"
BACKUP_FILE="$BACKUP_DIR/backup_user_db_$TIMESTAMP.sql"
$DOCKER_CMD exec $DB_CONTAINER pg_dump -U $DB_USER $DB_NAME > $BACKUP_FILE

if [ -f "$BACKUP_FILE" ] && [ -s "$BACKUP_FILE" ]; then
    echo -e "${GREEN}✓ Backup created successfully: $BACKUP_FILE${NC}"
    ls -lh $BACKUP_FILE
else
    echo -e "${RED}✗ Backup failed! Aborting migration.${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}Step 3: Running migration_employees.sql...${NC}"
$DOCKER_CMD exec -i $DB_CONTAINER psql -U $DB_USER -d $DB_NAME < migration_employees.sql

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Employee migration completed${NC}"
else
    echo -e "${RED}✗ Employee migration failed!${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}Step 4: Checking if user_stores table needs to be renamed...${NC}"
USER_STORES_EXISTS=$($DOCKER_CMD exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -t -c "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_stores');" | tr -d ' ')

if [ "$USER_STORES_EXISTS" = "t" ]; then
    echo -e "${YELLOW}user_stores table exists, running rename migration...${NC}"
    $DOCKER_CMD exec -i $DB_CONTAINER psql -U $DB_USER -d $DB_NAME < migration_rename_user_stores_to_employee_stores.sql
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Table rename completed${NC}"
    else
        echo -e "${RED}✗ Table rename failed!${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ user_stores table does not exist, skipping rename${NC}"
fi

echo ""
echo -e "${GREEN}Step 5: Verifying migration...${NC}"
$DOCKER_CMD exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "
SELECT 
    'Migration Summary' as summary,
    (SELECT COUNT(*) FROM employees WHERE is_deleted = false) as employees_count,
    (SELECT COUNT(*) FROM employee_stores WHERE is_deleted = false) as employee_stores_count,
    (SELECT COUNT(*) FROM users u INNER JOIN accounts a ON u.account_id = a.id WHERE a.role IN ('SELLER', 'BRANCH_MANAGER', 'DELIVERER', 'STAFF', 'ADMIN') AND u.is_deleted = false) as employees_still_in_users;
"

echo ""
echo -e "${GREEN}Step 6: Removing unnecessary columns from users table...${NC}"
$DOCKER_CMD exec -i $DB_CONTAINER psql -U $DB_USER -d $DB_NAME < migration_remove_user_columns.sql

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Columns removed successfully${NC}"
else
    echo -e "${RED}✗ Column removal failed!${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}Step 7: Final verification...${NC}"
$DOCKER_CMD exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "
SELECT 
    column_name,
    data_type
FROM information_schema.columns
WHERE table_name = 'users'
AND column_name IN ('department', 'position', 'salary')
ORDER BY column_name;
"

echo ""
echo -e "${GREEN}Step 8: Rebuilding Docker services (WITHOUT removing volumes)...${NC}"
echo -e "${YELLOW}Stopping containers...${NC}"
$DOCKER_CMD compose down

echo -e "${YELLOW}Rebuilding images...${NC}"
$DOCKER_CMD compose build --no-cache user-service

echo -e "${YELLOW}Starting services...${NC}"
$DOCKER_CMD compose up -d

echo ""
echo -e "${GREEN}Step 9: Waiting for services to start...${NC}"
sleep 30

echo ""
echo -e "${GREEN}Step 10: Checking service status...${NC}"
$DOCKER_CMD ps --filter "name=user" --format "table {{.Names}}\t{{.Status}}"

echo ""
echo -e "${GREEN}Step 11: Checking user-service logs...${NC}"
$DOCKER_CMD logs user-service --tail 30

echo ""
echo -e "${GREEN}=========================================="
echo -e "MIGRATION COMPLETED!"
echo -e "==========================================${NC}"
echo ""
echo "Backup location: $BACKUP_FILE"
echo ""
echo "Next steps:"
echo "1. Verify API endpoints are working"
echo "2. Check Swagger UI: http://152.53.227.115:8086/swagger-ui.html"
echo "3. Test GET /api/employees and GET /api/users endpoints"

