# PowerShell Script to Upload Migration Files to Production Server
# Usage: .\upload_migration_files.ps1

$Server = "nam@152.53.227.115"
$RemotePath = "~/FurniMart-BE"
$Password = "Namnam123@"

$Files = @(
    "migration_employees.sql",
    "migration_rename_user_stores_to_employee_stores.sql",
    "migration_remove_user_columns.sql",
    "check_database_state.sql",
    "production_migration.sh"
)

Write-Host "==========================================" -ForegroundColor Green
Write-Host "Uploading Migration Files to Production" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""

foreach ($File in $Files) {
    if (Test-Path $File) {
        Write-Host "Uploading $File..." -ForegroundColor Yellow
        scp $File "${Server}:${RemotePath}/"
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ $File uploaded successfully" -ForegroundColor Green
        } else {
            Write-Host "✗ Failed to upload $File" -ForegroundColor Red
        }
    } else {
        Write-Host "✗ File not found: $File" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "Upload Complete!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. SSH into server: ssh $Server" -ForegroundColor White
Write-Host "2. Navigate to project: cd $RemotePath" -ForegroundColor White
Write-Host "3. Make script executable: chmod +x production_migration.sh" -ForegroundColor White
Write-Host "4. Run migration: ./production_migration.sh" -ForegroundColor White

