# Test script to verify remote MSMQ connection
# Usage: .\test-remote-msmq-connection.ps1

$remoteHost = "192.168.2.170"
$queueName = "securities-settlement-queue"
# Use proper MSMQ format names for different protocols
$tcpQueuePath = "FormatName:DIRECT=TCP:$remoteHost\private$\$queueName"
$osQueuePath = "FormatName:DIRECT=OS:$remoteHost\private$\$queueName"

Write-Host "Testing remote MSMQ connection..." -ForegroundColor Yellow
Write-Host "Remote Host: $remoteHost" -ForegroundColor Cyan
Write-Host "Queue Name: $queueName" -ForegroundColor Cyan
Write-Host "TCP Path: $tcpQueuePath" -ForegroundColor Cyan
Write-Host "OS Path: $osQueuePath" -ForegroundColor Cyan
Write-Host ""

try {
    Write-Host "Step 1: Loading System.Messaging assembly..." -ForegroundColor Green
    Add-Type -AssemblyName System.Messaging
    Write-Host "✅ System.Messaging loaded successfully" -ForegroundColor Green

    Write-Host ""
    Write-Host "Step 2: Testing network connectivity to $remoteHost..." -ForegroundColor Green
    if (Test-Connection -ComputerName $remoteHost -Count 1 -Quiet) {
        Write-Host "✅ Network connectivity to $remoteHost is working" -ForegroundColor Green
    } else {
        Write-Host "❌ Cannot ping $remoteHost - network connectivity issue" -ForegroundColor Red
        exit 1
    }

    Write-Host ""
    Write-Host "Step 3: Testing TCP port 1801 (MSMQ TCP port)..." -ForegroundColor Green
    $tcpTest = Test-NetConnection -ComputerName $remoteHost -Port 1801 -WarningAction SilentlyContinue
    if ($tcpTest.TcpTestSucceeded) {
        Write-Host "✅ TCP port 1801 is open on $remoteHost" -ForegroundColor Green
    } else {
        Write-Host "⚠️ TCP port 1801 is not accessible on $remoteHost" -ForegroundColor Yellow
        Write-Host "   Will try OS (native) protocol instead..." -ForegroundColor Yellow
    }

    # Try TCP format first
    Write-Host ""
    Write-Host "Step 4a: Attempting to create MessageQueue object with TCP format..." -ForegroundColor Green
    try {
        $queue = New-Object System.Messaging.MessageQueue $tcpQueuePath
        Write-Host "✅ TCP MessageQueue object created successfully" -ForegroundColor Green
        $usingTcp = $true
    } catch {
        Write-Host "⚠️ TCP format failed: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "Step 4b: Trying OS (native) format..." -ForegroundColor Green
        $queue = New-Object System.Messaging.MessageQueue $osQueuePath
        Write-Host "✅ OS MessageQueue object created successfully" -ForegroundColor Green
        $usingTcp = $false
    }

    Write-Host ""
    Write-Host "Step 5: Setting up formatter..." -ForegroundColor Green
    $queue.Formatter = New-Object System.Messaging.ActiveXMessageFormatter
    Write-Host "✅ ActiveX formatter set successfully" -ForegroundColor Green

    Write-Host ""
    Write-Host "Step 6: Attempting to send test message..." -ForegroundColor Green
    $testMessage = "<test>Hello from PowerShell MSMQ test at $(Get-Date)</test>"
    $queue.Send($testMessage, "Test Message")

    if ($usingTcp) {
        Write-Host "✅ Test message sent successfully using TCP protocol!" -ForegroundColor Green
        Write-Host "✅ Use FormatName TCP format: $tcpQueuePath" -ForegroundColor Green
    } else {
        Write-Host "✅ Test message sent successfully using OS (native) protocol!" -ForegroundColor Green
        Write-Host "✅ Use FormatName OS format: $osQueuePath" -ForegroundColor Green
    }

    Write-Host ""
    Write-Host "🎉 Remote MSMQ connection is working!" -ForegroundColor Green

} catch {
    Write-Host ""
    Write-Host "❌ Error occurred during testing:" -ForegroundColor Red
    Write-Host "Error Type: $($_.Exception.GetType().Name)" -ForegroundColor Red
    Write-Host "Error Message: $($_.Exception.Message)" -ForegroundColor Red

    if ($_.Exception.InnerException) {
        Write-Host "Inner Exception: $($_.Exception.InnerException.Message)" -ForegroundColor Red
    }

    Write-Host ""
    Write-Host "Common solutions:" -ForegroundColor Yellow
    Write-Host "1. Ensure MSMQ is installed on both local and remote machines" -ForegroundColor Yellow
    Write-Host "2. Create the queue on the remote machine first" -ForegroundColor Yellow
    Write-Host "3. Use correct FormatName syntax for remote queues" -ForegroundColor Yellow
    Write-Host "4. Verify network connectivity between machines" -ForegroundColor Yellow
    Write-Host "5. Ensure proper MSMQ permissions are configured" -ForegroundColor Yellow

} finally {
    if ($queue) {
        try {
            $queue.Close()
            Write-Host "Queue connection closed." -ForegroundColor Gray
        } catch {
            # Ignore close errors
        }
    }
}
