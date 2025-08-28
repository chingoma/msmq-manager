package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.MsmqQueue;
import com.enterprise.msmq.model.MsmqQueueConfig;
import com.enterprise.msmq.repository.MsmqQueueConfigRepository;
import com.enterprise.msmq.util.PowerShellMsmqConnectionManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for synchronizing MSMQ queues with the application database.
 * This service ensures that the application database reflects the actual
 * MSMQ queues present in the system.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class MsmqQueueSyncService {

    private static final Logger logger = LoggerFactory.getLogger(MsmqQueueSyncService.class);

    private final PowerShellMsmqConnectionManager powerShellMsmqConnectionManager;

    private final MsmqQueueConfigRepository msmqQueueConfigRepository;

    /**
     * Synchronizes MSMQ queues with the application database at startup.
     * This method:
     * 1. Retrieves all queues from MSMQ
     * 2. Updates the application database to match
     * 3. Does NOT delete any MSMQ queues (only syncs from MSMQ to app)
     */
    public void syncQueuesAtStartup() {
        try {
            logger.info("Starting MSMQ queue synchronization at startup...");
            
            // Ensure connection to MSMQ
            if (!powerShellMsmqConnectionManager.isConnected()) {
                if (!powerShellMsmqConnectionManager.connect()) {
                    logger.error("Failed to connect to MSMQ during startup sync");
                    return;
                }
            }

            // Get all queues from MSMQ
            List<MsmqQueue> msmqQueues = getAllQueuesFromMsmq();
            logger.info("Found {} queues in MSMQ system", msmqQueues.size());

            // Update application database with these queues
            for (MsmqQueue queue : msmqQueues) {
                logger.info("MSMQ Queue found: {} - Path: {} - Status: {}", 
                    queue.getName(), queue.getPath(), queue.getStatus());
                
                // Save or update queue in database
                saveQueueToDatabase(queue);
            }

            logger.info("MSMQ queue synchronization completed successfully");

        } catch (Exception e) {
            logger.error("Error during MSMQ queue synchronization at startup", e);
        }
    }

    /**
     * Retrieves all queues from the MSMQ system using PowerShell.
     * 
     * @return List of MSMQ queues found in the system
     */
    public List<MsmqQueue> getAllQueuesFromMsmq() {
        List<MsmqQueue> queues = new ArrayList<>();
        
        try {
            // Use PowerShell to get all private queues
            String command = "Get-MsmqQueue -QueueType Private | Select-Object QueueName, Path, FormatName, MachineName, CreateTime, LastModifyTime, MessageCount, BytesInQueue | ConvertTo-Json";
            
            Process process = Runtime.getRuntime().exec("powershell.exe -Command \"" + command + "\"");
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // Read the output and parse JSON
                String output = readProcessOutput(process);
                if (output != null && !output.trim().isEmpty()) {
                    queues = parseMsmqQueueOutput(output);
                }
            } else {
                logger.warn("PowerShell command failed with exit code: {}", exitCode);
            }

        } catch (Exception e) {
            logger.error("Error retrieving queues from MSMQ", e);
        }

        return queues;
    }

    /**
     * Reads the output from a PowerShell process.
     * 
     * @param process the PowerShell process
     * @return the process output as a string
     */
    private String readProcessOutput(Process process) {
        try {
            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            return output.toString();
        } catch (Exception e) {
            logger.error("Error reading process output", e);
            return null;
        }
    }

    /**
     * Parses the PowerShell JSON output to extract queue information.
     * 
     * @param jsonOutput the JSON output from PowerShell
     * @return list of parsed MsmqQueue objects
     */
    private List<MsmqQueue> parseMsmqQueueOutput(String jsonOutput) {
        List<MsmqQueue> queues = new ArrayList<>();
        
        try {
            // Simple parsing for now - in production you'd use a proper JSON parser
            // This is a basic regex-based approach for demonstration
            
            // Extract queue names from the output
            Pattern pattern = Pattern.compile("\"QueueName\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(jsonOutput);
            
            while (matcher.find()) {
                String queueName = matcher.group(1);
                // Clean up the queue name - remove escape characters
                queueName = queueName.replace("\\\\", "\\");
                
                // Create queue object
                MsmqQueue queue = new MsmqQueue();
                // Extract just the queue name without the private$\ prefix
                String cleanQueueName = queueName;
                if (queueName.startsWith("private$\\")) {
                    cleanQueueName = queueName.substring("private$\\".length());
                } else if (queueName.startsWith("\\")) {
                    cleanQueueName = queueName.substring(1); // Remove leading backslash
                }
                queue.setName(cleanQueueName);
                queue.setPath("private$\\" + cleanQueueName);
                queue.setStatus("ACTIVE");
                queue.setCreatedTime(LocalDateTime.now());
                queue.setModifiedTime(LocalDateTime.now());
                queue.setLastAccessTime(LocalDateTime.now());
                queue.setMessageCount(0L);
                queue.setSize(0L);
                queue.setDescription("Queue synchronized from MSMQ at startup");
                
                queues.add(queue);
            }
            
        } catch (Exception e) {
            logger.error("Error parsing MSMQ queue output", e);
        }
        
        return queues;
    }

    /**
     * Saves or updates a queue in the database.
     * 
     * @param queue the queue to save
     */
    private void saveQueueToDatabase(MsmqQueue queue) {
        try {
            // Check if queue already exists in database
            Optional<MsmqQueueConfig> existingQueue = msmqQueueConfigRepository.findByQueueName(queue.getName());
            
            if (existingQueue.isPresent()) {
                // Update existing queue
                MsmqQueueConfig existingConfig = existingQueue.get();
                existingConfig.setQueuePath(queue.getPath());
                existingConfig.setDescription(queue.getDescription());
                existingConfig.setUpdatedAt(LocalDateTime.now());
                existingConfig.setIsActive(true);
                
                msmqQueueConfigRepository.save(existingConfig);
                logger.debug("Updated existing queue in database: {}", queue.getName());
            } else {
                // Create new queue configuration
                MsmqQueueConfig newConfig = new MsmqQueueConfig();
                newConfig.setQueueName(queue.getName());
                newConfig.setQueuePath(queue.getPath());
                newConfig.setDescription(queue.getDescription());
                newConfig.setMaxMessageSize(1024L); // Default max message size
                newConfig.setIsPrivate(true); // Default to private queue
                newConfig.setIsActive(true);
                newConfig.setCreatedBy("MSMQ_SYNC");
                
                msmqQueueConfigRepository.save(newConfig);
                logger.debug("Created new queue in database: {}", queue.getName());
            }
            
        } catch (Exception e) {
            logger.error("Failed to save queue to database: {}", queue.getName(), e);
        }
    }
}
