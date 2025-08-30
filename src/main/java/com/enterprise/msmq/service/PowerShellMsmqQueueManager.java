package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.MsmqMessage;
import com.enterprise.msmq.dto.MsmqQueue;
import com.enterprise.msmq.service.contracts.IMsmqQueueManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PowerShell-based MSMQ Queue Manager.
 * 
 * This implementation uses PowerShell MSMQ cmdlets and .NET System.Messaging
 * for reliable MSMQ operations. It implements the IMsmqQueueManager interface
 * to provide consistent queue management operations.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PowerShellMsmqQueueManager implements IMsmqQueueManager {

    @Override
    public boolean createQueue(MsmqQueue queue) {
        try {
            String queuePath = queue.getPath() != null ? queue.getPath() : "private$\\" + queue.getName();
            String queueName = queuePath.replace("private$\\", "");
            String command = "New-MsmqQueue -Name '" + queuePath + "' -QueueType Private -ErrorAction SilentlyContinue";
            
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("Successfully created queue via PowerShell: {}", queuePath);
                return true;
            } else {
                log.error("Failed to create queue via PowerShell: {}, exit code: {}", queuePath, exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("Error creating queue {} via PowerShell: {}", queue.getName(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean deleteQueue(String queuePath) {
        try {
            // For PowerShell MSMQ, we need to get the queue object first, then remove it
            String command = "$queue = Get-MsmqQueue -QueueType Private | Where-Object { $_.QueueName -like '*" + queuePath + "*' } | Select-Object -First 1; if ($queue) { Remove-MsmqQueue -InputObject $queue -ErrorAction SilentlyContinue; if ($?) { Write-Host 'SUCCESS' } else { Write-Host 'FAILED' } } else { Write-Host 'NOT_FOUND' }";
            
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            int exitCode = process.waitFor();
            
            // Read the output to check for our specific messages
            String output = "";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                StringBuilder outputBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line).append("\n");
                }
                output = outputBuilder.toString().trim();
            }
            
            if (exitCode == 0) {
                if (output.contains("SUCCESS")) {
                    log.info("Successfully deleted queue via PowerShell: {}", queuePath);
                    return true;
                } else if (output.contains("FAILED")) {
                    log.error("Failed to delete queue via PowerShell: {}", queuePath);
                    return false;
                } else if (output.contains("NOT_FOUND")) {
                    log.warn("Queue not found in MSMQ: {}", queuePath);
                    return false;
                } else {
                    log.warn("Unknown PowerShell output for queue deletion: {}", output);
                    return false;
                }
            } else {
                log.error("Failed to delete queue via PowerShell: {}, exit code: {}", queuePath, exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("Error deleting queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean queueExists(String queuePath) {
        try {
            String command = "Get-MsmqQueue -QueueType Private | Where-Object { $_.QueueName -like '*" + queuePath + "*' } | Select-Object -First 1";
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            int exitCode = process.waitFor();
            
            // Read output to see if queue exists
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().length() > 0) {
                        return true; // Queue found
                    }
                }
            }
            
            return false; // No queue found
            
        } catch (Exception e) {
            log.error("Error checking if queue exists {} via PowerShell: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendMessage(String queuePath, MsmqMessage message) {
        return sendMessage(queuePath, message.getBody());
    }

    @Override
    public boolean sendMessage(String queuePath, String messageBody) {
        try {
            // Use ActiveXMessageFormatter to send messages as raw text without any parsing or encoding
            String queuePathWithPrefix = ".\\private$\\" + queuePath;
            
            // Escape the message properly for PowerShell to preserve formatting
            String escapedMessage = messageBody
                .replace("\\", "\\\\")
                .replace("'", "''")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
            
            String command = "Add-Type -AssemblyName System.Messaging; " +
                "$queuePath = '" + queuePathWithPrefix + "'; " +
                "if (-not [System.Messaging.MessageQueue]::Exists($queuePath)) { " +
                "    [System.Messaging.MessageQueue]::Create($queuePath, $true) | Out-Null " +
                "}; " +
                "$queue = New-Object System.Messaging.MessageQueue $queuePath; " +
                "$queue.Formatter = New-Object System.Messaging.ActiveXMessageFormatter; " +
                "$queue.Send('" + escapedMessage + "', 'MSMQ Manager Message'); " +
                "$queue.Close(); " +
                "if ($?) { Write-Host 'SUCCESS' } else { Write-Host 'FAILED' }";
            
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            int exitCode = process.waitFor();
            
            // Read the output to check for our specific messages
            String output = "";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                StringBuilder outputBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line).append("\n");
                }
                output = outputBuilder.toString().trim();
            }
            
            if (exitCode == 0) {
                if (output.contains("SUCCESS")) {
                    log.info("Successfully sent message to queue via PowerShell: {}", queuePath);
                    return true;
                } else if (output.contains("FAILED")) {
                    log.error("Failed to send message to queue via PowerShell: {}", queuePath);
                    return false;
                } else {
                    log.warn("Unknown PowerShell output for message sending: {}", output);
                    return false;
                }
            } else {
                log.error("Failed to send message to PowerShell: {}, exit code: {}", queuePath, exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("Error sending message to queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Optional<MsmqMessage> receiveMessage(String queuePath) {
        try {
            String command = "Add-Type -AssemblyName System.Messaging; " +
                "$queuePath = '.\\private$\\" + queuePath + "'; " +
                "if ([System.Messaging.MessageQueue]::Exists($queuePath)) { " +
                "    $queue = New-Object System.Messaging.MessageQueue $queuePath; " +
                "    $queue.Formatter = New-Object System.Messaging.ActiveXMessageFormatter; " +
                "    $message = $queue.Receive([System.Messaging.MessageQueueTransaction]::Single); " +
                "    if ($message) { " +
                "        Write-Host $message.Body; " +
                "        Write-Host 'SUCCESS' " +
                "    } else { " +
                "        Write-Host 'NO_MESSAGE' " +
                "    } " +
                "    $queue.Close(); " +
                "} else { " +
                "    Write-Host 'QUEUE_NOT_FOUND' " +
                "}";
            
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    StringBuilder messageBody = new StringBuilder();
                    String status = "";
                    
                    while ((line = reader.readLine()) != null) {
                        if (line.equals("SUCCESS") || line.equals("NO_MESSAGE") || line.equals("QUEUE_NOT_FOUND")) {
                            status = line;
                            break;
                        } else {
                            messageBody.append(line).append("\n");
                        }
                    }
                    
                    if ("SUCCESS".equals(status) && messageBody.length() > 0) {
                        MsmqMessage message = new MsmqMessage();
                        message.setMessageId(UUID.randomUUID().toString());
                        message.setBody(messageBody.toString().trim());
                        message.setSourceQueue(queuePath);
                        message.setCreatedTime(java.time.LocalDateTime.now());
                        return Optional.of(message);
                    } else if ("NO_MESSAGE".equals(status)) {
                        return Optional.empty();
                    } else {
                        log.warn("Queue not found or error receiving message: {}", status);
                        return Optional.empty();
                    }
                }
            } else {
                log.error("Failed to receive message from queue: {}, exit code: {}", queuePath, exitCode);
            }
            
        } catch (Exception e) {
            log.error("Error receiving message from queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
        }
        
        return Optional.empty();
    }

    @Override
    public Optional<MsmqMessage> peekMessage(String queuePath) {
        try {
            String command = "Add-Type -AssemblyName System.Messaging; " +
                "$queuePath = '.\\private$\\" + queuePath + "'; " +
                "if ([System.Messaging.MessageQueue]::Exists($queuePath)) { " +
                "    $queue = New-Object System.Messaging.MessageQueue $queuePath; " +
                "    $queue.Formatter = New-Object System.Messaging.ActiveXMessageFormatter; " +
                "    $message = $queue.Peek(); " +
                "    if ($message) { " +
                "        Write-Host $message.Body; " +
                "        Write-Host 'SUCCESS' " +
                "    } else { " +
                "        Write-Host 'NO_MESSAGE' " +
                "    } " +
                "    $queue.Close(); " +
                "} else { " +
                "    Write-Host 'QUEUE_NOT_FOUND' " +
                "}";
            
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    StringBuilder messageBody = new StringBuilder();
                    String status = "";
                    
                    while ((line = reader.readLine()) != null) {
                        if (line.equals("SUCCESS") || line.equals("NO_MESSAGE") || line.equals("QUEUE_NOT_FOUND")) {
                            status = line;
                            break;
                        } else {
                            messageBody.append(line).append("\n");
                        }
                    }
                    
                    if ("SUCCESS".equals(status) && messageBody.length() > 0) {
                        MsmqMessage message = new MsmqMessage();
                        message.setMessageId(UUID.randomUUID().toString());
                        message.setBody(messageBody.toString().trim());
                        message.setSourceQueue(queuePath);
                        message.setCreatedTime(java.time.LocalDateTime.now());
                        return Optional.of(message);
                    } else if ("NO_MESSAGE".equals(status)) {
                        return Optional.empty();
                    } else {
                        log.warn("Queue not found or error peeking message: {}", status);
                        return Optional.empty();
                    }
                }
            } else {
                log.error("Failed to peek message from queue: {}, exit code: {}", queuePath, exitCode);
            }
            
        } catch (Exception e) {
            log.error("Error peeking message from queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
        }
        
        return Optional.empty();
    }

    @Override
    public boolean purgeQueue(String queuePath) {
        try {
            String command = "Add-Type -AssemblyName System.Messaging; " +
                "$queuePath = '.\\private$\\" + queuePath + "'; " +
                "if ([System.Messaging.MessageQueue]::Exists($queuePath)) { " +
                "    $queue = New-Object System.Messaging.MessageQueue $queuePath; " +
                "    $queue.Purge(); " +
                "    $queue.Close(); " +
                "    Write-Host 'SUCCESS' " +
                "} else { " +
                "    Write-Host 'QUEUE_NOT_FOUND' " +
                "}";
            
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.equals("SUCCESS")) {
                            log.info("Successfully purged queue via PowerShell: {}", queuePath);
                            return true;
                        } else if (line.equals("QUEUE_NOT_FOUND")) {
                            log.warn("Queue not found for purging: {}", queuePath);
                            return false;
                        }
                    }
                }
            } else {
                log.error("Failed to purge queue: {}, exit code: {}", queuePath, exitCode);
            }
            
        } catch (Exception e) {
            log.error("Error purging queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
        }
        
        return false;
    }

    @Override
    public List<MsmqQueue> getAllQueues() {
        List<MsmqQueue> queues = new ArrayList<>();
        try {
            String command = "Get-MsmqQueue -QueueType Private | Select-Object QueueName, QueueType, MessageCount";
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().length() > 0 && !line.contains("QueueName")) {
                            // Parse queue information (simplified parsing)
                            String[] parts = line.split("\\s+");
                            if (parts.length >= 2) {
                                MsmqQueue queue = new MsmqQueue();
                                queue.setName(parts[0]);
                                queue.setPath("private$\\" + parts[0]);
                                queue.setType("PRIVATE");
                                queues.add(queue);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error getting all queues via PowerShell: {}", e.getMessage(), e);
        }
        
        return queues;
    }

    @Override
    public Optional<MsmqQueue> getQueue(String queuePath) {
        try {
            if (queueExists(queuePath)) {
                MsmqQueue queue = new MsmqQueue();
                queue.setName(queuePath.replace("private$\\", ""));
                queue.setPath(queuePath);
                queue.setType("PRIVATE");
                return Optional.of(queue);
            }
        } catch (Exception e) {
            log.error("Error getting queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
        }
        
        return Optional.empty();
    }

    @Override
    public long getMessageCount(String queuePath) {
        try {
            String command = "Get-MsmqQueue -QueueType Private | Where-Object { $_.QueueName -like '*" + queuePath + "*' } | Select-Object -ExpandProperty MessageCount";
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && line.trim().length() > 0) {
                        try {
                            return Long.parseLong(line.trim());
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse message count: {}", line);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error getting message count for queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
        }
        
        return -1;
    }

    @Override
    public boolean testConnectivity() {
        try {
            // Test basic PowerShell MSMQ cmdlet availability
            String command = "Get-Command Get-MsmqQueue -ErrorAction SilentlyContinue";
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.debug("PowerShell MSMQ connectivity test passed");
                return true;
            } else {
                log.warn("PowerShell MSMQ cmdlets not available, but .NET System.Messaging may still work");
                return true; // Don't fail connectivity test for missing cmdlets
            }
            
        } catch (Exception e) {
            log.error("PowerShell MSMQ connectivity test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateQueue(String queuePath, MsmqQueue queue) {
        try {
            log.info("Updating queue via PowerShell MSMQ: {}", queuePath);
            // PowerShell MSMQ doesn't have a direct update method
            // We'll need to delete and recreate the queue
            if (deleteQueue(queuePath)) {
                return createQueue(queue);
            }
            return false;
        } catch (Exception e) {
            log.error("Error updating queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Optional<MsmqQueue> getQueueStatistics(String queuePath) {
        try {
            log.debug("Getting queue statistics via PowerShell MSMQ: {}", queuePath);
            // PowerShell MSMQ provides basic queue information
            if (queueExists(queuePath)) {
                MsmqQueue queue = new MsmqQueue();
                queue.setName(queuePath.replace("private$\\", ""));
                queue.setPath(queuePath);
                queue.setType("PRIVATE");
                // TODO: Add more statistics like message count, etc.
                return Optional.of(queue);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting queue statistics for {} via PowerShell: {}", queuePath, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<MsmqMessage> receiveMessage(String queuePath, long timeout) {
        // For now, ignore timeout and use the basic receive method
        // TODO: Implement timeout-based receiving
        return receiveMessage(queuePath);
    }

    @Override
    public Optional<MsmqMessage> peekMessage(String queuePath, long timeout) {
        // For now, ignore timeout and use the basic peek method
        // TODO: Implement timeout-based peeking
        return peekMessage(queuePath);
    }
}
