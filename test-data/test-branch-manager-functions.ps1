# Script Test: Branch Manager Functions (BRANCH_MANAGER Role)
# Test các chức năng của BRANCH_MANAGER: monitor progress, update status

$BASE_URL = "http://152.53.227.115:8086"
$DELIVERY_SERVICE_URL = "http://152.53.227.115:8089"

# BRANCH_MANAGER credentials
$BRANCH_MANAGER_EMAIL = "branchmanager@furnimart.com"
$BRANCH_MANAGER_PASSWORD = "BranchManager@123"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Branch Manager Functions" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Login as BRANCH_MANAGER
Write-Host "Step 1: Login as BRANCH_MANAGER..." -ForegroundColor Yellow
$loginBody = @{
    email = $BRANCH_MANAGER_EMAIL
    password = $BRANCH_MANAGER_PASSWORD
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    $TOKEN = $loginResponse.data.token
    Write-Host "Login success!" -ForegroundColor Green
} catch {
    Write-Host "Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Get stores
Write-Host ""
Write-Host "Step 2: Get stores..." -ForegroundColor Yellow
try {
    $stores = Invoke-RestMethod -Uri "$BASE_URL/api/stores" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    if ($stores.data.Count -gt 0) {
        $STORE_ID = $stores.data[0].id
        Write-Host "  Store ID: $STORE_ID" -ForegroundColor Cyan
        Write-Host "  Store Name: $($stores.data[0].name)" -ForegroundColor Cyan
    } else {
        Write-Host "  No stores found" -ForegroundColor Yellow
        $STORE_ID = "8d46e317-0596-4413-81b6-1a526398b3d7"
    }
} catch {
    Write-Host "  Using default store ID" -ForegroundColor Yellow
    $STORE_ID = "8d46e317-0596-4413-81b6-1a526398b3d7"
}

# Step 3: Monitor delivery progress
Write-Host ""
Write-Host "Step 3: Monitor delivery progress for store..." -ForegroundColor Yellow
try {
    $progress = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/progress/store/$STORE_ID" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    Write-Host "  Delivery progress retrieved!" -ForegroundColor Green
    Write-Host "  Total assignments: $($progress.data.totalAssignments)" -ForegroundColor Cyan
    Write-Host "  Completed: $($progress.data.completedCount)" -ForegroundColor Cyan
    Write-Host "  In progress: $($progress.data.inProgressCount)" -ForegroundColor Cyan
} catch {
    Write-Host "  Get progress failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "    Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}

# Step 4: Get assignments by store
Write-Host ""
Write-Host "Step 4: Get delivery assignments by store..." -ForegroundColor Yellow
try {
    $assignments = Invoke-RestMethod -Uri "$DELIVERY_SERVICE_URL/api/delivery/assignments/store/$STORE_ID" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $TOKEN"}
    
    Write-Host "  Found $($assignments.data.Count) assignments" -ForegroundColor Green
    
    if ($assignments.data.Count -gt 0) {
        $assignmentId = $assignments.data[0].id
        Write-Host "  First assignment ID: $assignmentId" -ForegroundColor Cyan
        
        # Step 5: Update delivery status
        Write-Host ""
        Write-Host "Step 5: Update delivery status..." -ForegroundColor Yellow
        
        $currentStatus = $assignments.data[0].status
        $newStatus = "PREPARING"
        if ($currentStatus -eq "PREPARING") { $newStatus = "READY" }
        elseif ($currentStatus -eq "READY") { $newStatus = "IN_TRANSIT" }
        elseif ($currentStatus -eq "IN_TRANSIT") { $newStatus = "DELIVERED" }
        
        try {
            $updateUrl = "$DELIVERY_SERVICE_URL/api/delivery/assignments/$assignmentId/status?status=$newStatus"
            $updated = Invoke-RestMethod -Uri $updateUrl `
                -Method PUT `
                -Headers @{"Authorization" = "Bearer $TOKEN"}
            
            Write-Host "  Status updated successfully!" -ForegroundColor Green
            Write-Host "  Old status: $currentStatus" -ForegroundColor Cyan
            Write-Host "  New status: $($updated.data.status)" -ForegroundColor Cyan
        } catch {
            Write-Host "  Update status failed: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "  Get assignments failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Test completed!" -ForegroundColor Green

