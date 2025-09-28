# Test script for MSMQ installation check
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
        Write-Error "Failed to list local queues - $($_.Exception.Message)"
    }
}

# Run the MSMQ installation check
Write-Host "======================================" -ForegroundColor Yellow
Write-Host "     TESTING MSMQ INSTALLATION" -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Yellow
Test-MsmqInstallation

Write-Host "`n======================================" -ForegroundColor Yellow
Write-Host "     TESTING LOCAL QUEUES" -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Yellow
List-LocalQueues

Write-Host "`nPress any key to continue..."
$null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')