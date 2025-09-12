# Test MSMQ PowerShell functionality
Write-Host "Testing MSMQ PowerShell functionality..." -ForegroundColor Green

try {
    # Add MSMQ assembly
    Add-Type -AssemblyName System.Messaging
    
    # Test queue path
    $queuePath = ".\private$\test-msmq-queue"
    Write-Host "Queue path: $queuePath"
    
    # Check if queue exists, create if not
    if (-not [System.Messaging.MessageQueue]::Exists($queuePath)) {
        Write-Host "Creating queue..." -ForegroundColor Yellow
        [System.Messaging.MessageQueue]::Create($queuePath, $true) | Out-Null
        Write-Host "Queue created successfully" -ForegroundColor Green
    } else {
        Write-Host "Queue already exists" -ForegroundColor Green
    }
    
    # Create queue object
    $queue = New-Object System.Messaging.MessageQueue $queuePath
    $queue.Formatter = New-Object System.Messaging.ActiveXMessageFormatter
    
    # Test message
    $testMessage = "Test message from PowerShell at $(Get-Date)"
    Write-Host "Sending test message: $testMessage" -ForegroundColor Yellow
    
    # Send message
    $queue.Send($testMessage, "Test Message")
    Write-Host "Message sent successfully!" -ForegroundColor Green
    
    # Check message count
    $messageCount = $queue.GetAllMessages().Count
    Write-Host "Messages in queue: $messageCount" -ForegroundColor Green
    
    # Close queue
    $queue.Close()
    
    Write-Host "MSMQ PowerShell test completed successfully!" -ForegroundColor Green
    
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Stack trace: $($_.Exception.StackTrace)" -ForegroundColor Red
}
