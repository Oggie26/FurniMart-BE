# PowerShell Script to Run Production Migration
# This script will SSH into production server and run migration

$Server = "nam@152.53.227.115"
$Password = "Namnam123@"
$RemotePath = "~/FurniMart-BE"

Write-Host "==========================================" -ForegroundColor Green
Write-Host "Production Migration - Automated Script" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""

# Check if Posh-SSH is installed
$poshSSHInstalled = Get-Module -ListAvailable -Name Posh-SSH
if (-not $poshSSHInstalled) {
    Write-Host "Installing Posh-SSH module..." -ForegroundColor Yellow
    Install-Module -Name Posh-SSH -Scope CurrentUser -Force -AllowClobber
    Import-Module Posh-SSH
}

Write-Host "Step 1: Uploading migration files..." -ForegroundColor Yellow

# Create secure password
$SecurePassword = ConvertTo-SecureString $Password -AsPlainText -Force
$Credential = New-Object System.Management.Automation.PSCredential("nam", $SecurePassword)

# Upload files
$Files = @(
    "migration_employees.sql",
    "migration_rename_user_stores_to_employee_stores.sql",
    "migration_remove_user_columns.sql",
    "check_database_state.sql",
    "production_migration.sh"
)

$Session = New-SSHSession -ComputerName "152.53.227.115" -Credential $Credential -AcceptKey

if ($Session) {
    Write-Host "✓ Connected to server" -ForegroundColor Green
    
    # Upload files
    foreach ($File in $Files) {
        if (Test-Path $File) {
            Write-Host "  Uploading $File..." -ForegroundColor Cyan
            Set-SCPFile -ComputerName "152.53.227.115" -Credential $Credential -LocalFile $File -RemotePath $RemotePath
        }
    }
    
    Write-Host ""
    Write-Host "Step 2: Making script executable..." -ForegroundColor Yellow
    $MakeExecutable = Invoke-SSHCommand -SessionId $Session.SessionId -Command "cd $RemotePath; chmod +x production_migration.sh"
    
    Write-Host ""
    Write-Host "Step 3: Running migration script..." -ForegroundColor Yellow
    Write-Host "This may take several minutes..." -ForegroundColor Cyan
    Write-Host ""
    
    # Run migration script
    $Result = Invoke-SSHCommand -SessionId $Session.SessionId -Command "cd $RemotePath; bash ./production_migration.sh"
    
    Write-Host ""
    Write-Host "Migration Output:" -ForegroundColor Green
    Write-Host $Result.Output
    
    if ($Result.ExitStatus -eq 0) {
        Write-Host ""
        Write-Host "==========================================" -ForegroundColor Green
        Write-Host "✓ Migration completed successfully!" -ForegroundColor Green
        Write-Host "==========================================" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "==========================================" -ForegroundColor Red
        Write-Host "✗ Migration failed with exit code: $($Result.ExitStatus)" -ForegroundColor Red
        Write-Host "Error: $($Result.Error)" -ForegroundColor Red
        Write-Host "==========================================" -ForegroundColor Red
    }
    
    # Check service status
    Write-Host ""
    Write-Host "Step 4: Checking service status..." -ForegroundColor Yellow
    $ServiceStatus = Invoke-SSHCommand -SessionId $Session.SessionId -Command "docker ps --filter name=user --format 'table {{.Names}}\t{{.Status}}'"
    Write-Host $ServiceStatus.Output
    
    # Close session
    Remove-SSHSession -SessionId $Session.SessionId | Out-Null
} else {
    Write-Host "✗ Failed to connect to server" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Check logs: ssh $Server 'docker logs user-service --tail 50'" -ForegroundColor White
Write-Host "2. Test API: http://152.53.227.115:8086/swagger-ui.html" -ForegroundColor White
Write-Host "3. Verify endpoints: GET /api/employees and GET /api/users" -ForegroundColor White
