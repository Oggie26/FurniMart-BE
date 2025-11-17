# PowerShell Script để kiểm tra log trên server
# Usage: .\check-logs.ps1 -Service "user-service" -Lines 50

param(
    [Parameter(Mandatory=$false)]
    [string]$Service = "user-service",
    
    [Parameter(Mandatory=$false)]
    [int]$Lines = 50,
    
    [Parameter(Mandatory=$false)]
    [switch]$Follow,
    
    [Parameter(Mandatory=$false)]
    [string]$SearchTerm = "",
    
    [Parameter(Mandatory=$false)]
    [string]$Server = "nam@152.53.227.115",
    
    [Parameter(Mandatory=$false)]
    [string]$ProjectPath = "~/FurniMart-BE"
)

Write-Host "==========================================" -ForegroundColor Green
Write-Host "Checking Logs on Server" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Server: $Server" -ForegroundColor Cyan
Write-Host "Service: $Service" -ForegroundColor Cyan
Write-Host "Lines: $Lines" -ForegroundColor Cyan
Write-Host ""

# Check if Posh-SSH is installed
$poshSSHInstalled = Get-Module -ListAvailable -Name Posh-SSH
if (-not $poshSSHInstalled) {
    Write-Host "Installing Posh-SSH module..." -ForegroundColor Yellow
    Install-Module -Name Posh-SSH -Scope CurrentUser -Force -AllowClobber
    Import-Module Posh-SSH
}

# Create secure password (you may need to update this)
$Password = Read-Host "Enter password for $Server" -AsSecureString
$Credential = New-Object System.Management.Automation.PSCredential("nam", $Password)

# Connect to server
Write-Host "Connecting to server..." -ForegroundColor Yellow
$Session = New-SSHSession -ComputerName "152.53.227.115" -Credential $Credential -AcceptKey

if ($Session) {
    Write-Host "✓ Connected to server" -ForegroundColor Green
    Write-Host ""
    
    # Build docker logs command
    $LogCommand = "cd $ProjectPath; docker logs"
    
    if ($Follow) {
        $LogCommand += " -f"
    } else {
        $LogCommand += " --tail $Lines"
    }
    
    $LogCommand += " $Service"
    
    if ($SearchTerm) {
        $LogCommand += " 2>&1 | grep -i `"$SearchTerm`""
    }
    
    Write-Host "Executing: $LogCommand" -ForegroundColor Cyan
    Write-Host ""
    
    if ($Follow) {
        Write-Host "Following logs (Press Ctrl+C to stop)..." -ForegroundColor Yellow
        Write-Host ""
        # For follow mode, we need to use SSH stream
        $Stream = New-SSHShellStream -SessionId $Session.SessionId
        $Stream.WriteLine($LogCommand)
        
        while ($true) {
            if ($Stream.DataAvailable) {
                $Output = $Stream.Read()
                Write-Host $Output -NoNewline
            }
            Start-Sleep -Milliseconds 100
        }
    } else {
        # Execute command
        $Result = Invoke-SSHCommand -SessionId $Session.SessionId -Command $LogCommand
        
        Write-Host "Log Output:" -ForegroundColor Green
        Write-Host "==========================================" -ForegroundColor Green
        Write-Host $Result.Output
        
        if ($Result.Error) {
            Write-Host ""
            Write-Host "Errors:" -ForegroundColor Red
            Write-Host $Result.Error -ForegroundColor Red
        }
        
        Write-Host ""
        Write-Host "==========================================" -ForegroundColor Green
        Write-Host "Exit Status: $($Result.ExitStatus)" -ForegroundColor $(if ($Result.ExitStatus -eq 0) { "Green" } else { "Red" })
    }
    
    # Close session
    Remove-SSHSession -SessionId $Session.SessionId | Out-Null
} else {
    Write-Host "✗ Failed to connect to server" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Done!" -ForegroundColor Green

