# Test MSMQ Connection to Remote Server
Write-Host "Testing MSMQ connection to 192.168.2.170..." -ForegroundColor Yellow

# Test 1: Basic network connectivity
Write-Host "`n1. Testing network connectivity..." -ForegroundColor Cyan
try {
    $result = Test-NetConnection -ComputerName 192.168.2.170 -Port 1801 -InformationLevel Quiet
    if ($result) {
        Write-Host "✓ Network connectivity to port 1801: OK" -ForegroundColor Green
    } else {
        Write-Host "✗ Network connectivity to port 1801: FAILED" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "✗ Network connectivity test failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 2: MSMQ queue access
Write-Host "`n2. Testing MSMQ queue access..." -ForegroundColor Cyan
try {
    Add-Type -AssemblyName System.Messaging
    
    # Test different queue names
    $queueNames = @("PrivateTransferReceive", "dse", "status_response_queue")
    
    foreach ($queueName in $queueNames) {
        Write-Host "`nTesting queue: $queueName" -ForegroundColor Yellow
        try {
            $formatName = "FormatName:DIRECT=TCP:192.168.2.170\private$\$queueName"
            $queue = New-Object System.Messaging.MessageQueue $formatName
            $queue.Formatter = New-Object System.Messaging.XmlMessageFormatter([Type[]]@( [System.Xml.XmlDocument] ))
            
            # Try to peek at messages (non-destructive)
            $message = $queue.Peek([System.TimeSpan]::FromMilliseconds(1000))
            if ($message -ne $null) {
                Write-Host "✓ Queue '$queueName' exists and has messages" -ForegroundColor Green
            } else {
                Write-Host "✓ Queue '$queueName' exists but no messages" -ForegroundColor Green
            }
            $queue.Close()
        } catch {
            Write-Host "✗ Queue '$queueName' error: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
} catch {
    Write-Host "✗ MSMQ access failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Check MSMQ security settings
Write-Host "`n3. Checking MSMQ security settings..." -ForegroundColor Cyan
try {
    $security = Get-ItemProperty -Path "HKLM:\SOFTWARE\Microsoft\MSMQ\Parameters" -Name "Security" -ErrorAction SilentlyContinue
    if ($security) {
        Write-Host "MSMQ Security setting: $($security.Security)" -ForegroundColor Yellow
        if ($security.Security -eq 1) {
            Write-Host "⚠ MSMQ Security is ENABLED - this may cause access issues" -ForegroundColor Red
        } else {
            Write-Host "✓ MSMQ Security is disabled" -ForegroundColor Green
        }
    } else {
        Write-Host "MSMQ Security setting not found" -ForegroundColor Yellow
    }
} catch {
    Write-Host "Could not check MSMQ security settings: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`nTest completed." -ForegroundColor Yellow
