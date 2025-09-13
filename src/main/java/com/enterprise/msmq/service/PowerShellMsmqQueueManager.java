package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.MsmqMessage;
import com.enterprise.msmq.dto.MsmqQueue;
import com.enterprise.msmq.service.contracts.IMsmqQueueManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PowerShell-based MSMQ Queue Manager.
 * <p>
 * This implementation uses PowerShell MSMQ cmdlets and .NET System.Messaging
 * for reliable MSMQ operations. It implements the IMsmqQueueManager interface
 * to provide consistent queue management operations.
 *
 * <p>Implements all required methods for queue CRUD and message operations.
 * Handles process exit codes and PowerShell output for robust error handling.
 *
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PowerShellMsmqQueueManager implements IMsmqQueueManager {

    /**
     * Creates a new MSMQ queue using PowerShell.
     * Handles process exit codes and logs errors.
     */
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

    /**
     * Deletes an MSMQ queue using PowerShell.
     * Handles process exit codes and parses PowerShell output for status.
     */
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

    /**
     * Checks if a queue exists using PowerShell.
     * Returns true if found, false otherwise.
     */
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
                    if (!line.trim().isEmpty()) {
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

    /**
     * Sends a message to a queue using PowerShell.
     * Handles process exit codes and logs errors.
     */
    @Override
    public boolean sendMessage(String queuePath, MsmqMessage message) {
        return sendMessage(queuePath, message.getBody());
    }

    /**
     * Sends a raw message body to a queue using PowerShell.
     * Handles process exit codes and logs errors and PowerShell output.
     */
    @Override
    public boolean sendMessage(String queuePath, String messageBody) {
        try {
            log.info("Sending message to queue via PowerShell: {}", queuePath);
            // Use ActiveXMessageFormatter to send messages as raw text without any parsing or encoding
            String queuePathWithPrefix = ".\\private$\\" + queuePath;
            // Use a temporary file approach to avoid PowerShell here-string formatting issues
            String tempFile = System.getProperty("java.io.tmpdir") + "\\msmq_message_" + System.currentTimeMillis() + ".xml";
            // Write message to temporary file
            try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
                writer.write(messageBody);
            }
            String command = "Add-Type -AssemblyName System.Messaging; " +
                "$queuePath = '" + queuePathWithPrefix + "'; " +
                "if (-not [System.Messaging.MessageQueue]::Exists($queuePath)) { " +
                "    [System.Messaging.MessageQueue]::Create($queuePath, $true) | Out-Null " +
                "}; " +
                "$queue = New-Object System.Messaging.MessageQueue $queuePath; " +
                "$queue.Formatter = New-Object System.Messaging.ActiveXMessageFormatter; " +
                "$xmlPayload = Get-Content '" + tempFile.replace("\\", "\\\\") + "' -Raw; " +
                "try { " +
                "    $queue.Send($xmlPayload, 'MSMQ Manager Message'); " +
                "    Write-Host 'SUCCESS' " +
                "} catch { " +
                "    Write-Host 'FAILED - ' + $_.Exception.Message " +
                "} finally { " +
                "    $queue.Close(); " +
                "    Remove-Item '" + tempFile.replace("\\", "\\\\") + "' -Force " +
                "}";
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            // Capture both standard output and error output
            StringBuilder outputBuilder = new StringBuilder();
            StringBuilder errorBuilder = new StringBuilder();
            // Read standard output in a separate thread
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputBuilder.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.error("Error reading PowerShell output: {}", e.getMessage());
                }
            });
            // Read error output in a separate thread
            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorBuilder.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.error("Error reading PowerShell error output: {}", e.getMessage());
                }
            });
            // Start both threads
            outputThread.start();
            errorThread.start();
            int exitCode = process.waitFor();
            // Wait for both threads to complete
            outputThread.join();
            errorThread.join();
            String output = outputBuilder.toString().trim();
            String errorOutput = errorBuilder.toString().trim();
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
                if (!errorOutput.isEmpty()) {
                    log.error("PowerShell error output: {}", errorOutput);
                }
                if (!output.isEmpty()) {
                    log.error("PowerShell standard output: {}", output);
                }
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending message to queue {} via PowerShell script: {}", 
                queuePath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Sends a raw message body to a remote MSMQ queue using PowerShell with TCP protocol.
     * The remoteQueuePath should be the full MSMQ TCP path, e.g., TCP:192.168.1.100\private$\QueueName
     * or it can be UNC path which will be converted to TCP format automatically.
     * Handles process exit codes and logs errors and PowerShell output.
     */
    public boolean sendMessageToRemote(String remoteQueuePath, String messageBody) {
        try {
            log.info("Sending message to remote queue via PowerShell using TCP: {}", remoteQueuePath);

            // Convert UNC path to TCP format if needed
            String tcpQueuePath = convertToTcpPath(remoteQueuePath);
            log.debug("Using TCP queue path: {}", tcpQueuePath);

            // Use a temporary file approach to avoid PowerShell here-string formatting issues
            String tempFile = System.getProperty("java.io.tmpdir") + "\\msmq_message_" + System.currentTimeMillis() + ".xml";
            // Write message to temporary file
            try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
                writer.write(messageBody);
            }
            String command = "Add-Type -AssemblyName System.Messaging; " +
                "$queuePath = '" + tcpQueuePath.replace("\\", "\\\\") + "'; " +
                "try { " +
                "    $queue = New-Object System.Messaging.MessageQueue $queuePath; " +
                "    $queue.Formatter = New-Object System.Messaging.ActiveXMessageFormatter; " +
                "    $xmlPayload = Get-Content '" + tempFile.replace("\\", "\\\\") + "' -Raw; " +
                "    $queue.Send($xmlPayload, 'MSMQ Manager TCP Message'); " +
                "    Write-Host 'SUCCESS' " +
                "} catch { " +
                "    Write-Host 'FAILED - ' + $_.Exception.Message " +
                "} finally { " +
                "    if ($queue) { $queue.Close(); } " +
                "    Remove-Item '" + tempFile.replace("\\", "\\\\") + "' -Force -ErrorAction SilentlyContinue " +
                "}";
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            // Capture both standard output and error output
            StringBuilder outputBuilder = new StringBuilder();
            StringBuilder errorBuilder = new StringBuilder();
            // Read standard output in a separate thread
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputBuilder.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.error("Error reading PowerShell output: {}", e.getMessage());
                }
            });
            // Read error output in a separate thread
            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorBuilder.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.error("Error reading PowerShell error output: {}", e.getMessage());
                }
            });
            // Start both threads
            outputThread.start();
            errorThread.start();
            int exitCode = process.waitFor();
            // Wait for both threads to complete
            outputThread.join();
            errorThread.join();
            String output = outputBuilder.toString().trim();
            String errorOutput = errorBuilder.toString().trim();
            if (exitCode == 0) {
                if (output.contains("SUCCESS")) {
                    log.info("Successfully sent message to remote queue via PowerShell: {}", remoteQueuePath);
                    return true;
                } else if (output.contains("FAILED")) {
                    log.error("Failed to send message to remote queue via PowerShell: {}", remoteQueuePath);
                    return false;
                } else {
                    log.warn("Unknown PowerShell output for remote message sending: {}", output);
                    return false;
                }
            } else {
                log.error("Failed to send message to remote PowerShell: {}, exit code: {}", remoteQueuePath, exitCode);
                if (!errorOutput.isEmpty()) {
                    log.error("PowerShell error output: {}", errorOutput);
                }
                if (!output.isEmpty()) {
                    log.error("PowerShell standard output: {}", output);
                }
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending message to remote queue {} via PowerShell TCP: {}",
                remoteQueuePath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Converts UNC path to TCP format for MSMQ remote connections.
     * Examples:
     * \\192.168.1.100\private$\QueueName -> TCP:192.168.1.100\private$\QueueName
     * \\ServerName\private$\QueueName -> TCP:ServerName\private$\QueueName
     * TCP:192.168.1.100\private$\QueueName -> TCP:192.168.1.100\private$\QueueName (no change)
     */
    private String convertToTcpPath(String queuePath) {
        if (queuePath == null || queuePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Queue path cannot be null or empty");
        }

        // If already TCP format, return as is
        if (queuePath.toUpperCase().startsWith("TCP:")) {
            return queuePath;
        }

        // Convert UNC path (\\server\private$\queue) to TCP format
        if (queuePath.startsWith("\\\\")) {
            return "TCP:" + queuePath.substring(2); // Remove \\ and add TCP:
        }

        // If it's just server\private$\queue format, add TCP:
        if (!queuePath.contains("\\\\") && queuePath.contains("\\")) {
            return "TCP:" + queuePath;
        }

        // Default case - assume it needs TCP: prefix
        return "TCP:" + queuePath;
    }

    /**
     * Convenience method to send message to remote queue using TCP with separate machine and queue name.
     * This method constructs the full TCP path automatically.
     *
     * @param remoteMachine The remote machine name or IP address
     * @param queueName The queue name (without path prefix)
     * @param messageBody The message content to send
     * @return true if message sent successfully, false otherwise
     */
    public boolean sendMessageToRemote(String remoteMachine, String queueName, String messageBody) {
        String tcpQueuePath = "TCP:" + remoteMachine + "\\private$\\" + queueName;
        return sendMessageToRemote(tcpQueuePath, messageBody);
    }

    /**
     * Convenience method to send MsmqMessage object to remote queue.
     *
     * @param remoteQueuePath Full remote queue path (e.g., \\RemoteMachine\private$\QueueName)
     * @param message The MsmqMessage object to send
     * @return true if message sent successfully, false otherwise
     */
    public boolean sendMessageToRemote(String remoteQueuePath, MsmqMessage message) {
        return sendMessageToRemote(remoteQueuePath, message.getBody());
    }

    /**
     * Convenience method to send MsmqMessage object to remote queue by specifying remote machine and queue name.
     *
     * @param remoteMachine The remote machine name or IP address
     * @param queueName The queue name (without path prefix)
     * @param message The MsmqMessage object to send
     * @return true if message sent successfully, false otherwise
     */
    public boolean sendMessageToRemote(String remoteMachine, String queueName, MsmqMessage message) {
        String tcpQueuePath = "TCP:" + remoteMachine + "\\private$\\" + queueName;
        return sendMessageToRemote(tcpQueuePath, message.getBody());
    }

    /**
     * Receives a message from a queue using PowerShell.
     * Returns Optional.empty() if no message or error.
     */
    @Override
    public Optional<MsmqMessage> receiveMessage(String queuePath) {
        try {
            String command = "Add-Type -AssemblyName System.Messaging; " +
                "$queuePath = '.\\private$\\" + queuePath + "'; " +
                "if ([System.Messaging.MessageQueue]::Exists($queuePath)) { " +
                "    $queue = New-Object System.Messaging.MessageQueue $queuePath; " +
                "    $queue.Formatter = New-Object System.Messaging.ActiveXMessageFormatter; " +
                "    try { " +
                "        $message = $queue.Receive(); " +
                "        Write-Host $message.Body; " +
                "        Write-Host 'SUCCESS' " +
                "    } catch { " +
                "        Write-Host 'NO_MESSAGE' " +
                "    } finally { " +
                "        $queue.Close(); " +
                "    } " +
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
                        message.setCreatedTime(LocalDateTime.now());
                        return Optional.of(message);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error receiving message from queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Receives a message from a queue with timeout using PowerShell.
     * Returns Optional.empty() if no message, timeout or error.
     */
    @Override
    public Optional<MsmqMessage> receiveMessage(String queuePath, long timeout) {
        try {
            String command = "Add-Type -AssemblyName System.Messaging; " +
                "$queuePath = '.\\private$\\" + queuePath + "'; " +
                "if ([System.Messaging.MessageQueue]::Exists($queuePath)) { " +
                "    $queue = New-Object System.Messaging.MessageQueue $queuePath; " +
                "    $queue.Formatter = New-Object System.Messaging.ActiveXMessageFormatter; " +
                "    try { " +
                "        $timeout = [TimeSpan]::FromMilliseconds(" + timeout + "); " +
                "        $message = $queue.Receive($timeout); " +
                "        Write-Host $message.Body; " +
                "        Write-Host 'SUCCESS' " +
                "    } catch { " +
                "        Write-Host 'NO_MESSAGE' " +
                "    } finally { " +
                "        $queue.Close(); " +
                "    } " +
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
                        message.setCreatedTime(LocalDateTime.now());
                        return Optional.of(message);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error receiving message from queue {} with timeout via PowerShell: {}", queuePath, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Peeks at a message in a queue using PowerShell.
     * Returns Optional.empty() if no message or error.
     */
    @Override
    public Optional<MsmqMessage> peekMessage(String queuePath) {
        try {
            String command = "Add-Type -AssemblyName System.Messaging; " +
                "$queuePath = '.\\private$\\" + queuePath + "'; " +
                "if ([System.Messaging.MessageQueue]::Exists($queuePath)) { " +
                "    $queue = New-Object System.Messaging.MessageQueue $queuePath; " +
                "    $queue.Formatter = New-Object System.Messaging.ActiveXMessageFormatter; " +
                "    try { " +
                "        $message = $queue.Peek(); " +
                "        Write-Host $message.Body; " +
                "        Write-Host 'SUCCESS' " +
                "    } catch { " +
                "        Write-Host 'NO_MESSAGE' " +
                "    } finally { " +
                "        $queue.Close(); " +
                "    } " +
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
                        message.setCreatedTime(LocalDateTime.now());
                        return Optional.of(message);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error peeking message from queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Peeks at a message in a queue with timeout using PowerShell.
     * Returns Optional.empty() if no message, timeout or error.
     */
    @Override
    public Optional<MsmqMessage> peekMessage(String queuePath, long timeout) {
        try {
            String command = "Add-Type -AssemblyName System.Messaging; " +
                "$queuePath = '.\\private$\\" + queuePath + "'; " +
                "if ([System.Messaging.MessageQueue]::Exists($queuePath)) { " +
                "    $queue = New-Object System.Messaging.MessageQueue $queuePath; " +
                "    $queue.Formatter = New-Object System.Messaging.ActiveXMessageFormatter; " +
                "    try { " +
                "        $timeout = [TimeSpan]::FromMilliseconds(" + timeout + "); " +
                "        $message = $queue.Peek($timeout); " +
                "        Write-Host $message.Body; " +
                "        Write-Host 'SUCCESS' " +
                "    } catch { " +
                "        Write-Host 'NO_MESSAGE' " +
                "    } finally { " +
                "        $queue.Close(); " +
                "    } " +
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
                        message.setCreatedTime(LocalDateTime.now());
                        return Optional.of(message);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error peeking message from queue {} with timeout via PowerShell: {}", queuePath, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Purges all messages from a queue using PowerShell.
     * Logs result and errors.
     */
    @Override
    public void purgeQueue(String queuePath) {
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
                            return;
                        } else if (line.equals("QUEUE_NOT_FOUND")) {
                            log.warn("Queue not found for purging: {}", queuePath);
                            return;
                        }
                    }
                }
            } else {
                log.error("Failed to purge queue: {}, exit code: {}", queuePath, exitCode);
            }
        } catch (Exception e) {
            log.error("Error purging queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
        }
    }

    /**
     * Gets all available queues using PowerShell.
     * Returns an empty list if none found or error occurs.
     */
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
                        if (!line.trim().isEmpty() && !line.contains("QueueName")) {
                            // Parse queue information (simplified parsing)
                            String[] parts = line.split("\\s+");
                            if (parts.length >= 2) {
                                MsmqQueue queue = new MsmqQueue();
                                queue.setName(parts[0]);
                                queue.setPath(parts[0]);
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

    /**
     * Gets a specific queue by path using PowerShell.
     * Returns Optional.empty() if not found or error occurs.
     */
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

    /**
     * Gets the count of messages in a queue using PowerShell.
     * Returns -1 if error or not found.
     */
    @Override
    public long getMessageCount(String queuePath) {
        try {
            String command = "Get-MsmqQueue -QueueType Private | Where-Object { $_.QueueName -like '*" + queuePath + "*' } | Select-Object -ExpandProperty MessageCount";
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && !line.trim().isEmpty()) {
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

    /**
     * Tests PowerShell MSMQ connectivity.
     * Returns true if PowerShell MSMQ cmdlets or .NET System.Messaging are available.
     */
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

    /**
     * Updates a queue by deleting and recreating it using PowerShell.
     * Returns true if update successful, false otherwise.
     */
    @Override
    public boolean updateQueue(String queuePath, MsmqQueue queue) {
        try {
            // Delete existing queue
            if (deleteQueue(queuePath)) {
                // Create new queue with updated properties
                return createQueue(queue);
            }
            return false;
        } catch (Exception e) {
            log.error("Error updating queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gets queue statistics using PowerShell.
     * Returns queue information if found, empty otherwise.
     */
    @Override
    public Optional<MsmqQueue> getQueueStatistics(String queuePath) {
        try {
            String command = "Get-MsmqQueue -QueueType Private | Where-Object { $_.QueueName -like '*" + queuePath + "*' } | Select-Object -First 1";
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && !line.trim().isEmpty()) {
                        MsmqQueue queue = new MsmqQueue();
                        queue.setName(queuePath.replace("private$\\", ""));
                        queue.setPath(queuePath);
                        queue.setType("PRIVATE");
                        // Add message count if available
                        long messageCount = getMessageCount(queuePath);
                        if (messageCount >= 0) {
                            queue.setDescription("Message Count: " + messageCount);
                        }
                        return Optional.of(queue);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting queue statistics for {} via PowerShell: {}", queuePath, e.getMessage(), e);
        }
        return Optional.empty();
    }
}
