package com.enterprise.msmq.util;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PowerShell-based MSMQ Connection Manager.
 * 
 * This implementation uses PowerShell MSMQ cmdlets instead of the failing native API.
 * PowerShell MSMQ cmdlets are more reliable and provide better error handling.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Component
public class PowerShellMsmqQueueManager {

    private static final Logger logger = LoggerFactory.getLogger(PowerShellMsmqQueueManager.class);

    public boolean createQueue(String queuePath) {
        try {
//            if (!isConnected.get()) {
//                if (!connect()) {
//                    return false;
//                }
//            }

            String queueName = queuePath.replace("private$\\", "");
            String command = "New-MsmqQueue -Name '" + queuePath + "' -QueueType Private -ErrorAction SilentlyContinue";
            
            Process process = Runtime.getRuntime().exec("powershell.exe -Command \"" + command + "\"");
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                logger.info("Successfully created queue via PowerShell: {}", queuePath);
                return true;
            } else {
                logger.error("Failed to create queue via PowerShell: {}, exit code: {}", queuePath, exitCode);
                return false;
            }

        } catch (Exception e) {
            logger.error("Error creating queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteQueue(String queuePath) {
        try {
            // For PowerShell MSMQ, we need to get the queue object first, then remove it
            // Using a more reliable approach with explicit error handling
            String command = "$queue = Get-MsmqQueue -QueueType Private | Where-Object { $_.QueueName -like '*" + queuePath + "*' } | Select-Object -First 1; if ($queue) { Remove-MsmqQueue -InputObject $queue -ErrorAction SilentlyContinue; if ($?) { Write-Host 'SUCCESS' } else { Write-Host 'FAILED' } } else { Write-Host 'NOT_FOUND' }";
            
            Process process = Runtime.getRuntime().exec("powershell.exe -Command \"" + command + "\"");
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
                    logger.info("Successfully deleted queue via PowerShell: {}", queuePath);
                    return true;
                } else if (output.contains("FAILED")) {
                    logger.error("Failed to delete queue via PowerShell: {}", queuePath);
                    return false;
                } else if (output.contains("NOT_FOUND")) {
                    logger.warn("Queue not found in MSMQ: {}", queuePath);
                    return false;
                } else {
                    logger.warn("Unknown PowerShell output for queue deletion: {}", output);
                    return false;
                }
            } else {
                logger.error("Failed to delete queue via PowerShell: {}, exit code: {}", queuePath, exitCode);
                return false;
            }

        } catch (Exception e) {
            logger.error("Error deleting queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    public boolean sendMessage(String queuePath, String message) {
        try {
            // Use ActiveXMessageFormatter to send messages as raw text without any parsing or encoding
            // This ensures the message content is sent exactly as provided - perfect for XML and plain text
            String queuePathWithPrefix = ".\\private$\\" + queuePath;
            
            // Escape the message properly for PowerShell to preserve formatting
            String escapedMessage = message
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
            
            Process process = Runtime.getRuntime().exec("powershell.exe -Command \"" + command + "\"");
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
                    logger.info("Successfully sent message to queue via PowerShell: {}", queuePath);
                    return true;
                } else if (output.contains("FAILED")) {
                    logger.error("Failed to send message to queue via PowerShell: {}", queuePath);
                    return false;
                } else {
                    logger.warn("Unknown PowerShell output for message sending: {}", output);
                    return false;
                }
            } else {
                logger.error("Failed to send message to PowerShell: {}, exit code: {}", queuePath, exitCode);
                return false;
            }

        } catch (Exception e) {
            logger.error("Error sending message to queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    public boolean sendMessageBackup(String queuePath, String message) {
        try {
            // Use ActiveXMessageFormatter to send messages as raw text without any parsing or encoding
            // This ensures the message content is sent exactly as provided - perfect for XML and plain text
            String queuePathWithPrefix = ".\\private$\\" + queuePath;
            
            // Escape the message properly for PowerShell to preserve formatting
            // Use simple escaping instead of here-string syntax for better compatibility
            String escapedMessage = message
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

            Process process = Runtime.getRuntime().exec("powershell.exe -Command \"" + command + "\"");
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
                    logger.info("Successfully sent message to queue via PowerShell: {}", queuePath);
                    return true;
                } else if (output.contains("FAILED")) {
                    logger.error("Failed to send message to queue via PowerShell: {}", queuePath);
                    return false;
                } else {
                    logger.warn("Unknown PowerShell output for message sending: {}", output);
                    return false;
                }
            } else {
                logger.error("Failed to send message to PowerShell: {}, exit code: {}", queuePath, exitCode);
                return false;
            }

        } catch (Exception e) {
            logger.error("Error sending message to queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    public String receiveMessage(String queuePath) {
        try {
            // Use ActiveXMessageFormatter consistently for both sending and receiving
            // This ensures messages are handled as raw text without any parsing or encoding
            String queuePathWithPrefix = ".\\private$\\" + queuePath;
            String command = "Add-Type -AssemblyName System.Messaging; $queuePath = '" + queuePathWithPrefix + "'; $queue = New-Object System.Messaging.MessageQueue $queuePath; $queue.Formatter = New-Object System.Messaging.ActiveXMessageFormatter; $msg = $queue.Receive([System.Messaging.MessageQueueTransactionType]::Single); if ($msg) { Write-Host 'BODY:' $msg.Body; Write-Host 'LABEL:' $msg.Label; Write-Host 'PRIORITY:' $msg.Priority; Write-Host 'CORRELATIONID:' $msg.CorrelationId; Write-Host 'MESSAGEID:' $msg.Id; Write-Host 'SUCCESS' } else { Write-Host 'NO_MESSAGE' }; $queue.Close()";
            
            Process process = Runtime.getRuntime().exec("powershell.exe -Command \"" + command + "\"");
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
                    logger.info("Successfully received message from queue via PowerShell: {}", queuePath);
                    // Extract message body from output
                    String[] lines = output.split("\n");
                    for (String line : lines) {
                        if (line.startsWith("BODY:")) {
                            String body = line.substring("BODY:".length()).trim();
                            return body;
                        }
                    }
                    return output; // Fallback to full output
                } else if (output.contains("NO_MESSAGE")) {
                    logger.debug("No message available in queue: {}", queuePath);
                    return null;
                } else {
                    logger.warn("Unknown PowerShell output for message receiving: {}", output);
                    return null;
                }
            } else {
                logger.error("Failed to receive message from queue via PowerShell: {}, exit code: {}", queuePath, exitCode);
                return null;
            }

        } catch (Exception e) {
            logger.error("Error receiving message from queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
            return null;
        }
    }

    @Setter
    @Getter
    public static class ConnectionStatus {
        private boolean connected;
        private String host;
        private int port;
        private int timeout;
        private int retryAttempts;
        private int queueHandleCount;
        private long lastActivity;
        private boolean connectionTestResult;

    }
}
