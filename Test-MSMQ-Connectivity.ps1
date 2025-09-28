# MSMQ Connectivity Testing Script
# Author: GitHub Copilot
# Date: September 27, 2025
# Description: Tests MSMQ connectivity and checks status of local and remote queues
# Run as Administrator: Right-click on this file and select "Run with PowerShell"

# Make sure we load the System.Messaging assembly first
Add-Type -AssemblyName System.Messaging

# Set text color functions
function Write-Success($message) {
    Write-Host $message -ForegroundColor Green
}

function Write-Error($message) {
    Write-Host $message -ForegroundColor Red
}

function Write-Info($message) {
    Write-Host $message -ForegroundColor Cyan
}

function Write-Warning($message) {
    Write-Host $message -ForegroundColor Yellow
}

# Function to test if MSMQ is installed
function Test-MsmqInstallation {
    try {
        $msmqService = Get-Service MSMQ -ErrorAction Stop
        Write-Success "MSMQ Service is installed and its status is: $($msmqService.Status)"
        
        if ($msmqService.Status -ne "Running") {
            Write-Warning "MSMQ service is not running. Attempting to start..."
            Start-Service MSMQ
            $msmqService = Get-Service MSMQ
            
            if ($msmqService.Status -eq "Running") {
                Write-Success "Successfully started MSMQ service"
            } else {
                Write-Error "Failed to start MSMQ service"
                return $false
            }
        }
        
        return $true
    } catch {
        Write-Error "MSMQ Service is not installed on this computer"
        Write-Warning "Please install MSMQ via 'Windows Features' in Control Panel"
        Write-Info "Steps to install MSMQ:"
        Write-Info "1. Open Control Panel"
        Write-Info "2. Go to Programs > Programs and Features"
        Write-Info "3. Click 'Turn Windows features on or off'"
        Write-Info "4. Expand 'Microsoft Message Queue (MSMQ) Server'"
        Write-Info "5. Check 'Microsoft Message Queue (MSMQ) Server Core'"
        Write-Info "6. Click OK and restart if prompted"
        return $false
    }
}

# Function to list all local queues
function List-LocalQueues {
    Write-Info "Listing all local private queues..."
    
    try {
        $queues = [System.Messaging.MessageQueue]::GetPrivateQueuesByMachine(".")
        
        if ($queues.Count -eq 0) {
            Write-Warning "No private queues found on local machine."
            return
        }
        
        Write-Host "Found $($queues.Count) private queues:" -ForegroundColor Yellow
        
        foreach ($queue in $queues) {
            Write-Host "  - $($queue.QueueName)" -ForegroundColor White
            
            # Try to get message count
            try {
                $messageCount = ($queue.GetAllMessages()).Count
                Write-Host "    Messages: $messageCount" -ForegroundColor Gray
            } catch {
                Write-Host "    Unable to count messages (access denied)" -ForegroundColor Gray
            }
            
            # Check permissions
            try {
                $accessControl = $queue.GetAccessControl()
                Write-Host "    Has read access: Yes" -ForegroundColor Gray
            } catch {
                Write-Host "    Has read access: No" -ForegroundColor Gray
            }
        }
    } catch {
        Write-Error "Failed to list local queues: $($_.Exception.Message)"
    }
}

# Function to test network connectivity to a remote server
function Test-NetworkConnectivity {
    param(
        [Parameter(Mandatory=$true)]
        [string]$server
    )
    
    Write-Info "Testing network connectivity to $server..."
    
    # Test basic ping
    if (Test-Connection -ComputerName $server -Count 2 -Quiet) {
        Write-Success "Server $server is reachable via ping"
    } else {
        Write-Error "Server $server is NOT reachable via ping"
        Write-Warning "Check firewall settings or network connectivity"
    }
    
    # Test MSMQ ports
    $ports = @(1801, 2101, 2103, 2105)
    $portNames = @{
        1801 = "MSMQ-RPC"
        2101 = "MSMQ-DS"
        2103 = "MSMQ-PING"
        2105 = "MSMQ-UDP-MULTICAST"
    }
    
    foreach ($port in $ports) {
        $connection = New-Object System.Net.Sockets.TcpClient
        try {
            $timeoutTask = $connection.BeginConnect($server, $port, $null, $null)
            $success = $timeoutTask.AsyncWaitHandle.WaitOne(1000, $true)
            
            if ($success -and $connection.Connected) {
                Write-Success "Port $port ($($portNames[$port])) is open on $server"
            } else {
                Write-Error "Port $port ($($portNames[$port])) is NOT open on $server"
            }
        } catch {
            Write-Error "Port $port ($($portNames[$port])) is NOT open on $server - $($_.Exception.Message)"
        } finally {
            $connection.Close()
        }
    }
    
    # Check if remote MSMQ service is accessible
    try {
        $remoteService = Get-Service -ComputerName $server -Name "MSMQ" -ErrorAction Stop
        Write-Success "MSMQ Service on $server is $($remoteService.Status)"
    } catch {
        Write-Error "Cannot query MSMQ service status on $server - $($_.Exception.Message)"
        Write-Warning "⚠️ This may indicate permissions issues or that the service is not installed"
    }
}

# Function to test connection to a remote queue
function Test-RemoteQueue {
    param(
        [Parameter(Mandatory=$true)]
        [string]$server,
        
        [Parameter(Mandatory=$true)]
        [string]$queueName
    )
    
    # Test various format name patterns
    $formatOptions = @(
        "FormatName:DIRECT=TCP:$server\private$\$queueName",
        "FormatName:DIRECT=TCP:$server\\private$\\$queueName",
        "FormatName:DIRECT=OS:$server\private$\$queueName",
        "TCP:$server\private$\$queueName"
    )
    
    $success = $false
    foreach ($format in $formatOptions) {
        Write-Info "Testing connection to $format"
        
        # Test if we can access the queue
        try {
            $queue = New-Object System.Messaging.MessageQueue($format)
            
            # Attempt to peek a message to verify connectivity
            $queue.MessageReadPropertyFilter.Body = $true
            $queue.MessageReadPropertyFilter.Label = $true
            
            try {
                # Using Peek with very short timeout as we don't actually need a message
                $message = $queue.Peek([TimeSpan]::FromMilliseconds(100))
                Write-Success "Successfully connected to remote queue: $format"
                Write-Success "   Queue exists and has messages"
                $success = $true
                break
            } catch [System.Messaging.MessageQueueException] {
                # Check if the error is "timeout expired" which means queue exists but no messages
                if ($_.Exception.MessageQueueErrorCode -eq "IOTimeout") {
                    Write-Success "Successfully connected to remote queue: $format"
                    Write-Info "   Queue exists but contains no messages"
                    $success = $true
                    break
                } else {
                    Write-Error "Failed to peek message from queue - $($_.Exception.Message)"
                }
            }
        } catch {
            Write-Error "Failed to connect to remote queue using $format - $($_.Exception.Message)"
        }
    }
    
    return $success
}

# Function to send a simple test message to a queue
function Send-TestMessage {
    param(
        [Parameter(Mandatory=$true)]
        [string]$server,
        
        [Parameter(Mandatory=$true)]
        [string]$queueName
    )
    
    $formatName = "FormatName:DIRECT=TCP:$server\private$\$queueName"
    Write-Info "Sending test message to $formatName"
    
    try {
        $queue = New-Object System.Messaging.MessageQueue($formatName)
        $message = New-Object System.Messaging.Message
        $message.Body = "Test message sent at $(Get-Date) from $($env:COMPUTERNAME)"
        $message.Label = "Test Message"
        
        # Use string formatter to ensure text messages work
        $message.Formatter = New-Object System.Messaging.XmlMessageFormatter(@("System.String"))
        
        $queue.Send($message)
        Write-Success "Successfully sent test message to remote queue"
        return $true
    } catch {
        Write-Error "Failed to send test message to remote queue - $($_.Exception.Message)"
        return $false
    }
}

# Main menu function
function Show-Menu {
    Clear-Host
    Write-Host "======================================" -ForegroundColor Yellow
    Write-Host "     MSMQ Connectivity Testing Tool" -ForegroundColor Yellow
    Write-Host "======================================" -ForegroundColor Yellow
    Write-Host "1: Check MSMQ Installation"
    Write-Host "2: List Local Queues"
    Write-Host "3: Test Network Connectivity"
    Write-Host "4: Test Remote Queue"
    Write-Host "5: Send Test Message"
    Write-Host "6: Run All Tests"
    Write-Host "C: Configure Remote Server"
    Write-Host "Q: Quit"
    Write-Host "======================================" -ForegroundColor Yellow
}

# Function to configure remote server
function Configure-RemoteServer {
    Write-Host "Current remote server: $remoteServer" -ForegroundColor Yellow
    Write-Host "Current remote queue: $remoteQueue" -ForegroundColor Yellow
    Write-Host
    
    $newServer = Read-Host "Enter new remote server address [leave blank to keep current]"
    if ($newServer) {
        $script:remoteServer = $newServer
    }
    
    $newQueue = Read-Host "Enter new remote queue name [leave blank to keep current]"
    if ($newQueue) {
        $script:remoteQueue = $newQueue
    }
    
    Write-Success "Updated configuration:"
    Write-Host "Remote server: $remoteServer" -ForegroundColor Cyan
    Write-Host "Remote queue: $remoteQueue" -ForegroundColor Cyan
}

# Run all tests function
function Run-AllTests {
    Write-Info "`n>> Checking MSMQ Installation"
    $msmqInstalled = Test-MsmqInstallation
    if (-not $msmqInstalled) {
        return
    }

    Write-Info "`n>> Listing Local Queues"
    List-LocalQueues

    Write-Info "`n>> Testing Network Connectivity"
    Test-NetworkConnectivity -server $remoteServer

    Write-Info "`n>> Testing Remote Queue"
    Test-RemoteQueue -server $remoteServer -queueName $remoteQueue

    Write-Info "`n>> Sending Test Message"
    Send-TestMessage -server $remoteServer -queueName $remoteQueue
}

# Set initial remote server and queue
$remoteServer = "192.168.2.170"  # Default server
$remoteQueue = "crdb_to_dse"     # Default queue

# Main execution loop
do {
    Show-Menu
    $selection = Read-Host "Enter your selection"
    switch ($selection) {
        '1' {
            Test-MsmqInstallation
            pause
        }
        '2' {
            List-LocalQueues
            pause
        }
        '3' {
            Test-NetworkConnectivity -server $remoteServer
            pause
        }
        '4' {
            Test-RemoteQueue -server $remoteServer -queueName $remoteQueue
            pause
        }
        '5' {
            Send-TestMessage -server $remoteServer -queueName $remoteQueue
            pause
        }
        '6' {
            Run-AllTests
            pause
        }
        'c' {
            Configure-RemoteServer
            pause
        }
        'q' {
            return
        }
    }
} while ($selection -ne 'q')