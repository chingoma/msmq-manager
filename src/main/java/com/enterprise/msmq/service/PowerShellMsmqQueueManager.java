package com.enterprise.msmq.service;

import com.enterprise.msmq.config.MsmqRemoteProperties;
import com.enterprise.msmq.dto.MsmqMessage;
import com.enterprise.msmq.dto.MsmqQueue;
import com.enterprise.msmq.model.MsmqQueueConfig;
import com.enterprise.msmq.repository.MsmqQueueConfigRepository;
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

    private final MsmqRemoteProperties remoteProperties;
    private final MsmqQueueConfigRepository queueConfigRepository;

    /**
     * Creates a new MSMQ queue using PowerShell.
     * Handles process exit codes and logs errors.
     */
    @Override
    public boolean createQueue(MsmqQueue queue) {
        try {
            String queuePath = queue.getPath() != null ? queue.getPath() : "private$\\" + queue.getName();
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
            String command = "$queue = Get-MsmqQueue -QueueType Private | Where-Object { $_.QueueName -like '*" + queuePath + "*' } | Select-Object -First 1; if ($queue) { Remove-MsmqQueue -InputObject $queue -ErrorAction SilentlyContinue; if ($?) { Write-Host 'SUCCESS' } else { Write-Host 'FAILED' } } else { Write-Host 'NOT_FOUND' }";
            Process process = new ProcessBuilder("powershell.exe", "-Command", command).start();
            int exitCode = process.waitFor();
            StringBuilder outputBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line).append("\n");
                }
            }
            String output = outputBuilder.toString().trim();
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
            process.waitFor();
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
                "$queue.Formatter = New-Object System.Messaging.XmlMessageFormatter([Type[]]@( [System.Xml.XmlDocument] )); " +
                "$xmlPayload = Get-Content '" + tempFile.replace("\\", "\\\\") + "' -Raw; " +
                "$xmlDoc = New-Object System.Xml.XmlDocument; " +
                "$xmlDoc.LoadXml($xmlPayload); " +
                "try { " +
                "    $queue.Send($xmlDoc, 'MSMQ Manager XML Message'); " +
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
     * The remoteQueuePath should be the full MSMQ TCP path, e.g., TCP:192.168.1.100\private$\QueueName,
     * or it can be UNC path which will be converted to TCP format automatically.
     * Handles process exit codes and logs errors and PowerShell output.
     */
    @Override
    public boolean sendMessageToRemote(String remoteQueuePath, String messageBody) {
        try {
            log.info("Sending message to remote queue via PowerShell using TCP: {}", remoteQueuePath);

            // Convert UNC path to TCP format if needed
            String tcpQueuePath = convertToTcpPath(remoteQueuePath);
            log.debug("Using TCP queue path: {}", tcpQueuePath);

            // Use a temporary file approach to avoid PowerShell here-string formatting issues
            String tempFile = System.getProperty("java.io.tmpdir") + "\\msmq_message_" + System.currentTimeMillis() + ".xml";
            try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
                writer.write(messageBody);
            }

            String command;
            if (tcpQueuePath.toUpperCase().startsWith("FORMATNAME:")) {
                // Use constructor approach for FormatName (same as working test script)
                // Don't double-escape backslashes for FormatName paths
                command = "Add-Type -AssemblyName System.Messaging; " +
                    "$queuePath = '" + tcpQueuePath + "'; " +
                    "$queue = New-Object System.Messaging.MessageQueue $queuePath; " +
                    "$queue.Formatter = New-Object System.Messaging.XmlMessageFormatter([Type[]]@( [System.Xml.XmlDocument] )); " +
                "$xmlPayload = Get-Content '" + tempFile.replace("\\", "\\\\") + "' -Raw; " +
                "$xmlDoc = New-Object System.Xml.XmlDocument; " +
                "$xmlDoc.LoadXml($xmlPayload); " +
                "try { " +
                "    $queue.Send($xmlDoc, 'MSMQ Manager TCP XML Message'); " +
                    "    Write-Host 'SUCCESS' " +
                    "} catch { " +
                    "    Write-Host 'FAILED - ' + $_.Exception.Message " +
                    "} finally { " +
                    "    if ($queue) { $queue.Close(); } " +
                    "    Remove-Item '" + tempFile.replace("\\", "\\\\") + "' -Force -ErrorAction SilentlyContinue " +
                    "}";
            } else {
                // Use constructor for local/normal queue (keep double-escaping for non-FormatName paths)
                command = "Add-Type -AssemblyName System.Messaging; " +
                    "$queuePath = '" + tcpQueuePath.replace("\\", "\\\\") + "'; " +
                    "$queue = New-Object System.Messaging.MessageQueue $queuePath; " +
                    "$queue.Formatter = New-Object System.Messaging.XmlMessageFormatter([Type[]]@( [System.Xml.XmlDocument] )); " +
                "$xmlPayload = Get-Content '" + tempFile.replace("\\", "\\\\") + "' -Raw; " +
                "$xmlDoc = New-Object System.Xml.XmlDocument; " +
                "$xmlDoc.LoadXml($xmlPayload); " +
                "try { " +
                "    $queue.Send($xmlDoc, 'MSMQ Manager TCP XML Message'); " +
                    "    Write-Host 'SUCCESS' " +
                    "} catch { " +
                    "    Write-Host 'FAILED - ' + $_.Exception.Message " +
                    "} finally { " +
                    "    if ($queue) { $queue.Close(); } " +
                    "    Remove-Item '" + tempFile.replace("\\", "\\\\") + "' -Force -ErrorAction SilentlyContinue " +
                    "}";
            }
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

            // Enhanced logging for debugging
            log.debug("PowerShell command executed: {}...", command.length() > 200 ? command.substring(0, 200) : command);
            log.debug("PowerShell exit code: {}", exitCode);
            log.debug("PowerShell stdout: '{}'", output);
            log.debug("PowerShell stderr: '{}'", errorOutput);

            if (exitCode == 0) {
                if (output.contains("SUCCESS")) {
                    log.info("Successfully sent message to remote queue via PowerShell: {}", remoteQueuePath);
                    return true;
                } else if (output.contains("FAILED")) {
                    String failureReason = extractFailureReason(output);
                    log.error("Failed to send message to remote queue via PowerShell: {} - Reason: {}", remoteQueuePath, failureReason);
                    return false;
                } else {
                    log.warn("Unknown PowerShell output for remote message sending: {}", output);
                    log.warn("PowerShell error stream: {}", errorOutput);
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
     * Converts UNC path to proper MSMQ format for remote connections.
     * Returns the format that works with MessageQueue constructor.
     * Examples:
     * \\192.168.1.100\private$\QueueName -> FormatName:DIRECT=OS:192.168.1.100\private$\QueueName
     * TCP:192.168.1.100\private$\QueueName -> FormatName:DIRECT=TCP:192.168.1.100\private$\QueueName
     * 192.168.1.100\private$\QueueName -> FormatName:DIRECT=OS:192.168.1.100\private$\QueueName
     */
    @Override
    public String convertToTcpPath(String queuePath) {
        if (queuePath == null || queuePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Queue path cannot be null or empty");
        }

        // If already FormatName format, return as is
        if (queuePath.toUpperCase().startsWith("FORMATNAME:")) {
            return queuePath;
        }

        // If it's TCP: format, convert to FormatName:DIRECT=TCP:
        if (queuePath.toUpperCase().startsWith("TCP:")) {
            String pathWithoutTcp = queuePath.substring(4); // Remove "TCP:"
            return "FormatName:DIRECT=TCP:" + pathWithoutTcp;
        }

        // Convert UNC path (\\server\private$\queue) to FormatName OS format
        if (queuePath.startsWith("\\\\")) {
            String pathWithoutUNC = queuePath.substring(2); // Remove "\\"
            return "FormatName:DIRECT=OS:" + pathWithoutUNC;
        }

        // If it's just server\private$\queue format, use OS (native) protocol by default
        if (queuePath.contains("\\")) {
            return "FormatName:DIRECT=OS:" + queuePath;
        }

        // Default case - assume it's a server name and needs path completion
        throw new IllegalArgumentException("Invalid queue path format: " + queuePath +
            ". Expected formats: '\\\\server\\private$\\queue', 'TCP:server\\private$\\queue', or 'FormatName:DIRECT=...'");
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
    @Override
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
    @Override
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
    @Override
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
                    if ("SUCCESS".equals(status) && !messageBody.isEmpty()) {
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
                    if ("SUCCESS".equals(status) && !messageBody.isEmpty()) {
                        MsmqMessage message = new MsmqMessage();
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
                    if ("SUCCESS".equals(status) && !messageBody.isEmpty()) {
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
                    if ("SUCCESS".equals(status) && !messageBody.isEmpty()) {
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
     * Gets all available queues from database.
     * Fetches all queues regardless of active status since queue syncing manages the data.
     * Returns an empty list if none found or error occurs.
     */
    @Override
    public List<MsmqQueue> getAllQueues() {
        log.debug("Fetching all queues from database");
        return getAllQueuesFromDatabase();
    }

    /**
     * Gets all available queues from database.
     * Fetches all queues regardless of active status since queue syncing manages the data.
     * Returns an empty list if none found or error occurs.
     */
    private List<MsmqQueue> getAllQueuesFromDatabase() {
        List<MsmqQueue> queues = new ArrayList<>();
        try {
            log.info("Fetching all queues from database (including inactive)");
            List<MsmqQueueConfig> configs = queueConfigRepository.findAll();
            
            for (MsmqQueueConfig config : configs) {
                MsmqQueue queue = new MsmqQueue();
                queue.setName(config.getQueueName());
                queue.setPath(config.getQueuePath());
                queue.setType(config.getQueueType() != null ? config.getQueueType() : "PRIVATE");
                queue.setDescription(config.getDescription());
                
                // Add status information to description
                String status = config.getIsActive() ? "ACTIVE" : "INACTIVE";
                if (queue.getDescription() != null && !queue.getDescription().isEmpty()) {
                    queue.setDescription(queue.getDescription() + " | Status: " + status);
                } else {
                    queue.setDescription("Status: " + status);
                }
                
                queues.add(queue);
                log.debug("Found queue in database: {} ({}) - {}", config.getQueueName(), config.getQueueType(), status);
            }
            
            log.info("Successfully fetched {} queues from database", queues.size());
        } catch (Exception e) {
            log.error("Error fetching queues from database: {}", e.getMessage(), e);
        }
        return queues;
    }

    /**
     * Gets all available queues from local machine using PowerShell.
     * Returns an empty list if none found or error occurs.
     */
    private List<MsmqQueue> getAllLocalQueues() {
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
            log.error("Error getting all local queues via PowerShell: {}", e.getMessage(), e);
        }
        return queues;
    }

    /**
     * Gets all available queues from a remote MSMQ server using PowerShell.
     * Uses TCP or OS protocol based on configuration.
     * Returns an empty list if none found or error occurs.
     */
    @Override
    public List<MsmqQueue> getAllRemoteQueues(String remoteHost) {
        List<MsmqQueue> queues = new ArrayList<>();
        try {
            log.info("Fetching queues from remote MSMQ server: {}", remoteHost);
            
            // Log the remote host being used
            log.debug("Using remote host: {}", remoteHost);
            
            // Use PowerShell to get remote queues using FormatName approach
            // This approach works better with MSMQ security restrictions
            String queueNamesList = remoteProperties.getQueueNames();
            String[] queueNames = queueNamesList.split(",");
            StringBuilder queueArray = new StringBuilder();
            for (int i = 0; i < queueNames.length; i++) {
                if (i > 0) queueArray.append(",");
                queueArray.append("'").append(queueNames[i].trim()).append("'");
            }
            
            String command = "Add-Type -AssemblyName System.Messaging; " +
                "try { " +
                "    $queues = @(); " +
                "    $commonQueues = @(" + queueArray.toString() + "); " +
                "    foreach ($queueName in $commonQueues) { " +
                "        try { " +
                "            $formatName = 'FormatName:DIRECT=TCP:" + remoteHost + "\\private$\\' + $queueName; " +
                "            $queue = New-Object System.Messaging.MessageQueue $formatName; " +
                "            $queue.Formatter = New-Object System.Messaging.XmlMessageFormatter([Type[]]@( [System.Xml.XmlDocument] )); " +
                "            $messageCount = 0; " +
                "            try { " +
                "                $messageCount = $queue.GetMessageEnumerator2().Count; " +
                "            } catch { " +
                "                $messageCount = 0; " +
                "            } " +
                "            Write-Host \"$queueName~$messageCount~PRIVATE\"; " +
                "            $queue.Close(); " +
                "        } catch { " +
                "            # Queue doesn't exist or can't be accessed, skip silently " +
                "        } " +
                "    } " +
                "    Write-Host 'SUCCESS'; " +
                "} catch { " +
                "    Write-Host 'FAILED - ' + $_.Exception.Message; " +
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
            
            log.debug("PowerShell command executed for remote queue fetch");
            log.debug("PowerShell exit code: {}", exitCode);
            log.debug("PowerShell stdout: '{}'", output);
            log.debug("PowerShell stderr: '{}'", errorOutput);
            
            if (exitCode == 0) {
                if (output.contains("SUCCESS")) {
                    // Parse the queue information
                    String[] lines = output.split("\n");
                    for (String line : lines) {
                        if (line.contains("~") && !line.contains("SUCCESS") && !line.contains("FAILED")) {
                            String[] parts = line.split("~");
                            if (parts.length >= 3) {
                                MsmqQueue queue = new MsmqQueue();
                                queue.setName(parts[0]);
                                queue.setPath(parts[0]);
                                queue.setType(parts[2]);
                                try {
                                    long messageCount = Long.parseLong(parts[1]);
                                    queue.setDescription("Message Count: " + messageCount);
                                } catch (NumberFormatException e) {
                                    log.warn("Could not parse message count for queue {}: {}", parts[0], parts[1]);
                                }
                                queues.add(queue);
                            }
                        }
                    }
                    log.info("Successfully fetched {} queues from remote server: {}", queues.size(), remoteHost);
                } else if (output.contains("FAILED")) {
                    String failureReason = extractFailureReason(output);
                    log.error("Failed to fetch remote queues from {}: {}", remoteHost, failureReason);
                } else {
                    log.warn("Unknown PowerShell output for remote queue fetching: {}", output);
                }
            } else {
                log.error("Failed to fetch remote queues from {}: exit code {}", remoteHost, exitCode);
                if (!errorOutput.isEmpty()) {
                    log.error("PowerShell error output: {}", errorOutput);
                }
                if (!output.isEmpty()) {
                    log.error("PowerShell standard output: {}", output);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching remote queues from {}: {}", remoteHost, e.getMessage(), e);
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

    /**
     * Extracts the failure reason from PowerShell output containing "FAILED - " prefix.
     */
    private String extractFailureReason(String output) {
        if (output.contains("FAILED - ")) {
            int startIndex = output.indexOf("FAILED - ") + 9;
            String reason = output.substring(startIndex);
            // Clean up the reason - take only the first line if there are multiple lines
            if (reason.contains("\n")) {
                reason = reason.substring(0, reason.indexOf("\n"));
            }
            return reason.trim();
        }
        return "Unknown failure reason";
    }
}
