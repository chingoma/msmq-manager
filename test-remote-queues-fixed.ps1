# Test script for the fixed remote queue fetching approach
# This uses the FormatName method that works with MSMQ security restrictions

$remoteHost = $env:MSMQ_REMOTE_SERVER
if (-not $remoteHost) {
    $remoteHost = "192.168.2.170"  # Default fallback
}
$queueNames = @("securities-settlement-queue", "testqueue", "orders-queue", "settlements-queue", "payment-queue", "notification-queue")

Write-Host "Testing Fixed Remote Queue Fetching" -ForegroundColor Green
Write-Host "====================================" -ForegroundColor Green
Write-Host "Remote Host: $remoteHost" -ForegroundColor Cyan
Write-Host "Queue Names: $($queueNames -join ', ')" -ForegroundColor Cyan
Write-Host ""

try {
    Add-Type -AssemblyName System.Messaging
    
    $foundQueues = 0
    foreach ($queueName in $queueNames) {
        try {
            Write-Host "Checking queue: $queueName" -NoNewline
            $formatName = "FormatName:DIRECT=TCP:$remoteHost\private$\$queueName"
            $queue = New-Object System.Messaging.MessageQueue $formatName
            $queue.Formatter = New-Object System.Messaging.XmlMessageFormatter([Type[]]@( [System.Xml.XmlDocument] ))
            
            $messageCount = 0
            try {
                $messageCount = $queue.GetMessageEnumerator2().Count
            } catch {
                $messageCount = 0
            }
            
            Write-Host " - FOUND ($messageCount messages)" -ForegroundColor Green
            Write-Host "  Format: $formatName" -ForegroundColor Gray
            Write-Host "  Output: $queueName~$messageCount~PRIVATE" -ForegroundColor Gray
            $foundQueues++
            
            $queue.Close()
        } catch {
            Write-Host " - NOT FOUND" -ForegroundColor Yellow
            Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    Write-Host ""
    Write-Host "Summary: Found $foundQueues out of $($queueNames.Count) queues" -ForegroundColor Cyan
    
    if ($foundQueues -gt 0) {
        Write-Host "SUCCESS: Remote queue access is working!" -ForegroundColor Green
    } else {
        Write-Host "WARNING: No queues found. Check queue names and remote server configuration." -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Error details: $($_.Exception)" -ForegroundColor Red
}
