# MSMQ Permission Fix Script
# This script helps diagnose and fix MSMQ access issues

Write-Host "MSMQ Permission Diagnostic and Fix Script" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green

$remoteHost = "192.168.2.170"

# 1. Check local MSMQ service status
Write-Host "`n1. Checking local MSMQ service..." -ForegroundColor Yellow
try {
    $msmqService = Get-Service MSMQ -ErrorAction Stop
    Write-Host "   Local MSMQ Service Status: $($msmqService.Status)" -ForegroundColor Green
} catch {
    Write-Host "   ERROR: MSMQ service not found or not running" -ForegroundColor Red
    exit 1
}

# 2. Check MSMQ security settings
Write-Host "`n2. Checking MSMQ security settings..." -ForegroundColor Yellow
try {
    $msmqSecurity = Get-ItemProperty -Path "HKLM:\SOFTWARE\Microsoft\MSMQ\Parameters" -Name "Security" -ErrorAction SilentlyContinue
    if ($msmqSecurity) {
        Write-Host "   MSMQ Security setting found" -ForegroundColor Green
    } else {
        Write-Host "   MSMQ Security setting not found (this might be the issue)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   Could not check MSMQ security settings" -ForegroundColor Yellow
}

# 3. Check MSMQ registry settings
Write-Host "`n3. Checking MSMQ registry settings..." -ForegroundColor Yellow
try {
    $msmqParams = Get-ItemProperty -Path "HKLM:\SOFTWARE\Microsoft\MSMQ\Parameters" -ErrorAction SilentlyContinue
    if ($msmqParams) {
        Write-Host "   MSMQ Parameters registry key exists" -ForegroundColor Green
        Write-Host "   Security: $($msmqParams.Security)" -ForegroundColor Cyan
        Write-Host "   MQISServer: $($msmqParams.MQISServer)" -ForegroundColor Cyan
    }
} catch {
    Write-Host "   Could not access MSMQ registry settings" -ForegroundColor Yellow
}

# 4. Test remote connectivity with different methods
Write-Host "`n4. Testing remote MSMQ connectivity..." -ForegroundColor Yellow

# Test 1: Basic network connectivity
Write-Host "   Testing network connectivity to $remoteHost..."
$pingResult = Test-NetConnection -ComputerName $remoteHost -Port 1801 -WarningAction SilentlyContinue
if ($pingResult.TcpTestSucceeded) {
    Write-Host "   ✓ Network connectivity to port 1801: OK" -ForegroundColor Green
} else {
    Write-Host "   ✗ Network connectivity to port 1801: FAILED" -ForegroundColor Red
}

# Test 2: Try to access remote MSMQ using different methods
Write-Host "   Testing MSMQ access methods..."

# Method 1: Try GetPrivateQueuesByMachine
Write-Host "   Method 1: GetPrivateQueuesByMachine..."
try {
    Add-Type -AssemblyName System.Messaging
    $queues = [System.Messaging.MessageQueue]::GetPrivateQueuesByMachine($remoteHost)
    Write-Host "   ✓ GetPrivateQueuesByMachine: SUCCESS (Found $($queues.Count) queues)" -ForegroundColor Green
} catch {
    Write-Host "   ✗ GetPrivateQueuesByMachine: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Method 2: Try using FormatName
Write-Host "   Method 2: FormatName approach..."
try {
    $formatName = "FormatName:DIRECT=TCP:$remoteHost\private$\*"
    $queue = New-Object System.Messaging.MessageQueue $formatName
    Write-Host "   ✓ FormatName approach: SUCCESS" -ForegroundColor Green
} catch {
    Write-Host "   ✗ FormatName approach: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# Method 3: Try using UNC path
Write-Host "   Method 3: UNC path approach..."
try {
    $uncPath = "\\$remoteHost\private$\*"
    $queue = New-Object System.Messaging.MessageQueue $uncPath
    Write-Host "   ✓ UNC path approach: SUCCESS" -ForegroundColor Green
} catch {
    Write-Host "   ✗ UNC path approach: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# 5. Provide solutions
Write-Host "`n5. Recommended Solutions:" -ForegroundColor Yellow
Write-Host "   If all methods failed, try these solutions:" -ForegroundColor Cyan
Write-Host "   " -NoNewline
Write-Host "   a) " -NoNewline -ForegroundColor Yellow
Write-Host "Run PowerShell as Administrator" -ForegroundColor White
Write-Host "   " -NoNewline
Write-Host "   b) " -NoNewline -ForegroundColor Yellow
Write-Host "Check MSMQ security settings on remote server" -ForegroundColor White
Write-Host "   " -NoNewline
Write-Host "   c) " -NoNewline -ForegroundColor Yellow
Write-Host "Enable MSMQ security on remote server:" -ForegroundColor White
Write-Host "      Set-ItemProperty -Path 'HKLM:\SOFTWARE\Microsoft\MSMQ\Parameters' -Name 'Security' -Value 1" -ForegroundColor Gray
Write-Host "   " -NoNewline
Write-Host "   d) " -NoNewline -ForegroundColor Yellow
Write-Host "Check Windows Firewall settings on both machines" -ForegroundColor White
Write-Host "   " -NoNewline
Write-Host "   e) " -NoNewline -ForegroundColor Yellow
Write-Host "Verify MSMQ is installed and running on remote server" -ForegroundColor White

Write-Host "`n6. Alternative Approach:" -ForegroundColor Yellow
Write-Host "   If remote access continues to fail, consider:" -ForegroundColor Cyan
Write-Host "   - Using local queue monitoring instead of remote" -ForegroundColor White
Write-Host "   - Setting up MSMQ replication or forwarding" -ForegroundColor White
Write-Host "   - Using a different authentication method" -ForegroundColor White

Write-Host "`nScript completed. Check the results above for next steps." -ForegroundColor Green
