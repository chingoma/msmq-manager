package com.enterprise.msmq.util;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class PowerShellMsmqConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(PowerShellMsmqConnectionManager.class);

    @Value("${msmq.connection.host:localhost}")
    private String msmqHost;

    @Value("${msmq.connection.port:1801}")
    private int msmqPort;

    @Value("${msmq.connection.timeout:30000}")
    private int connectionTimeout;

    @Value("${msmq.connection.retry-attempts:3}")
    private int retryAttempts;

    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    public boolean connect() {
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                logger.info("PowerShell MSMQ connection attempt {} of {} to MSMQ service at {}:{}", 
                    attempt, retryAttempts, msmqHost, msmqPort);
                
                if (testConnection()) {
                    isConnected.set(true);
                    logger.info("Successfully connected to MSMQ service via PowerShell at {}:{}", msmqHost, msmqPort);
                    return true;
                }
            } catch (Exception e) {
                logger.error("PowerShell MSMQ attempt {} failed: {}", attempt, e.getMessage());
                if (attempt == retryAttempts) {
                    logger.error("Failed to establish PowerShell MSMQ connection at {}:{}", msmqHost, msmqPort);
                    return false;
                }
            }
        }
        return false;
    }

    private boolean testConnection() {
        try {
            logger.debug("Testing PowerShell MSMQ connection...");

            // Test 1: Check if PowerShell MSMQ cmdlets are available
            if (!checkPowerShellMsmqAvailability()) {
                logger.error("PowerShell MSMQ cmdlets are not available");
                return false;
            }

            // Test 2: Try to access existing queue
            if (testExistingQueue()) {
                logger.debug("PowerShell MSMQ connection test successful");
                return true;
            }

            // Test 3: Try to create a temporary test queue
            if (testCreateQueue()) {
                logger.debug("PowerShell MSMQ connection test successful - test queue created");
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.error("PowerShell MSMQ connection test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean checkPowerShellMsmqAvailability() {
        try {
            String command = "Get-Command Get-MsmqQueue -ErrorAction SilentlyContinue";
            Process process = Runtime.getRuntime().exec("powershell.exe -Command \"" + command + "\"");
            
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            logger.error("Failed to check PowerShell MSMQ availability: {}", e.getMessage());
            return false;
        }
    }

    private boolean testExistingQueue() {
        try {
            logger.debug("Testing PowerShell MSMQ connection to existing queue: nmb_to_dse");

            String command = "Get-MsmqQueue -QueueType Private | Where-Object { $_.QueueName -like '*nmb_to_dse*' } | Select-Object QueueName, Path, FormatName, MachineName | ConvertTo-Json";
            Process process = Runtime.getRuntime().exec("powershell.exe -Command \"" + command + "\"");
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                // Read the output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    StringBuilder output = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                    
                    if (output.toString().trim().length() > 0) {
                        logger.debug("Successfully connected to existing queue via PowerShell");
                        return true;
                    }
                }
            }

            // Check error output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                StringBuilder error = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                }
                if (error.toString().trim().length() > 0) {
                    logger.debug("PowerShell MSMQ error: {}", error.toString().trim());
                }
            }

            return false;

        } catch (Exception e) {
            logger.debug("Error testing existing queue via PowerShell: {}", e.getMessage());
            return false;
        }
    }

    private boolean testCreateQueue() {
        try {
            String testQueueName = "msmq-test-" + System.currentTimeMillis();
            logger.debug("Testing PowerShell MSMQ connection by creating test queue: {}", testQueueName);

            // Create test queue
            String createCommand = "New-MsmqQueue -Name 'private$\\" + testQueueName + "' -QueueType Private -ErrorAction SilentlyContinue";
            Process createProcess = Runtime.getRuntime().exec("powershell.exe -Command \"" + createCommand + "\"");
            
            int createExitCode = createProcess.waitFor();
            if (createExitCode == 0) {
                logger.debug("Successfully created test queue via PowerShell: {}", testQueueName);
                
                // Clean up - delete the test queue
                String deleteCommand = "$queue = Get-MsmqQueue -QueueType Private | Where-Object { $_.QueueName -like '*" + testQueueName + "*' } | Select-Object -First 1; if ($queue) { Remove-MsmqQueue -InputObject $queue -ErrorAction SilentlyContinue }";
                Process deleteProcess = Runtime.getRuntime().exec("powershell.exe -Command \"" + deleteCommand + "\"");
                deleteProcess.waitFor();
                
                logger.debug("Successfully deleted test queue via PowerShell: {}", testQueueName);
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.debug("Error testing queue creation via PowerShell: {}", e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        return isConnected.get();
    }

    public String getMsmqHost() {
        return msmqHost;
    }

    public int getMsmqPort() {
        return msmqPort;
    }

    public ConnectionStatus getConnectionStatus() {
        ConnectionStatus status = new ConnectionStatus();
        status.setConnected(isConnected.get());
        status.setHost(msmqHost);
        status.setPort(msmqPort);
        status.setTimeout(connectionTimeout);
        status.setRetryAttempts(retryAttempts);
        status.setQueueHandleCount(0); // PowerShell doesn't use handles
        status.setLastActivity(System.currentTimeMillis());

        if (!isConnected.get()) {
            status.setConnectionTestResult(testConnection());
        } else {
            status.setConnectionTestResult(true);
        }

        return status;
    }

    public void disconnect() {
        isConnected.set(false);
        logger.info("Disconnected from PowerShell MSMQ service");
    }

    public boolean createQueue(String queuePath) {
        try {
            if (!isConnected.get()) {
                if (!connect()) {
                    return false;
                }
            }

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
            if (!isConnected.get()) {
                if (!connect()) {
                    return false;
                }
            }

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
            if (!isConnected.get()) {
                if (!connect()) {
                    return false;
                }
            }

            // Use XmlMessageFormatter consistently for both sending and receiving
            // Use the simple approach that worked in PowerShell documentation
            String queuePathWithPrefix = ".\\private$\\" + queuePath;
            String command = "Add-Type -AssemblyName System.Messaging; $queuePath = '" + queuePathWithPrefix + "'; if (-not [System.Messaging.MessageQueue]::Exists($queuePath)) { [System.Messaging.MessageQueue]::Create($queuePath, $true) | Out-Null }; $queue = New-Object System.Messaging.MessageQueue $queuePath; $queue.Formatter = New-Object System.Messaging.XmlMessageFormatter; $queue.Send('" + message.replace("'", "''") + "', 'MSMQ Manager Message'); $queue.Close(); if ($?) { Write-Host 'SUCCESS' } else { Write-Host 'FAILED' }";
            
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
                logger.error("Failed to send message to queue via PowerShell: {}, exit code: {}", queuePath, exitCode);
                return false;
            }

        } catch (Exception e) {
            logger.error("Error sending message to queue {} via PowerShell: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    public String receiveMessage(String queuePath) {
        try {
            if (!isConnected.get()) {
                if (!connect()) {
                    return null;
                }
            }

            // Use XmlMessageFormatter consistently for both sending and receiving
            String queuePathWithPrefix = ".\\private$\\" + queuePath;
            String command = "Add-Type -AssemblyName System.Messaging; $queuePath = '" + queuePathWithPrefix + "'; $queue = New-Object System.Messaging.MessageQueue $queuePath; $queue.Formatter = New-Object System.Messaging.XmlMessageFormatter; $msg = $queue.Receive([System.Messaging.MessageQueueTransactionType]::Single); if ($msg) { Write-Host 'BODY:' $msg.Body; Write-Host 'LABEL:' $msg.Label; Write-Host 'PRIORITY:' $msg.Priority; Write-Host 'CORRELATIONID:' $msg.CorrelationId; Write-Host 'MESSAGEID:' $msg.Id; Write-Host 'SUCCESS' } else { Write-Host 'NO_MESSAGE' }; $queue.Close()";
            
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
