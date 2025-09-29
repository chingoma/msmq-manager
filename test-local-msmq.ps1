# Test Local MSMQ Access
Write-Host "Testing local MSMQ access..." -ForegroundColor Yellow

try {
    Add-Type -AssemblyName System.Messaging
    
    # Test local queue access
    $localQueues = @("PrivateTransferReceive", "dse", "status_response_queue")
    
    foreach ($queueName in $localQueues) {
        Write-Host "`nTesting local queue: $queueName" -ForegroundColor Cyan
        try {
            $queuePath = ".\private$\$queueName"
            if ([System.Messaging.MessageQueue]::Exists($queuePath)) {
                $queue = New-Object System.Messaging.MessageQueue $queuePath
                $queue.Formatter = New-Object System.Messaging.XmlMessageFormatter([Type[]]@( [System.Xml.XmlDocument] ))
                
                $message = $queue.Peek([System.TimeSpan]::FromMilliseconds(1000))
                if ($message -ne $null) {
                    Write-Host "✓ Local queue '$queueName' exists and has messages" -ForegroundColor Green
                } else {
                    Write-Host "✓ Local queue '$queueName' exists but no messages" -ForegroundColor Green
                }
                $queue.Close()
            } else {
                Write-Host "✗ Local queue '$queueName' does not exist" -ForegroundColor Red
            }
        } catch {
            Write-Host "✗ Local queue '$queueName' error: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
} catch {
    Write-Host "✗ Local MSMQ access failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nLocal MSMQ test completed." -ForegroundColor Yellow
