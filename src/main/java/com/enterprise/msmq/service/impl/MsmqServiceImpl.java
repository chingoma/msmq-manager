package com.enterprise.msmq.service.impl;

import com.enterprise.msmq.dto.*;
import com.enterprise.msmq.enums.ResponseCode;
import com.enterprise.msmq.exception.MsmqException;
import com.enterprise.msmq.service.MsmqService;
import com.enterprise.msmq.util.MsmqConnectionManager;
import com.enterprise.msmq.util.MsmqMessageParser;
import com.enterprise.msmq.util.MsmqQueueManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of MSMQ Service interface.
 * 
 * This class contains all the business logic for MSMQ operations including
 * queue management, message operations, and connection management. The implementation
 * follows enterprise best practices with proper error handling, logging, and
 * performance optimization.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
public class MsmqServiceImpl implements MsmqService {

    private static final Logger logger = LoggerFactory.getLogger(MsmqServiceImpl.class);

    private final MsmqConnectionManager connectionManager;
    private final MsmqQueueManager queueManager;
    private final MsmqMessageParser messageParser;

    // Performance metrics tracking
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    private final AtomicLong totalMessagesReceived = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final Map<String, Long> operationTimings = new ConcurrentHashMap<>();
    private final Map<String, Long> errorCounts = new ConcurrentHashMap<>();

    @Override
    public MsmqQueue createQueue(MsmqQueue queue) throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "CREATE_QUEUE";
        
        try {
            logger.info("Creating MSMQ queue: {}", queue.getName());
            
            // Validate queue parameters
            validateQueueParameters(queue);
            
            // Check if queue already exists
            if (queueExists(queue.getName())) {
                throw new MsmqException(ResponseCode.QUEUE_ALREADY_EXISTS, "Queue already exists: " + queue.getName());
            }
            
            // Ensure connection is active
            ensureConnection();
            
            // Create queue using queue manager
            MsmqQueue createdQueue = queueManager.createQueue(queue);
            
            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully created MSMQ queue: {}", queue.getName());
            
            return createdQueue;
            
        } catch (MsmqException e) {
            handleOperationError(operation, e);
            throw e;
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to create queue: " + queue.getName(), e));
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to create queue: " + queue.getName(), e);
        }
    }

    @Override
    public void deleteQueue(String queueName) throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "DELETE_QUEUE";
        
        try {
            logger.info("Deleting MSMQ queue: {}", queueName);
            
            // Validate queue name
            if (queueName == null || queueName.trim().isEmpty()) {
                throw new MsmqException(ResponseCode.INVALID_QUEUE_NAME, "Queue name cannot be null or empty");
            }
            
            // Check if queue exists
            if (!queueExists(queueName)) {
                throw new MsmqException(ResponseCode.QUEUE_NOT_FOUND, "Queue not found: " + queueName);
            }
            
            // Ensure connection is active
            ensureConnection();
            
            // Delete queue using queue manager
            queueManager.deleteQueue(queueName);
            
            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully deleted MSMQ queue: {}", queueName);
            
        } catch (MsmqException e) {
            handleOperationError(operation, e);
            throw e;
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to delete queue: " + queueName, e));
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to delete queue: " + queueName, e);
        }
    }

    @Override
    public MsmqQueue getQueue(String queueName) throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "GET_QUEUE";
        
        try {
            logger.debug("Retrieving MSMQ queue: {}", queueName);
            
            // Validate queue name
            if (queueName == null || queueName.trim().isEmpty()) {
                throw new MsmqException(ResponseCode.INVALID_QUEUE_NAME, "Queue name cannot be null or empty");
            }
            
            // Ensure connection is active
            ensureConnection();
            
            // Get queue using queue manager
            MsmqQueue queue = queueManager.getQueue(queueName);
            
            if (queue == null) {
                throw new MsmqException(ResponseCode.QUEUE_NOT_FOUND, "Queue not found: " + queueName);
            }
            
            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.debug("Successfully retrieved MSMQ queue: {}", queueName);
            
            return queue;
            
        } catch (MsmqException e) {
            handleOperationError(operation, e);
            throw e;
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to retrieve queue: " + queueName, e));
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to retrieve queue: " + queueName, e);
        }
    }

    @Override
    public List<MsmqQueue> listQueues() throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "LIST_QUEUES";
        
        try {
            logger.debug("Listing all MSMQ queues");
            
            // Ensure connection is active
            ensureConnection();
            
            // List queues using queue manager
            List<MsmqQueue> queues = queueManager.listQueues();
            
            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.debug("Successfully listed {} MSMQ queues", queues.size());
            
            return queues;
            
        } catch (MsmqException e) {
            handleOperationError(operation, e);
            throw e;
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to list queues", e));
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to list queues", e);
        }
    }

    @Override
    public MsmqQueue updateQueue(String queueName, MsmqQueue queue) throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "UPDATE_QUEUE";
        
        try {
            logger.info("Updating MSMQ queue: {}", queueName);
            
            // Validate parameters
            if (queueName == null || queueName.trim().isEmpty()) {
                throw new MsmqException(ResponseCode.INVALID_QUEUE_NAME, "Queue name cannot be null or empty");
            }
            if (queue == null) {
                throw new MsmqException(ResponseCode.VALIDATION_ERROR, "Queue information cannot be null");
            }
            
            // Check if queue exists
            if (!queueExists(queueName)) {
                throw new MsmqException(ResponseCode.QUEUE_NOT_FOUND, "Queue not found: " + queueName);
            }
            
            // Ensure connection is active
            ensureConnection();
            
            // Update queue using queue manager
            MsmqQueue updatedQueue = queueManager.updateQueue(queueName, queue);
            
            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully updated MSMQ queue: {}", queueName);
            
            return updatedQueue;
            
        } catch (MsmqException e) {
            handleOperationError(operation, e);
            throw e;
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to update queue: " + queueName, e));
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to update queue: " + queueName, e);
        }
    }

    @Override
    public boolean queueExists(String queueName) throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "QUEUE_EXISTS";
        
        try {
            logger.debug("Checking if MSMQ queue exists: {}", queueName);
            
            // Validate queue name
            if (queueName == null || queueName.trim().isEmpty()) {
                throw new MsmqException(ResponseCode.INVALID_QUEUE_NAME, "Queue name cannot be null or empty");
            }
            
            // Ensure connection is active
            ensureConnection();
            
            // Check queue existence using queue manager
            boolean exists = queueManager.queueExists(queueName);
            
            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.debug("Queue existence check result for {}: {}", queueName, exists);
            
            return exists;
            
        } catch (MsmqException e) {
            handleOperationError(operation, e);
            throw e;
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to check queue existence: " + queueName, e));
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to check queue existence: " + queueName, e);
        }
    }

    @Override
    public MsmqQueue getQueueStatistics(String queueName) throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "GET_QUEUE_STATISTICS";
        
        try {
            logger.debug("Retrieving statistics for MSMQ queue: {}", queueName);
            
            // Validate queue name
            if (queueName == null || queueName.trim().isEmpty()) {
                throw new MsmqException(ResponseCode.INVALID_QUEUE_NAME, "Queue name cannot be null or empty");
            }
            
            // Check if queue exists
            if (!queueExists(queueName)) {
                throw new MsmqException(ResponseCode.QUEUE_NOT_FOUND, "Queue not found: " + queueName);
            }
            
            // Ensure connection is active
            ensureConnection();
            
            // Get queue statistics using queue manager
            MsmqQueue statistics = queueManager.getQueueStatistics(queueName);
            
            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.debug("Successfully retrieved statistics for MSMQ queue: {}", queueName);
            
            return statistics;
            
        } catch (MsmqException e) {
            handleOperationError(operation, e);
            throw e;
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to retrieve queue statistics: " + queueName, e));
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to retrieve queue statistics: " + queueName, e);
        }
    }

    @Override
    public MsmqMessage sendMessage(String queueName, MsmqMessage message) throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "SEND_MESSAGE";
        
        try {
            logger.info("Sending message to MSMQ queue: {}", queueName);
            
            // Validate parameters
            if (queueName == null || queueName.trim().isEmpty()) {
                throw new MsmqException(ResponseCode.INVALID_QUEUE_NAME, "Queue name cannot be null or empty");
            }
            if (message == null) {
                throw new MsmqException(ResponseCode.VALIDATION_ERROR, "Message cannot be null");
            }
            if (message.getBody() == null || message.getBody().trim().isEmpty()) {
                throw new MsmqException(ResponseCode.INVALID_MESSAGE_FORMAT, "Message body cannot be null or empty");
            }
            
            // Check if queue exists
            if (!queueExists(queueName)) {
                throw new MsmqException(ResponseCode.QUEUE_NOT_FOUND, "Queue not found: " + queueName);
            }
            
            // Ensure connection is active
            ensureConnection();
            
            // Parse and validate message
            MsmqMessage parsedMessage = messageParser.parseOutgoingMessage(message);
            
            // Send message using queue manager
            MsmqMessage sentMessage = queueManager.sendMessage(queueName, parsedMessage);
            
            // Update metrics
            totalMessagesSent.incrementAndGet();
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully sent message to MSMQ queue: {}", queueName);
            
            return sentMessage;
            
        } catch (MsmqException e) {
            handleOperationError(operation, e);
            throw e;
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to send message to queue: " + queueName, e));
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to send message to queue: " + queueName, e);
        }
    }

    @Override
    public Optional<MsmqMessage> receiveMessage(String queueName, long timeout) throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "RECEIVE_MESSAGE";
        
        try {
            logger.debug("Receiving message from MSMQ queue: {} with timeout: {}ms", queueName, timeout);
            
            // Validate parameters
            if (queueName == null || queueName.trim().isEmpty()) {
                throw new MsmqException(ResponseCode.INVALID_QUEUE_NAME, "Queue name cannot be null or empty");
            }
            if (timeout < 0) {
                throw new MsmqException(ResponseCode.VALIDATION_ERROR, "Timeout cannot be negative");
            }
            
            // Check if queue exists
            if (!queueExists(queueName)) {
                throw new MsmqException(ResponseCode.QUEUE_NOT_FOUND, "Queue not found: " + queueName);
            }
            
            // Ensure connection is active
            ensureConnection();
            
            // Receive message using queue manager
            Optional<MsmqMessage> receivedMessage = queueManager.receiveMessage(queueName, timeout);
            
            if (receivedMessage.isPresent()) {
                // Parse incoming message
                MsmqMessage parsedMessage = messageParser.parseIncomingMessage(receivedMessage.get());
                receivedMessage = Optional.of(parsedMessage);
                
                // Update metrics
                totalMessagesReceived.incrementAndGet();
            }
            
            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.debug("Message receive operation completed for queue: {}", queueName);
            
            return receivedMessage;
            
        } catch (MsmqException e) {
            handleOperationError(operation, e);
            throw e;
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to receive message from queue: " + queueName, e));
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to receive message from queue: " + queueName, e);
        }
    }

    @Override
    public Optional<MsmqMessage> receiveMessage(String queueName) throws MsmqException {
        // Use default timeout from configuration
        return receiveMessage(queueName, 60000); // 60 seconds default
    }

    @Override
    public Optional<MsmqMessage> peekMessage(String queueName, long timeout) throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "PEEK_MESSAGE";
        
        try {
            logger.debug("Peeking message from MSMQ queue: {} with timeout: {}ms", queueName, timeout);
            
            // Validate parameters
            if (queueName == null || queueName.trim().isEmpty()) {
                throw new MsmqException(ResponseCode.INVALID_QUEUE_NAME, "Queue name cannot be null or empty");
            }
            if (timeout < 0) {
                throw new MsmqException(ResponseCode.VALIDATION_ERROR, "Timeout cannot be negative");
            }
            
            // Check if queue exists
            if (!queueExists(queueName)) {
                throw new MsmqException(ResponseCode.QUEUE_NOT_FOUND, "Queue not found: " + queueName);
            }
            
            // Ensure connection is active
            ensureConnection();
            
            // Peek message using queue manager
            Optional<MsmqMessage> peekedMessage = queueManager.peekMessage(queueName, timeout);
            
            if (peekedMessage.isPresent()) {
                // Parse incoming message
                MsmqMessage parsedMessage = messageParser.parseIncomingMessage(peekedMessage.get());
                peekedMessage = Optional.of(parsedMessage);
            }
            
            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.debug("Message peek operation completed for queue: {}", queueName);
            
            return peekedMessage;
            
        } catch (MsmqException e) {
            handleOperationError(operation, e);
            throw e;
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to peek message from queue: " + queueName, e));
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to peek message from queue: " + queueName, e);
        }
    }

    @Override
    public Optional<MsmqMessage> peekMessage(String queueName) throws MsmqException {
        // Use default timeout from configuration
        return peekMessage(queueName, 60000); // 60 seconds default
    }

    @Override
    public void purgeQueue(String queueName) throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "PURGE_QUEUE";
        
        try {
            logger.info("Purging MSMQ queue: {}", queueName);
            
            // Validate queue name
            if (queueName == null || queueName.trim().isEmpty()) {
                throw new MsmqException(ResponseCode.INVALID_QUEUE_NAME, "Queue name cannot be null or empty");
            }
            
            // Check if queue exists
            if (!queueExists(queueName)) {
                throw new MsmqException(ResponseCode.QUEUE_NOT_FOUND, "Queue not found: " + queueName);
            }
            
            // Ensure connection is active
            ensureConnection();
            
            // Purge queue using queue manager
            queueManager.purgeQueue(queueName);
            
            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully purged MSMQ queue: {}", queueName);
            
        } catch (MsmqException e) {
            handleOperationError(operation, e);
            throw e;
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to purge queue: " + queueName, e));
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to purge queue: " + queueName, e);
        }
    }

    @Override
    public long getMessageCount(String queueName) throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "GET_MESSAGE_COUNT";
        
        try {
            logger.debug("Getting message count for MSMQ queue: {}", queueName);
            
            // Validate queue name
            if (queueName == null || queueName.trim().isEmpty()) {
                throw new MsmqException(ResponseCode.INVALID_QUEUE_NAME, "Queue name cannot be null or empty");
            }
            
            // Check if queue exists
            if (!queueExists(queueName)) {
                throw new MsmqException(ResponseCode.QUEUE_NOT_FOUND, "Queue not found: " + queueName);
            }
            
            // Ensure connection is active
            ensureConnection();
            
            // Get message count using queue manager
            long count = queueManager.getMessageCount(queueName);
            
            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.debug("Message count for queue {}: {}", queueName, count);
            
            return count;
            
        } catch (MsmqException e) {
            handleOperationError(operation, e);
            throw e;
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to get message count for queue: " + queueName, e));
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to get message count for queue: " + queueName, e);
        }
    }

    @Override
    public void connect() throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "CONNECT";
        
        try {
            logger.info("Establishing connection to MSMQ service");
            
            // Connect using connection manager
            connectionManager.connect();
            
            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully connected to MSMQ service");
            
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.CONNECTION_ERROR, "Failed to connect to MSMQ service", e));
            throw new MsmqException(ResponseCode.CONNECTION_ERROR, "Failed to connect to MSMQ service", e);
        }
    }

    @Override
    public void disconnect() throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "DISCONNECT";
        
        try {
            logger.info("Disconnecting from MSMQ service");
            
            // Disconnect using connection manager
            connectionManager.disconnect();
            
            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully disconnected from MSMQ service");
            
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.CONNECTION_ERROR, "Failed to disconnect from MSMQ service", e));
            throw new MsmqException(ResponseCode.CONNECTION_ERROR, "Failed to disconnect from MSMQ service", e);
        }
    }

    @Override
    public boolean isConnected() {
        return connectionManager.isConnected();
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return connectionManager.getConnectionStatus();
    }

    @Override
    public void reconnect() throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "RECONNECT";
        
        try {
            logger.info("Reconnecting to MSMQ service");
            
            // Reconnect using connection manager
            connectionManager.reconnect();
            
            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully reconnected to MSMQ service");
            
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.CONNECTION_ERROR, "Failed to reconnect to MSMQ service", e));
            throw new MsmqException(ResponseCode.CONNECTION_ERROR, "Failed to reconnect to MSMQ service", e);
        }
    }

    @Override
    public SystemHealth getSystemHealth() {
        // Implementation for system health monitoring
        // This would integrate with actual MSMQ health checks
        return new SystemHealth();
    }

    @Override
    public PerformanceMetrics getPerformanceMetrics() {
        // Implementation for performance metrics
        // This would provide actual performance data
        return new PerformanceMetrics();
    }

    @Override
    public ErrorStatistics getErrorStatistics() {
        // Implementation for error statistics
        // This would provide actual error data
        return new ErrorStatistics();
    }

    @Override
    public boolean validateConfiguration() {
        try {
            // Validate MSMQ configuration
            // Check if connection manager is properly configured
            return connectionManager != null && 
                   connectionManager.getHost() != null && 
                   connectionManager.getPort() > 0 && 
                   connectionManager.getTimeout() > 0;
        } catch (Exception e) {
            logger.error("Configuration validation failed", e);
            return false;
        }
    }

    @Override
    public String getMsmqVersion() {
        try {
            // Get MSMQ version information
            // This would typically query the actual MSMQ service for version information
            // For now, return a placeholder version
            return "MSMQ 4.0 (Simulated)";
        } catch (Exception e) {
            logger.error("Failed to get MSMQ version", e);
            return "Unknown";
        }
    }

    @Override
    public HealthCheckResult performHealthCheck() {
        // Implementation for health check
        // This would perform actual health checks
        return new HealthCheckResult();
    }

    // Private helper methods

    /**
     * Ensures that the MSMQ connection is active.
     * 
     * @throws MsmqException if connection is not available
     */
    private void ensureConnection() throws MsmqException {
        if (!isConnected()) {
            logger.warn("MSMQ connection not available, attempting to reconnect");
            try {
                reconnect();
            } catch (MsmqException e) {
                throw new MsmqException(ResponseCode.MSMQ_NOT_AVAILABLE, "MSMQ connection not available", e);
            }
        }
    }

    /**
     * Validates queue parameters.
     * 
     * @param queue the queue to validate
     * @throws MsmqException if validation fails
     */
    private void validateQueueParameters(MsmqQueue queue) throws MsmqException {
        if (queue == null) {
            throw new MsmqException(ResponseCode.VALIDATION_ERROR, "Queue information cannot be null");
        }
        if (queue.getName() == null || queue.getName().trim().isEmpty()) {
            throw new MsmqException(ResponseCode.INVALID_QUEUE_NAME, "Queue name cannot be null or empty");
        }
        if (queue.getName().length() > 124) {
            throw new MsmqException(ResponseCode.INVALID_QUEUE_NAME, "Queue name cannot exceed 124 characters");
        }
        if (queue.getPath() == null || queue.getPath().trim().isEmpty()) {
            throw new MsmqException(ResponseCode.INVALID_QUEUE_PATH, "Queue path cannot be null or empty");
        }
    }

    /**
     * Updates operation timing metrics.
     * 
     * @param operation the operation name
     * @param duration the operation duration in milliseconds
     */
    private void updateOperationTiming(String operation, long duration) {
        operationTimings.put(operation, duration);
    }

    /**
     * Handles operation errors and updates metrics.
     * 
     * @param operation the operation name
     * @param exception the exception that occurred
     */
    private void handleOperationError(String operation, MsmqException exception) {
        totalErrors.incrementAndGet();
        errorCounts.merge(operation, 1L, Long::sum);
        logger.error("Operation {} failed: {}", operation, exception.getMessage(), exception);
    }
}
