# Script Test: All Roles - Comprehensive Test
# Test tất cả các API theo từng role

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Comprehensive API Test by Role" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test STAFF functions
Write-Host ""
Write-Host ">>> Testing STAFF Functions..." -ForegroundColor Magenta
& "$PSScriptRoot\test-staff-functions.ps1"

# Test BRANCH_MANAGER functions
Write-Host ""
Write-Host ">>> Testing BRANCH_MANAGER Functions..." -ForegroundColor Magenta
& "$PSScriptRoot\test-branch-manager-functions.ps1"

# Test DELIVERY functions
Write-Host ""
Write-Host ">>> Testing DELIVERY Functions..." -ForegroundColor Magenta
& "$PSScriptRoot\test-delivery-functions.ps1"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "All Tests Completed!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

