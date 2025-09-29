# Test Status Listener with Local Queue
Write-Host "Testing status listener with local status_response_queue..." -ForegroundColor Yellow

try {
    Add-Type -AssemblyName System.Messaging
    
    $queueName = "status_response_queue"
    $queuePath = ".\private$\$queueName"
    
    Write-Host "`n1. Checking if queue exists..." -ForegroundColor Cyan
    if ([System.Messaging.MessageQueue]::Exists($queuePath)) {
        Write-Host "✓ Queue '$queueName' exists" -ForegroundColor Green
        
        Write-Host "`n2. Checking message count..." -ForegroundColor Cyan
        $queue = New-Object System.Messaging.MessageQueue $queuePath
        $queue.Formatter = New-Object System.Messaging.XmlMessageFormatter([Type[]]@( [System.Xml.XmlDocument] ))
        
        $messageCount = 0
        try {
            $messageCount = $queue.GetMessageEnumerator2().Count
            Write-Host "✓ Queue has $messageCount messages" -ForegroundColor Green
        } catch {
            Write-Host "Could not get message count: $($_.Exception.Message)" -ForegroundColor Yellow
        }
        
        Write-Host "`n3. Peeking at first message..." -ForegroundColor Cyan
        try {
            $message = $queue.Peek([System.TimeSpan]::FromMilliseconds(1000))
            if ($message -ne $null) {
                Write-Host "✓ Successfully peeked at message" -ForegroundColor Green
                Write-Host "Message ID: $($message.Id)" -ForegroundColor White
                Write-Host "Message Label: $($message.Label)" -ForegroundColor White
                Write-Host "Message Body (first 200 chars): $($message.Body.ToString().Substring(0, [Math]::Min(200, $message.Body.ToString().Length)))..." -ForegroundColor White
            } else {
                Write-Host "No messages found in queue" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "✗ Error peeking at message: $($_.Exception.Message)" -ForegroundColor Red
        }
        
        Write-Host "`n4. Testing message receive..." -ForegroundColor Cyan
        try {
            $message = $queue.Receive([System.TimeSpan]::FromMilliseconds(1000))
            if ($message -ne $null) {
                Write-Host "✓ Successfully received message" -ForegroundColor Green
                Write-Host "Message ID: $($message.Id)" -ForegroundColor White
                Write-Host "Message Label: $($message.Label)" -ForegroundColor White
                Write-Host "Message Body (first 200 chars): $($message.Body.ToString().Substring(0, [Math]::Min(200, $message.Body.ToString().Length)))..." -ForegroundColor White
                
                # Put the message back (since we received it)
                Write-Host "`n5. Putting message back in queue..." -ForegroundColor Cyan
                $queue.Send($message, "Test Message")
                Write-Host "✓ Message put back in queue" -ForegroundColor Green
            } else {
                Write-Host "No messages found in queue" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "✗ Error receiving message: $($_.Exception.Message)" -ForegroundColor Red
        }
        
        $queue.Close()
        
    } else {
        Write-Host "✗ Queue '$queueName' does not exist" -ForegroundColor Red
    }
    
} catch {
    Write-Host "✗ Test failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nTest completed." -ForegroundColor Yellow
