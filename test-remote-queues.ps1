# Test script to verify remote queue fetching PowerShell command
# This script tests the exact command used in the Java application

$remoteHost = $env:MSMQ_REMOTE_SERVER
if (-not $remoteHost) {
    $remoteHost = "192.168.2.170"  # Default fallback
}

Write-Host "Testing remote queue fetching for host: $remoteHost"
Write-Host "================================================"

try {
    Add-Type -AssemblyName System.Messaging
    
    Write-Host "Attempting to get private queues from remote machine..."
    $queues = [System.Messaging.MessageQueue]::GetPrivateQueuesByMachine($remoteHost)
    
    Write-Host "Found $($queues.Count) queues on remote machine"
    
    foreach ($queue in $queues) {
        try {
            $queueName = $queue.QueueName
            $messageCount = $queue.GetMessageEnumerator2().Count
            Write-Host "Queue: $queueName~$messageCount~PRIVATE"
        } catch {
            Write-Host "Queue: $($queue.QueueName)~0~PRIVATE (Error getting count: $($_.Exception.Message))"
        }
    }
    
    Write-Host "SUCCESS"
    
} catch {
    Write-Host "FAILED - $($_.Exception.Message)"
    Write-Host "Error details: $($_.Exception)"
}
