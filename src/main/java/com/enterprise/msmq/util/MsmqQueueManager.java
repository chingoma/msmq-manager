package com.enterprise.msmq.util;

import com.enterprise.msmq.dto.MsmqMessage;
import com.enterprise.msmq.dto.MsmqQueue;

import com.enterprise.msmq.exception.MsmqException;
import com.enterprise.msmq.platform.windows.MsmqConstants;
import com.enterprise.msmq.util.PowerShellMsmqConnectionManager;
import com.enterprise.msmq.service.MsmqQueueSyncService;
import com.enterprise.msmq.repository.MsmqMessageRepository;
import com.enterprise.msmq.model.MsmqQueueConfig;
import com.enterprise.msmq.repository.MsmqQueueConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import com.enterprise.msmq.enums.ResponseCode;

/**
 * MSMQ Queue Manager utility class.
 * <p>
 * This class manages MSMQ queue operations including creation,
 * deletion, and message operations.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class MsmqQueueManager {

    private static final Logger logger = LoggerFactory.getLogger(MsmqQueueManager.class);

    private final PowerShellMsmqConnectionManager powerShellMsmqConnectionManager;
    private final MsmqQueueSyncService msmqQueueSyncService;
    private final MsmqMessageRepository msmqMessageRepository;
    private final MsmqQueueConfigRepository msmqQueueConfigRepository;

    public MsmqQueueManager(
            PowerShellMsmqConnectionManager powerShellMsmqConnectionManager,
            MsmqQueueSyncService msmqQueueSyncService,
            MsmqMessageRepository msmqMessageRepository,
            MsmqQueueConfigRepository msmqQueueConfigRepository) {
        this.powerShellMsmqConnectionManager = powerShellMsmqConnectionManager;
        this.msmqQueueSyncService = msmqQueueSyncService;
        this.msmqMessageRepository = msmqMessageRepository;
        this.msmqQueueConfigRepository = msmqQueueConfigRepository;
    }

    /**
     * Creates a new MSMQ queue.
     * 
     * @param queue the queue information
     * @return the created queue
     * @throws MsmqException if queue creation fails
     */
    public MsmqQueue createQueue(MsmqQueue queue) throws MsmqException {
        try {
            logger.debug("Creating queue: {}", queue.getName());
            
            // Validate queue name
            if (queue.getName() == null || queue.getName().trim().isEmpty()) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue name cannot be null or empty");
            }
            
            if (queue.getName().length() > 124) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue name cannot exceed 124 characters");
            }

            // MSMQ-specific validation
            String queueName = queue.getName().trim();
            
            // Check for reserved names
            if (queueName.equalsIgnoreCase("SYSTEM$") || queueName.equalsIgnoreCase("SYSTEM$DEADLETTER")) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue name is reserved and cannot be used");
            }
            
            // Validate private queue format (most common)
            if (queueName.startsWith("PRIVATE$\\")) {
                String privateName = queueName.substring("PRIVATE$\\".length());
                if (privateName.isEmpty() || privateName.contains("\\") || privateName.contains("/")) {
                    throw new MsmqException(ResponseCode.fromCode("611"), "Invalid private queue format. Use 'PRIVATE$\\queuename'");
                }
            }
            // Validate machine queue format
            else if (queueName.startsWith("MACHINE$\\")) {
                String machineName = queueName.substring("MACHINE$\\".length());
                if (machineName.isEmpty() || machineName.contains("\\") || machineName.contains("/")) {
                    throw new MsmqException(ResponseCode.fromCode("611"), "Invalid machine queue format. Use 'MACHINE$\\queuename'");
                }
            }
            // Validate direct queue format
            else if (queueName.startsWith("DIRECT=")) {
                if (!queueName.matches("DIRECT=TCP:[^:]+:\\d+\\\\[^\\\\]+")) {
                    throw new MsmqException(ResponseCode.fromCode("611"), "Invalid direct queue format. Use 'DIRECT=TCP:host:port\\queuename'");
                }
            }
            // Validate format name
            else if (queueName.startsWith("FORMATNAME:")) {
                if (!queueName.matches("FORMATNAME:\\{[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}\\}")) {
                    throw new MsmqException(ResponseCode.fromCode("611"), "Invalid format name. Use 'FORMATNAME:{GUID}'");
                }
            }
            // Validate OS queue format
            else if (queueName.startsWith("OS:")) {
                if (!queueName.matches("OS:[^:]+:[^:]+")) {
                    throw new MsmqException(ResponseCode.fromCode("611"), "Invalid OS queue format. Use 'OS:computer:queue'");
                }
            }
            // Validate HTTP/HTTPS/SOAP queue format
            else if (queueName.startsWith("HTTP://") || queueName.startsWith("HTTPS://") || queueName.startsWith("SOAP://")) {
                if (!queueName.matches("(HTTP|HTTPS|SOAP)://[^:]+:\\d+/[^/]+")) {
                    throw new MsmqException(ResponseCode.fromCode("611"), "Invalid web service queue format. Use 'PROTOCOL://host:port/path'");
                }
            }
            // Default case: treat as private queue name (will be converted to PRIVATE$\name)
            else {
                // Check for invalid characters in simple queue names
                if (queueName.contains("\\") || queueName.contains("/") || queueName.contains(":") || 
                    queueName.contains("<") || queueName.contains(">") || queueName.contains("\"") || 
                    queueName.contains("|") || queueName.contains("?") || queueName.contains("*") ||
                    queueName.startsWith(".") || queueName.endsWith(".") || queueName.contains("..")) {
                    throw new MsmqException(ResponseCode.fromCode("611"), 
                        "Invalid queue name characters. Use simple names like 'myqueue' or proper MSMQ formats like 'PRIVATE$\\myqueue'");
                }
            }

            // Check if queue already exists in database
            if (msmqQueueConfigRepository.findByQueueName(queue.getName()).isPresent()) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue already exists: " + queue.getName());
            }
            
            // Log the queue creation attempt with MSMQ path
            String queuePath = buildQueuePath(queue.getName());
            logger.info("Attempting to create MSMQ queue: '{}' -> '{}'", queue.getName(), queuePath);
            
            // Set default values
            queue.setCreatedTime(LocalDateTime.now());
            queue.setModifiedTime(LocalDateTime.now());
            queue.setLastAccessTime(LocalDateTime.now());
            queue.setStatus("ACTIVE");
            queue.setMessageCount(0L);
            queue.setSize(0L);
            
            // Create queue in PowerShell MSMQ
            boolean msmqCreated = powerShellMsmqConnectionManager.createQueue(queuePath);
            
            if (!msmqCreated) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Failed to create MSMQ queue: " + queuePath);
            }
            
            // Store queue in database for tracking
            MsmqQueueConfig queueConfig = new MsmqQueueConfig();
            queueConfig.setQueueName(queue.getName());
            queueConfig.setQueuePath(queuePath);
            queueConfig.setDescription(queue.getDescription());
            queueConfig.setIsActive(true);
            queueConfig.setIsPrivate(true); // Default to private queue
            queueConfig.setIsTransactional(false); // Default to non-transactional
            queueConfig.setIsAuthenticated(false); // Default to non-authenticated
            queueConfig.setIsEncrypted(false); // Default to non-encrypted
            queueConfig.setCreatedAt(LocalDateTime.now());
            queueConfig.setCreatedBy("System");
            queueConfig.setRetryCount(3); // Default retry count
            queueConfig.setRetryIntervalMs(5000L); // Default 5 seconds
            queueConfig.setTimeoutMs(30000L); // Default 30 seconds
            queueConfig.setMaxMessageSize(4194304L); // Default 4MB
            
            // Save to database
            msmqQueueConfigRepository.save(queueConfig);
            logger.debug("Successfully saved queue configuration to database: {}", queue.getName());
            
            logger.debug("Successfully created queue: {} with path: {}", queue.getName(), queuePath);
            return queue;
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to create queue: {}", queue.getName(), e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to create queue: " + queue.getName(), e);
        }
    }

    /**
     * Deletes an MSMQ queue.
     * 
     * @param queueName the name of the queue to delete
     * @param deleteFromMsmq if true, deletes from MSMQ; if false, only removes from app database
     * @throws MsmqException if queue deletion fails
     */
    public void deleteQueue(String queueName, boolean deleteFromMsmq) throws MsmqException {
        try {
            logger.debug("Deleting queue: {} (deleteFromMsmq: {})", queueName, deleteFromMsmq);
            
            // Check if queue exists in database
            if (!msmqQueueConfigRepository.findByQueueName(queueName).isPresent()) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found in database: " + queueName);
            }
            
            // If requested, delete from MSMQ
            if (deleteFromMsmq) {
                String queuePath = buildQueuePath(queueName);
                boolean msmqDeleted = powerShellMsmqConnectionManager.deleteQueue(queuePath);
                
                if (!msmqDeleted) {
                    throw new MsmqException(ResponseCode.fromCode("611"), "Failed to delete MSMQ queue: " + queuePath);
                }
                logger.debug("Successfully deleted queue from MSMQ: {} with path: {}", queueName, queuePath);
            } else {
                logger.debug("Skipping MSMQ deletion for queue: {} (deleteFromMsmq=false)", queueName);
            }
            
            // Remove all messages for this queue from database
            msmqMessageRepository.deleteByQueueName(queueName);
            
            logger.debug("Successfully removed queue from application database: {}", queueName);
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to delete queue: {}", queueName, e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to delete queue: " + queueName, e);
        }
    }

    /**
     * Deletes an MSMQ queue (defaults to deleting from MSMQ).
     * 
     * @param queueName the name of the queue to delete
     * @throws MsmqException if queue deletion fails
     */
    public void deleteQueue(String queueName) throws MsmqException {
        deleteQueue(queueName, true);
    }

    /**
     * Retrieves information about a specific queue.
     * 
     * @param queueName the name of the queue
     * @return the queue information
     * @throws MsmqException if queue retrieval fails
     */
    public MsmqQueue getQueue(String queueName) throws MsmqException {
        try {
            logger.debug("Retrieving queue: {}", queueName);
            
            // Check if queue exists in database
            Optional<MsmqQueueConfig> queueConfig = msmqQueueConfigRepository.findByQueueName(queueName);
            if (!queueConfig.isPresent()) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found: " + queueName);
            }
            
            // Convert MsmqQueueConfig to MsmqQueue DTO
            MsmqQueueConfig config = queueConfig.get();
            MsmqQueue queue = new MsmqQueue();
            queue.setName(config.getQueueName());
            queue.setPath(config.getQueuePath());
            queue.setDescription(config.getDescription());
            queue.setStatus("ACTIVE");
            queue.setMessageCount(msmqMessageRepository.countByQueueName(queueName));
            queue.setSize(0L); // TODO: Calculate actual size from messages
            queue.setLastAccessTime(LocalDateTime.now());
            
            return queue;
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to retrieve queue: {}", queueName, e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to retrieve queue: " + queueName, e);
        }
    }

    /**
     * Lists all available MSMQ queues.
     * 
     * @return list of all queues
     * @throws MsmqException if queue listing fails
     */
    public List<MsmqQueue> listQueues() throws MsmqException {
        try {
            logger.debug("Listing all queues from database");
            
            // Get all queues from database
            List<MsmqQueueConfig> queueConfigs = msmqQueueConfigRepository.findAll();
            List<MsmqQueue> queueList = new ArrayList<>();
            
            for (MsmqQueueConfig config : queueConfigs) {
                MsmqQueue queue = new MsmqQueue();
                queue.setName(config.getQueueName());
                queue.setPath(config.getQueuePath());
                queue.setDescription(config.getDescription());
                queue.setStatus("ACTIVE");
                queue.setMessageCount(msmqMessageRepository.countByQueueName(config.getQueueName()));
                queue.setSize(0L); // TODO: Calculate actual size from messages
                queue.setLastAccessTime(LocalDateTime.now());
                queueList.add(queue);
            }
            
            return queueList;
            
        } catch (Exception e) {
            logger.error("Failed to list queues", e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to list queues", e);
        }
    }

    /**
     * Synchronizes queues from MSMQ to the application database.
     * This method:
     * 1. Gets all queues from MSMQ
     * 2. Adds new queues to the app database
     * 3. Updates existing queues
     * 4. Does NOT delete any MSMQ queues
     * 
     * @throws MsmqException if synchronization fails
     */
    public void syncQueuesFromMsmq() throws MsmqException {
        try {
            logger.info("Starting queue synchronization from MSMQ...");
            
            // Call the service to sync queues to database
            msmqQueueSyncService.syncQueuesAtStartup();
            
            // Get count of queues now in database after sync
            long queueCount = msmqQueueConfigRepository.count();
            logger.info("Successfully synchronized {} queues from MSMQ to database", queueCount);
            
            logger.debug("Queue synchronization completed - all queues now stored in database");
            
        } catch (Exception e) {
            logger.error("Failed to synchronize queues from MSMQ", e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to synchronize queues from MSMQ", e);
        }
    }

    /**
     * Updates queue properties.
     * 
     * @param queueName the name of the queue to update
     * @param queue the updated queue information
     * @return the updated queue
     * @throws MsmqException if queue update fails
     */
    public MsmqQueue updateQueue(String queueName, MsmqQueue queue) throws MsmqException {
        try {
            logger.debug("Updating queue: {}", queueName);
            
            // Check if queue exists in database
            Optional<MsmqQueueConfig> existingConfig = msmqQueueConfigRepository.findByQueueName(queueName);
            if (!existingConfig.isPresent()) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found: " + queueName);
            }
            
            // Get existing queue config
            MsmqQueueConfig config = existingConfig.get();
            
            // Update properties
            if (queue.getDescription() != null) {
                config.setDescription(queue.getDescription());
            }
            
            // Update timestamps
            config.setUpdatedAt(LocalDateTime.now());
            
            // Save to database
            MsmqQueueConfig updatedConfig = msmqQueueConfigRepository.save(config);
            
            // Convert back to DTO
            MsmqQueue updatedQueue = new MsmqQueue();
            updatedQueue.setName(updatedConfig.getQueueName());
            updatedQueue.setPath(updatedConfig.getQueuePath());
            updatedQueue.setDescription(updatedConfig.getDescription());
            updatedQueue.setStatus("ACTIVE");
            updatedQueue.setMessageCount(msmqMessageRepository.countByQueueName(queueName));
            updatedQueue.setSize(0L);
            updatedQueue.setModifiedTime(updatedConfig.getUpdatedAt());
            
            logger.debug("Successfully updated queue: {}", queueName);
            return updatedQueue;
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to update queue: {}", queueName, e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to update queue: " + queueName, e);
        }
    }

    /**
     * Checks if a queue exists.
     * 
     * @param queueName the name of the queue to check
     * @return true if the queue exists
     * @throws MsmqException if check operation fails
     */
    public boolean queueExists(String queueName) throws MsmqException {
        try {
            return msmqQueueConfigRepository.findByQueueName(queueName).isPresent();
        } catch (Exception e) {
            logger.error("Failed to check queue existence: {}", queueName, e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to check queue existence: " + queueName, e);
        }
    }

    /**
     * Gets queue statistics and health information.
     * 
     * @param queueName the name of the queue
     * @return queue statistics
     * @throws MsmqException if statistics retrieval fails
     */
    public MsmqQueue getQueueStatistics(String queueName) throws MsmqException {
        try {
            logger.debug("Getting statistics for queue: {}", queueName);
            
            MsmqQueue queue = getQueue(queueName);
            
            // Get message count from database
            long messageCount = msmqMessageRepository.countByQueueName(queueName);
            queue.setMessageCount(messageCount);
            
            // Calculate queue size from database
            long totalSize = 0;
            List<com.enterprise.msmq.entity.MsmqMessage> dbMessages = msmqMessageRepository.findByQueueNameOrderByCreatedAtDesc(queueName);
            for (com.enterprise.msmq.entity.MsmqMessage dbMessage : dbMessages) {
                if (dbMessage.getMessageSize() != null) {
                    totalSize += dbMessage.getMessageSize();
                }
            }
            
            queue.setSize(totalSize);
            
            return queue;
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to get queue statistics: {}", queueName, e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to get queue statistics: " + queueName, e);
        }
    }

    /**
     * Sends a message to a specific queue.
     * 
     * @param queueName the destination queue name
     * @param message the message to send
     * @return the sent message
     * @throws MsmqException if message sending fails
     */
    public MsmqMessage sendMessage(String queueName, MsmqMessage message) throws MsmqException {
        try {
            logger.debug("Sending message to queue: {}", queueName);
            
            // Check if queue exists in database
            if (!msmqQueueConfigRepository.findByQueueName(queueName).isPresent()) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found: " + queueName);
            }
            
            // Generate message ID if not provided
            if (message.getMessageId() == null) {
                message.setMessageId(UUID.randomUUID().toString());
            }
            
            // Set message properties
            message.setSentTime(LocalDateTime.now());
            message.setDestinationQueue(queueName);
            message.setStatus("QUEUED");
            
            // Calculate message size
            if (message.getBody() != null) {
                message.setSize((long) message.getBody().getBytes().length);
            }
            
            // Send message to PowerShell MSMQ queue
            String queuePath = buildQueuePath(queueName);
            
            try {
                // Use PowerShell MSMQ to send message
                boolean messageSent = powerShellMsmqConnectionManager.sendMessage(queuePath, message.getBody());
                
                if (!messageSent) {
                    throw new MsmqException(ResponseCode.fromCode("611"), "Failed to send message to PowerShell MSMQ queue: " + queuePath);
                }
                
                // Store message in database for tracking
                com.enterprise.msmq.entity.MsmqMessage dbMessage = new com.enterprise.msmq.entity.MsmqMessage();
                dbMessage.setMessageId(message.getMessageId());
                dbMessage.setQueueName(queueName);
                dbMessage.setCorrelationId(message.getCorrelationId());
                dbMessage.setLabel(message.getLabel());
                dbMessage.setBody(message.getBody());
                dbMessage.setPriority(message.getPriority());
                dbMessage.setMessageSize(message.getSize());
                dbMessage.setProcessingStatus("SENT");
                dbMessage.setIsProcessed(true);
                
                msmqMessageRepository.save(dbMessage);
                
                logger.debug("Message stored in database with ID: {}", message.getMessageId());
                
                logger.debug("Successfully sent message to PowerShell MSMQ queue: {}", queueName);
                return message;
                
            } catch (Exception e) {
                logger.error("Failed to send message to PowerShell MSMQ queue: {}", queueName, e);
                throw new MsmqException(ResponseCode.fromCode("611"), "Failed to send message to PowerShell MSMQ queue: " + queueName, e);
            }
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to send message to queue: {}", queueName, e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to send message to queue: " + queueName, e);
        }
    }

    /**
     * Receives a message from a specific queue.
     * 
     * @param queueName the source queue name
     * @param timeout the timeout in milliseconds
     * @return the received message, or empty if no message available
     * @throws MsmqException if message receiving fails
     */
    public Optional<MsmqMessage> receiveMessage(String queueName, long timeout) throws MsmqException {
        try {
            logger.debug("Receiving message from queue: {} with timeout: {}ms", queueName, timeout);
            
            // Check if queue exists in database
            if (!msmqQueueConfigRepository.findByQueueName(queueName).isPresent()) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found: " + queueName);
            }
            
            // Receive message from PowerShell MSMQ queue
            String queuePath = buildQueuePath(queueName);
            
            try {
                // Use PowerShell MSMQ to receive message
                String receivedMessage = powerShellMsmqConnectionManager.receiveMessage(queuePath);
                
                if (receivedMessage == null || receivedMessage.isEmpty()) {
                    return Optional.empty();
                }
                
                // Create message object from received data
                MsmqMessage message = new MsmqMessage();
                message.setMessageId(UUID.randomUUID().toString());
                message.setBody(receivedMessage);
                message.setReceivedTime(LocalDateTime.now());
                message.setStatus("RECEIVED");
                message.setSize((long) receivedMessage.getBytes().length);
                
                // Store received message in database for tracking
                com.enterprise.msmq.entity.MsmqMessage dbMessage = new com.enterprise.msmq.entity.MsmqMessage();
                dbMessage.setMessageId(message.getMessageId());
                dbMessage.setQueueName(queueName);
                dbMessage.setBody(receivedMessage);
                dbMessage.setMessageSize((long) receivedMessage.getBytes().length);
                dbMessage.setProcessingStatus("RECEIVED");
                dbMessage.setIsProcessed(true);
                
                msmqMessageRepository.save(dbMessage);
                
                logger.debug("Received message stored in database with ID: {}", message.getMessageId());
                
                logger.debug("Successfully received message from PowerShell MSMQ queue: {}", queueName);
                return Optional.of(message);
                
            } catch (Exception e) {
                logger.error("Failed to receive message from PowerShell MSMQ queue: {}", queueName, e);
                throw new MsmqException(ResponseCode.fromCode("611"), "Failed to receive message from PowerShell MSMQ queue: " + queueName, e);
            }
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to receive message from queue: {}", queueName, e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to receive message from queue: " + queueName, e);
        }
    }

    /**
     * Peeks at a message in the queue without removing it.
     * 
     * @param queueName the queue name
     * @param timeout the timeout in milliseconds
     * @return the peeked message, or empty if no message available
     * @throws MsmqException if message peeking fails
     */
    public Optional<MsmqMessage> peekMessage(String queueName, long timeout) throws MsmqException {
        try {
            logger.debug("Peeking message from queue: {} with timeout: {}ms", queueName, timeout);
            
            // Check if queue exists in database
            if (!msmqQueueConfigRepository.findByQueueName(queueName).isPresent()) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found: " + queueName);
            }
            
            // PowerShell MSMQ doesn't have a direct peek operation
            // For now, we'll return empty since we can't peek without removing
            logger.debug("PowerShell MSMQ peeking not yet implemented - returning empty result");
            return Optional.empty();
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to peek message from queue: {}", queueName, e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to peek message from queue: " + queueName, e);
        }
    }

    /**
     * Purges all messages from a queue.
     * 
     * @param queueName the queue name
     * @throws MsmqException if queue purging fails
     */
    public void purgeQueue(String queueName) throws MsmqException {
        try {
            logger.debug("Purging queue: {}", queueName);
            
            // Check if queue exists in database
            if (!msmqQueueConfigRepository.findByQueueName(queueName).isPresent()) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found: " + queueName);
            }
            
            // Clear all messages for this queue from database
            msmqMessageRepository.deleteByQueueName(queueName);
            
            logger.debug("Successfully purged all messages from queue: {} in database", queueName);
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to purge queue: {}", queueName, e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to purge queue: " + queueName, e);
        }
    }

    /**
     * Gets the number of messages in a queue.
     * 
     * @param queueName the queue name
     * @return the message count
     * @throws MsmqException if count retrieval fails
     */
    public long getMessageCount(String queueName) throws MsmqException {
        try {
            // Check if queue exists
            if (!msmqQueueConfigRepository.findByQueueName(queueName).isPresent()) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found: " + queueName);
            }
            
            // Get message count
            return msmqMessageRepository.countByQueueName(queueName);
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to get message count for queue: {}", queueName, e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to get message count for queue: " + queueName, e);
        }
    }
    
    /**
     * Builds the MSMQ queue path from the queue name.
     * Converts logical queue names to PowerShell MSMQ path format.
     * 
     * @param queueName the logical queue name
     * @return the MSMQ queue path
     */
    private String buildQueuePath(String queueName) {
        // Handle different queue name formats for PowerShell MSMQ
        if (queueName.startsWith("private$\\")) {
            // Remove the private$ prefix since PowerShell will add it automatically
            return queueName.substring("private$\\".length());
        } else if (queueName.startsWith("machine$\\")) {
            // Remove the machine$ prefix since PowerShell will add it automatically
            return queueName.substring("machine$\\".length());
        } else if (queueName.startsWith("DIRECT=")) {
            return queueName; // Direct queue format
        } else if (queueName.startsWith("FORMATNAME:")) {
            return queueName; // Format name
        } else if (queueName.startsWith("OS:")) {
            return queueName; // OS queue format
        } else if (queueName.startsWith("HTTP://") || queueName.startsWith("HTTPS://") || queueName.startsWith("SOAP://")) {
            return queueName; // Web service queue format
        } else {
            // Default to just the queue name for PowerShell MSMQ
            return queueName;
        }
    }
}
