package com.enterprise.msmq.util;

import com.enterprise.msmq.dto.MsmqMessage;
import com.enterprise.msmq.dto.MsmqQueue;
import com.enterprise.msmq.exception.MsmqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.enterprise.msmq.enums.ResponseCode;

/**
 * MSMQ Queue Manager utility class.
 * 
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

    // In-memory storage for queues and messages (replace with actual MSMQ operations)
    private final Map<String, MsmqQueue> queues = new ConcurrentHashMap<>();
    private final Map<String, Queue<MsmqMessage>> messageQueues = new ConcurrentHashMap<>();
    private final Map<String, MsmqMessage> messages = new ConcurrentHashMap<>();

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

            if (queue.getName().contains("\\") || queue.getName().contains("/") || queue.getName().contains(":")) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue name contains invalid characters");
            }

            if (queue.getName().startsWith(".") || queue.getName().endsWith(".")) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue name cannot start or end with a dot");
            }

            if (queue.getName().contains("..")) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue name cannot contain consecutive dots");
            }

            if (queue.getName().matches(".*[<>\"|?*].*")) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue name contains invalid characters");
            }

            if (queue.getName().equalsIgnoreCase("SYSTEM$") || queue.getName().equalsIgnoreCase("SYSTEM$DEADLETTER")) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue name is reserved and cannot be used");
            }

            if (queue.getName().startsWith("PRIVATE$") && !queue.getName().startsWith("PRIVATE$\\")) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Private queue name must start with 'PRIVATE$\\'");
            }

            if (queue.getName().startsWith("MACHINE$") && !queue.getName().startsWith("MACHINE$\\")) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Machine queue name must start with 'MACHINE$\\'");
            }

            if (queue.getName().startsWith("DIRECT=") && !queue.getName().matches("DIRECT=TCP:[^:]+:\\d+")) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Direct queue format must be 'DIRECT=TCP:host:port'");
            }

            if (queue.getName().startsWith("FORMATNAME:") && !queue.getName().matches("FORMATNAME:\\{[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}\\}")) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Format name must be 'FORMATNAME:{GUID}'");
            }

            if (queue.getName().startsWith("OS:") && !queue.getName().matches("OS:[^:]+:[^:]+")) {
                throw new MsmqException(ResponseCode.fromCode("611"), "OS queue format must be 'OS:computer:queue'");
            }

            if (queue.getName().startsWith("HTTP://") && !queue.getName().matches("HTTP://[^:]+:\\d+/[^/]+")) {
                throw new MsmqException(ResponseCode.fromCode("611"), "HTTP queue format must be 'HTTP://host:port/path'");
            }

            if (queue.getName().startsWith("HTTPS://") && !queue.getName().matches("HTTPS://[^:]+:\\d+/[^/]+")) {
                throw new MsmqException(ResponseCode.fromCode("611"), "HTTPS queue format must be 'HTTPS://host:port/path'");
            }

            if (queue.getName().startsWith("SOAP://") && !queue.getName().matches("SOAP://[^:]+:\\d+/[^/]+")) {
                throw new MsmqException(ResponseCode.fromCode("611"), "SOAP queue format must be 'SOAP://host:port/path'");
            }

            // Check if queue already exists
            if (queues.containsKey(queue.getName())) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue already exists: " + queue.getName());
            }
            
            // Set default values
            queue.setCreatedTime(LocalDateTime.now());
            queue.setModifiedTime(LocalDateTime.now());
            queue.setLastAccessTime(LocalDateTime.now());
            queue.setStatus("ACTIVE");
            queue.setMessageCount(0L);
            queue.setSize(0L);
            
            // Store queue
            queues.put(queue.getName(), queue);
            messageQueues.put(queue.getName(), new LinkedList<>());
            
            logger.debug("Successfully created queue: {}", queue.getName());
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
     * @throws MsmqException if queue deletion fails
     */
    public void deleteQueue(String queueName) throws MsmqException {
        try {
            logger.debug("Deleting queue: {}", queueName);
            
            // Check if queue exists
            if (!queues.containsKey(queueName)) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found: " + queueName);
            }
            
            // Remove queue and its messages
            queues.remove(queueName);
            messageQueues.remove(queueName);
            
            // Remove all messages for this queue
            messages.entrySet().removeIf(entry -> queueName.equals(entry.getValue().getDestinationQueue()));
            
            logger.debug("Successfully deleted queue: {}", queueName);
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to delete queue: {}", queueName, e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to delete queue: " + queueName, e);
        }
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
            
            MsmqQueue queue = queues.get(queueName);
            if (queue == null) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found: " + queueName);
            }
            
            // Update last access time
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
            logger.debug("Listing all queues");
            
            List<MsmqQueue> queueList = new ArrayList<>(queues.values());
            
            // Update last access time for all queues
            queueList.forEach(queue -> queue.setLastAccessTime(LocalDateTime.now()));
            
            return queueList;
            
        } catch (Exception e) {
            logger.error("Failed to list queues", e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to list queues", e);
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
            
            // Check if queue exists
            if (!queues.containsKey(queueName)) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found: " + queueName);
            }
            
            // Get existing queue
            MsmqQueue existingQueue = queues.get(queueName);
            
            // Update properties
            if (queue.getDescription() != null) {
                existingQueue.setDescription(queue.getDescription());
            }
            if (queue.getMaxMessageCount() != null) {
                existingQueue.setMaxMessageCount(queue.getMaxMessageCount());
            }
            if (queue.getMaxSize() != null) {
                existingQueue.setMaxSize(queue.getMaxSize());
            }
            
            // Update timestamps
            existingQueue.setModifiedTime(LocalDateTime.now());
            existingQueue.setLastAccessTime(LocalDateTime.now());
            
            logger.debug("Successfully updated queue: {}", queueName);
            return existingQueue;
            
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
            return queues.containsKey(queueName);
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
            
            // Get message count
            Queue<MsmqMessage> messageQueue = messageQueues.get(queueName);
            if (messageQueue != null) {
                queue.setMessageCount((long) messageQueue.size());
            }
            
            // Calculate queue size
            long totalSize = 0;
            if (messageQueue != null) {
                for (MsmqMessage message : messageQueue) {
                    if (message.getSize() != null) {
                        totalSize += message.getSize();
                    }
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
            
            // Check if queue exists
            if (!queues.containsKey(queueName)) {
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
            
            // Add message to queue
            Queue<MsmqMessage> messageQueue = messageQueues.get(queueName);
            if (messageQueue != null) {
                messageQueue.offer(message);
                messages.put(message.getMessageId(), message);
            }
            
            // Update queue statistics
            MsmqQueue queue = queues.get(queueName);
            if (queue != null) {
                queue.setMessageCount((long) messageQueue.size());
                queue.setLastAccessTime(LocalDateTime.now());
            }
            
            logger.debug("Successfully sent message to queue: {}", queueName);
            return message;
            
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
            
            // Check if queue exists
            if (!queues.containsKey(queueName)) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found: " + queueName);
            }
            
            // Get message queue
            Queue<MsmqMessage> messageQueue = messageQueues.get(queueName);
            if (messageQueue == null || messageQueue.isEmpty()) {
                return Optional.empty();
            }
            
            // Receive message
            MsmqMessage message = messageQueue.poll();
            if (message != null) {
                message.setReceivedTime(LocalDateTime.now());
                message.setStatus("RECEIVED");
                
                // Remove from messages map
                messages.remove(message.getMessageId());
                
                // Update queue statistics
                MsmqQueue queue = queues.get(queueName);
                if (queue != null) {
                    queue.setMessageCount((long) messageQueue.size());
                    queue.setLastAccessTime(LocalDateTime.now());
                }
                
                logger.debug("Successfully received message from queue: {}", queueName);
                return Optional.of(message);
            }
            
            return Optional.empty();
            
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
            
            // Check if queue exists
            if (!queues.containsKey(queueName)) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found: " + queueName);
            }
            
            // Get message queue
            Queue<MsmqMessage> messageQueue = messageQueues.get(queueName);
            if (messageQueue == null || messageQueue.isEmpty()) {
                return Optional.empty();
            }
            
            // Peek at message
            MsmqMessage message = messageQueue.peek();
            if (message != null) {
                // Update queue last access time
                MsmqQueue queue = queues.get(queueName);
                if (queue != null) {
                    queue.setLastAccessTime(LocalDateTime.now());
                }
                
                logger.debug("Successfully peeked message from queue: {}", queueName);
                return Optional.of(message);
            }
            
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
            
            // Check if queue exists
            if (!queues.containsKey(queueName)) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found: " + queueName);
            }
            
            // Clear message queue
            Queue<MsmqMessage> messageQueue = messageQueues.get(queueName);
            if (messageQueue != null) {
                // Remove all messages for this queue from messages map
                messageQueue.forEach(message -> messages.remove(message.getMessageId()));
                messageQueue.clear();
            }
            
            // Update queue statistics
            MsmqQueue queue = queues.get(queueName);
            if (queue != null) {
                queue.setMessageCount(0L);
                queue.setSize(0L);
                queue.setLastAccessTime(LocalDateTime.now());
            }
            
            logger.debug("Successfully purged queue: {}", queueName);
            
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
            if (!queues.containsKey(queueName)) {
                throw new MsmqException(ResponseCode.fromCode("611"), "Queue not found: " + queueName);
            }
            
            // Get message count
            Queue<MsmqMessage> messageQueue = messageQueues.get(queueName);
            return messageQueue != null ? messageQueue.size() : 0;
            
        } catch (MsmqException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to get message count for queue: {}", queueName, e);
            throw new MsmqException(ResponseCode.fromCode("611"), "Failed to get message count for queue: " + queueName, e);
        }
    }
}
