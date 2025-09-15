package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.*;
import com.enterprise.msmq.enums.QueueDirection;
import com.enterprise.msmq.enums.QueuePurpose;
import com.enterprise.msmq.enums.ResponseCode;
import com.enterprise.msmq.exception.MsmqException;
import com.enterprise.msmq.factory.MsmqConnectionFactory;
import com.enterprise.msmq.service.contracts.IMsmqConnectionManager;
import com.enterprise.msmq.service.contracts.IMsmqService;
import com.enterprise.msmq.util.MsmqMessageParser;
import com.enterprise.msmq.factory.MsmqQueueManagerFactory;
import com.enterprise.msmq.service.contracts.IMsmqQueueManager;
import com.enterprise.msmq.validator.MsmqConfigurationValidator;
import com.enterprise.msmq.repository.MsmqQueueConfigRepository;
import com.enterprise.msmq.model.MsmqQueueConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.LocalDateTime;

/**
 * Implementation of MSMQ Service interface.
 * <p>
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
public class MsmqService implements IMsmqService {

    private static final Logger logger = LoggerFactory.getLogger(MsmqService.class);

    private final MsmqConnectionFactory connectionFactory;
    private final MsmqQueueManagerFactory queueManagerFactory;
    private final MsmqMessageParser messageParser;
    private final RedisMetricsService redisMetricsService;
    private final MsmqConfigurationValidator configurationValidator;
    private final MsmqQueueConfigRepository queueConfigRepository;

    private IMsmqConnectionManager connectionManager;
    private IMsmqQueueManager queueManager;

    @PostConstruct
    private void init() {
        try {
            this.connectionManager = connectionFactory.createConnectionManager();
            if (this.connectionManager == null) {
                throw new IllegalStateException("Connection manager creation failed");
            }
            logger.info("Initialized MSMQ connection manager using type: {}", connectionFactory.getConnectionType());
            
            this.queueManager = queueManagerFactory.createQueueManager();
            if (this.queueManager == null) {
                throw new IllegalStateException("Queue manager creation failed");
            }
            logger.info("Initialized MSMQ queue manager using type: {}", queueManagerFactory.getConfiguredConnectionType());
        } catch (Exception e) {
            logger.error("Failed to initialize MSMQ managers", e);
            throw new IllegalStateException("Failed to initialize MSMQ managers", e);
        }
    }

    /**
     * Updates operation timing metrics in Redis.
     *
     * @param operation the operation name
     * @param duration the operation duration in milliseconds
     */
    private void updateOperationTiming(String operation, long duration) {
        try {
            redisMetricsService.storeOperationTiming(operation, duration);
        } catch (Exception e) {
            logger.warn("Failed to update operation timing metrics: {}", e.getMessage());
            // Don't throw - metrics are non-critical
        }
    }

    /**
     * Handles operation errors by updating metrics and logging.
     *
     * @param operation the operation name
     * @param exception the exception that occurred
     */
    private void handleOperationError(String operation, MsmqException exception) {
        try {
            redisMetricsService.incrementErrorCount(operation);
            redisMetricsService.storeLastError(operation, exception.getMessage());
            logger.error("Operation '{}' failed: {}", operation, exception.getMessage());
        } catch (Exception e) {
            logger.warn("Failed to update error metrics: {}", e.getMessage());
            // Don't throw - metrics are non-critical
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
            throw new MsmqException(ResponseCode.VALIDATION_ERROR, "Queue cannot be null");
        }
        if (queue.getName() == null || queue.getName().trim().isEmpty()) {
            throw new MsmqException(ResponseCode.INVALID_QUEUE_NAME, "Queue name cannot be null or empty");
        }
        // Add additional validation as needed
    }

    @PreDestroy
    private void cleanup() {
        try {
            if (isConnected()) {
                disconnect();
            }
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }

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
            boolean success = queueManager.createQueue(queue);
            if (!success) {
                throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to create queue: " + queue.getName());
            }

            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully created MSMQ queue: {}", queue.getName());

            return queue;

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
            Optional<MsmqQueue> queueOpt = queueManager.getQueue(queueName);

            if (queueOpt.isEmpty()) {
                throw new MsmqException(ResponseCode.QUEUE_NOT_FOUND, "Queue not found: " + queueName);
            }
            
            MsmqQueue queue = queueOpt.get();

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
            List<MsmqQueue> queues = queueManager.getAllQueues();

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
            boolean success = queueManager.updateQueue(queueName, queue);
            if (!success) {
                throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to update queue: " + queueName);
            }

            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully updated MSMQ queue: {}", queueName);

            return queue;

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
            Optional<MsmqQueue> statisticsOpt = queueManager.getQueueStatistics(queueName);
            if (statisticsOpt.isEmpty()) {
                throw new MsmqException(ResponseCode.QUEUE_NOT_FOUND, "Queue statistics not found: " + queueName);
            }
            MsmqQueue statistics = statisticsOpt.get();

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

            // NEW: Validate queue direction for sending messages
            validateQueueDirectionForSending(queueName);

            // Ensure connection is active
            ensureConnection();

            // Parse and validate message
            MsmqMessage parsedMessage = messageParser.parseOutgoingMessage(message);

            // Send message using queue manager
            boolean success = queueManager.sendMessage(queueName, parsedMessage);
            if (!success) {
                throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to send message to queue: " + queueName);
            }

            // Update metrics
            redisMetricsService.incrementTotalMessageCount("sent");
            redisMetricsService.storeOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully sent message to MSMQ queue: {}", queueName);

            return parsedMessage;

        } catch (MsmqException e) {
            handleOperationError(operation, e);
            throw e;
        } catch (Exception e) {
            handleOperationError(operation, new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to send message to queue: " + queueName, e));
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to send message to queue: " + queueName, e);
        }
    }

    @Override
    public MsmqMessage sendMessage(String queueName, MsmqMessage message, String environment) throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "SEND_MESSAGE_REMOTE";

        try {
            logger.info("Sending message to MSMQ queue: {} (environment: {})", queueName, environment);

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
            if (environment == null || environment.trim().isEmpty()) {
                throw new MsmqException(ResponseCode.VALIDATION_ERROR, "Environment cannot be null or empty");
            }

            // Validate environment
            if (!"local".equalsIgnoreCase(environment) && !"remote".equalsIgnoreCase(environment)) {
                throw new MsmqException(ResponseCode.VALIDATION_ERROR, "Environment must be 'local' or 'remote'");
            }

            // Check if queue exists (only for local environment)
            if ("local".equalsIgnoreCase(environment) && !queueExists(queueName)) {
                throw new MsmqException(ResponseCode.QUEUE_NOT_FOUND, "Queue not found: " + queueName);
            }

            // NEW: Validate queue direction for sending messages (only for local)
            if ("local".equalsIgnoreCase(environment)) {
                validateQueueDirectionForSending(queueName);
            }

            // Ensure connection is active
            ensureConnection();

            // Parse and validate message
            MsmqMessage parsedMessage = messageParser.parseOutgoingMessage(message);

            // Send message using appropriate queue manager based on environment
            boolean success;
            if ("remote".equalsIgnoreCase(environment)) {
                // For remote sending, use the proper remote sending methods
                logger.info("Sending to remote environment: {} using remote machine: 192.168.2.170", queueName);
                
                // Check if queueName is already a FormatName path
                if (queueName.toUpperCase().startsWith("FORMATNAME:")) {
                    success = queueManager.sendMessageToRemote(queueName, parsedMessage);
                } else {
                    // Use machine name and queue name for remote sending
                    success = queueManager.sendMessageToRemote("192.168.2.170", queueName, parsedMessage);
                }
            } else {
                // Local sending
                success = queueManager.sendMessage(queueName, parsedMessage);
            }

            if (!success) {
                throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to send message to queue: " + queueName);
            }

            // Update metrics
            redisMetricsService.incrementTotalMessageCount("sent");
            redisMetricsService.storeOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully sent message to MSMQ queue: {} (environment: {})", queueName, environment);

            return parsedMessage;

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

            // NEW: Validate queue direction for receiving messages
            validateQueueDirectionForReceiving(queueName);

            // Ensure connection is active
            ensureConnection();

            // Receive message using queue manager
            Optional<MsmqMessage> receivedMessage = queueManager.receiveMessage(queueName, timeout);

            if (receivedMessage.isPresent()) {
                // Parse incoming message
                MsmqMessage parsedMessage = messageParser.parseIncomingMessage(receivedMessage.get());
                receivedMessage = Optional.of(parsedMessage);

                // Update metrics
                redisMetricsService.incrementTotalMessageCount("received");
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
            ConnectionStatus status = connectionManager.getConnectionStatus();
            status.setStatus("CONNECTING");

            // Connect using connection manager
            connectionManager.connect();

            // Update connection status on success
            status = connectionManager.getConnectionStatus();
            status.setStatus("CONNECTED");
            status.setLastConnected(System.currentTimeMillis());

            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully connected to MSMQ service");

        } catch (Exception e) {
            // Update connection status on failure
            try {
                ConnectionStatus status = connectionManager.getConnectionStatus();
                status.setStatus("ERROR");
                status.setErrorMessage(e.getMessage());
                status.setLastError(System.currentTimeMillis());
            } catch (Exception statusError) {
                logger.error("Failed to update connection status", statusError);
            }

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
            ConnectionStatus status = connectionManager.getConnectionStatus();
            status.setStatus("DISCONNECTING");

            // Disconnect using connection manager
            connectionManager.disconnect();

            // Update connection status on success
            status = connectionManager.getConnectionStatus();
            status.setStatus("DISCONNECTED");
            status.setLastDisconnected(System.currentTimeMillis());

            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully disconnected from MSMQ service");

        } catch (Exception e) {
            // Update connection status on failure
            try {
                ConnectionStatus status = connectionManager.getConnectionStatus();
                status.setStatus("ERROR");
                status.setErrorMessage(e.getMessage());
                status.setLastError(System.currentTimeMillis());
            } catch (Exception statusError) {
                logger.error("Failed to update connection status", statusError);
            }

            handleOperationError(operation, new MsmqException(ResponseCode.CONNECTION_ERROR, "Failed to disconnect from MSMQ service", e));
            throw new MsmqException(ResponseCode.CONNECTION_ERROR, "Failed to disconnect from MSMQ service", e);
        }
    }

    @Override
    public void reconnect() throws MsmqException {
        long startTime = System.currentTimeMillis();
        String operation = "RECONNECT";

        try {
            logger.info("Reconnecting to MSMQ service");
            ConnectionStatus status = connectionManager.getConnectionStatus();
            status.setStatus("RECONNECTING");

            // Reconnect using connection manager
            connectionManager.reconnect();

            // Update connection status on success
            status = connectionManager.getConnectionStatus();
            status.setStatus("CONNECTED");
            status.setLastConnected(System.currentTimeMillis());

            // Update metrics
            updateOperationTiming(operation, System.currentTimeMillis() - startTime);
            logger.info("Successfully reconnected to MSMQ service");

        } catch (Exception e) {
            // Update connection status on failure
            try {
                ConnectionStatus status = connectionManager.getConnectionStatus();
                status.setStatus("ERROR");
                status.setErrorMessage(e.getMessage());
                status.setLastError(System.currentTimeMillis());
            } catch (Exception statusError) {
                logger.error("Failed to update connection status", statusError);
            }

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
            // Validate connection manager exists
            if (connectionManager == null) {
                logger.error("Connection manager is null");
                return false;
            }

            // Validate configuration values
            try {
                // Use configuration validator
                configurationValidator.validateHostname(connectionManager.getHost());
                configurationValidator.validatePort(connectionManager.getPort());
                configurationValidator.validateTimeout(connectionManager.getTimeout());
                configurationValidator.validateRetryAttempts(connectionManager.getRetryAttempts());
            } catch (MsmqException e) {
                logger.error("Configuration validation failed: {}", e.getMessage());
                return false;
            }

            // Validate connection status
            ConnectionStatus status = connectionManager.getConnectionStatus();
            if (status == null) {
                logger.error("Unable to get connection status");
                return false;
            }

            // Test connection if not already connected
            if (!status.isConnected() && !testConnection()) {
                logger.error("Connection test failed");
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("Configuration validation failed", e);
            return false;
        }
    }

    /**
     * Ensures that the MSMQ connection is active with exponential backoff retry.
     *
     * @throws MsmqException if connection is not available
     */
    private void ensureConnection() throws MsmqException {
        if (!isConnected()) {
            logger.warn("MSMQ connection not available, attempting to reconnect");
            try {
                reconnect();
                // Wait for connection to be established with exponential backoff
                long startTime = System.currentTimeMillis();
                long timeout = 30000; // 30 seconds timeout
                long waitTime = 100; // Start with 100ms wait
                int attempts = 0;
                int maxAttempts = 10;

                while (!isConnected() &&
                       (System.currentTimeMillis() - startTime) < timeout &&
                       attempts < maxAttempts) {
                    try {
                        Thread.sleep(waitTime);
                        waitTime = Math.min(waitTime * 2, 1000); // Double wait time up to 1 second max
                        attempts++;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new MsmqException(ResponseCode.CONNECTION_INTERRUPTED,
                            "Connection attempt interrupted", e);
                    }
                }

                if (!isConnected()) {
                    if (attempts >= maxAttempts) {
                        throw new MsmqException(ResponseCode.CONNECTION_ERROR,
                            "Maximum reconnection attempts reached");
                    } else {
                        throw new MsmqException(ResponseCode.CONNECTION_TIMEOUT,
                            "Connection attempt timed out");
                    }
                }
            } catch (MsmqException e) {
                throw new MsmqException(ResponseCode.MSMQ_NOT_AVAILABLE,
                    "MSMQ connection not available", e);
            }
        }
    }

    private boolean testConnection() {
        try {
            connect();
            return isConnected();
        } catch (Exception e) {
            logger.error("Connection test failed: {}", e.getMessage());
            return false;
        } finally {
            try {
                if (isConnected()) {
                    disconnect();
                }
            } catch (Exception e) {
                logger.warn("Error disconnecting after connection test", e);
            }
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
        try {
            logger.debug("Performing comprehensive MSMQ health check...");

            HealthCheckResult healthCheck = new HealthCheckResult();
            healthCheck.setStatus("HEALTHY");
            healthCheck.setTimestamp(LocalDateTime.now());

            // Check 1: MSMQ Connection Status
            try {
                ConnectionStatus status = connectionManager.getConnectionStatus();
                if (status.isConnected()) {
                    logger.debug("MSMQ connection health check: PASSED");
                    // Store MSMQ status in components map
                    if (healthCheck.getComponents() == null) {
                        healthCheck.setComponents(new HashMap<>());
                    }
                    healthCheck.getComponents().put("MSMQ_CONNECTION", "HEALTHY");
                    healthCheck.getComponents().put("MSMQ_CONNECTION_TYPE", connectionFactory.getConnectionType().toString());
                    healthCheck.getComponents().put("MSMQ_STATUS", "Connected and operational");
                } else {
                    healthCheck.setStatus("UNHEALTHY");
                    logger.warn("MSMQ connection health check: FAILED");
                    if (healthCheck.getComponents() == null) {
                        healthCheck.setComponents(new HashMap<>());
                    }
                    healthCheck.getComponents().put("MSMQ_CONNECTION", "UNHEALTHY");
                    healthCheck.getComponents().put("MSMQ_STATUS", "Connection test failed");
                    healthCheck.getComponents().put("MSMQ_DIAGNOSTIC", "Check MSMQ service and permissions");
                }
            } catch (Exception e) {
                healthCheck.setStatus("UNHEALTHY");
                if (healthCheck.getComponents() == null) {
                    healthCheck.setComponents(new HashMap<>());
                }
                healthCheck.getComponents().put("MSMQ_CONNECTION", "ERROR");
                logger.error("MSMQ connection health check error", e);
            }

            // Check 2: Configuration Validation
            try {
                if (validateConfiguration()) {
                    if (healthCheck.getComponents() == null) {
                        healthCheck.setComponents(new HashMap<>());
                    }
                    healthCheck.getComponents().put("CONFIGURATION", "HEALTHY");
                } else {
                    healthCheck.setStatus("UNHEALTHY");
                    if (healthCheck.getComponents() == null) {
                        healthCheck.setComponents(new HashMap<>());
                    }
                    healthCheck.getComponents().put("CONFIGURATION", "UNHEALTHY");
                }
            } catch (Exception e) {
                healthCheck.setStatus("UNHEALTHY");
                if (healthCheck.getComponents() == null) {
                    healthCheck.setComponents(new HashMap<>());
                }
                healthCheck.getComponents().put("CONFIGURATION", "ERROR");
            }

            // Check 3: Database Connection (if applicable)
            try {
                // This would check database connectivity
                healthCheck.getComponents().put("DATABASE", "HEALTHY");
            } catch (Exception e) {
                healthCheck.setStatus("UNHEALTHY");
                if (healthCheck.getComponents() == null) {
                    healthCheck.setComponents(new HashMap<>());
                }
                healthCheck.getComponents().put("DATABASE", "UNHEALTHY");
            }

            // Check 4: System Resources
            try {
                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                long usedMemory = totalMemory - freeMemory;
                double memoryUsagePercent = (double) usedMemory / maxMemory * 100;

                if (healthCheck.getComponents() == null) {
                    healthCheck.setComponents(new HashMap<>());
                }

                if (memoryUsagePercent < 80) {
                    healthCheck.getComponents().put("MEMORY", "HEALTHY");
                    healthCheck.getComponents().put("MEMORY_USAGE",
                        String.format("%.1f%% (%.1f MB / %.1f MB)",
                            memoryUsagePercent, usedMemory / 1024.0 / 1024.0, maxMemory / 1024.0 / 1024.0));
                } else {
                    healthCheck.getComponents().put("MEMORY", "WARNING");
                    healthCheck.getComponents().put("MEMORY_USAGE",
                        String.format("%.1f%%", memoryUsagePercent));
                }
            } catch (Exception e) {
                if (healthCheck.getComponents() == null) {
                    healthCheck.setComponents(new HashMap<>());
                }
                healthCheck.getComponents().put("MEMORY", "ERROR");
            }

            logger.info("Health check completed with status: {}", healthCheck.getStatus());
            return healthCheck;

        } catch (Exception e) {
            logger.error("Health check failed", e);
            HealthCheckResult errorResult = new HealthCheckResult();
            errorResult.setStatus("ERROR");
            errorResult.setTimestamp(LocalDateTime.now());
            if (errorResult.getComponents() == null) {
                errorResult.setComponents(new HashMap<>());
            }
            errorResult.getComponents().put("HEALTH_CHECK", "ERROR");
            return errorResult;
        }
    }

    @Override
    public boolean isConnected() {
        try {
            if (connectionManager == null) {
                return false;
            }
            ConnectionStatus status = connectionManager.getConnectionStatus();
            return status != null && status.isConnected();
        } catch (Exception e) {
            logger.error("Error checking connection status: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        try {
            if (connectionManager == null) {
                ConnectionStatus status = new ConnectionStatus();
                status.setStatus("NOT_INITIALIZED");
                status.setErrorMessage("Connection manager not initialized");
                status.setConnected(false);
                return status;
            }
            return connectionManager.getConnectionStatus();
        } catch (Exception e) {
            logger.error("Error getting connection status: {}", e.getMessage());
            ConnectionStatus errorStatus = new ConnectionStatus();
            errorStatus.setStatus("ERROR");
            errorStatus.setErrorMessage(e.getMessage());
            errorStatus.setLastError(System.currentTimeMillis());
            errorStatus.setConnected(false);
            return errorStatus;
        }
    }

    // Private helper methods
    
    // =====================================================
    // NEW: Queue Direction Validation Methods
    // =====================================================
    
    /**
     * Validate that a queue allows sending messages.
     * 
     * @param queueName the name of the queue to validate
     * @throws MsmqException if the queue does not allow sending
     */
    private void validateQueueDirectionForSending(String queueName) throws MsmqException {
        try {
            // Get queue configuration from database
            var queueConfigOpt = queueConfigRepository.findByQueueName(queueName);
            
            if (queueConfigOpt.isPresent()) {
                var queueConfig = queueConfigOpt.get();
                QueueDirection direction = queueConfig.getQueueDirection();
                
                if (!direction.allowsSending()) {
                    String errorMsg = String.format(
                        "Queue '%s' is configured as %s and does not allow sending messages", 
                        queueName, direction.getDisplayName());
                    
                    logger.warn("Direction validation failed for sending: {}", errorMsg);
                    throw new MsmqException(ResponseCode.BUSINESS_ERROR, errorMsg);
                }
                
                logger.debug("Queue '{}' direction validation passed for sending (direction: {})", 
                           queueName, direction.getDisplayName());
            } else {
                // Queue not in database yet, allow operation (will be created during sync)
                logger.debug("Queue '{}' not found in database, allowing send operation", queueName);
            }
            
        } catch (MsmqException e) {
            // Re-throw business logic exceptions
            throw e;
        } catch (Exception e) {
            // Log unexpected errors but don't fail the operation
            logger.warn("Error during queue direction validation for sending to '{}': {}", queueName, e.getMessage());
        }
    }
    
    /**
     * Validate that a queue allows receiving messages.
     * 
     * @param queueName the name of the queue to validate
     * @throws MsmqException if the queue does not allow receiving
     */
    private void validateQueueDirectionForReceiving(String queueName) throws MsmqException {
        try {
            // Get queue configuration from database
            var queueConfigOpt = queueConfigRepository.findByQueueName(queueName);
            
            if (queueConfigOpt.isPresent()) {
                var queueConfig = queueConfigOpt.get();
                QueueDirection direction = queueConfig.getQueueDirection();
                
                if (!direction.allowsReceiving()) {
                    String errorMsg = String.format(
                        "Queue '%s' is configured as %s and does not allow receiving messages", 
                        queueName, direction.getDisplayName());
                    
                    logger.warn("Direction validation failed for receiving: {}", errorMsg);
                    throw new MsmqException(ResponseCode.BUSINESS_ERROR, errorMsg);
                }
                
                logger.debug("Queue '{}' direction validation passed for receiving (direction: {})", 
                           queueName, direction.getDisplayName());
            } else {
                // Queue not in database yet, allow operation (will be created during sync)
                logger.debug("Queue '{}' not found in database, allowing receive operation", queueName);
            }
            
        } catch (MsmqException e) {
            // Re-throw business logic exceptions
            throw e;
        } catch (Exception e) {
            // Log unexpected errors but don't fail the operation
            logger.warn("Error during queue direction validation for receiving from '{}': {}", queueName, e.getMessage());
        }
    }
    
    /**
     * Get all queues by direction for filtering operations.
     * 
     * @param direction the queue direction to filter by
     * @return list of queue names with the specified direction
     */
    public List<String> getQueueNamesByDirection(QueueDirection direction) {
        try {
            var queueConfigs = queueConfigRepository.findByQueueDirectionAndIsActiveTrue(direction);
            return queueConfigs.stream()
                .map(MsmqQueueConfig::getQueueName)
                .toList();
        } catch (Exception e) {
            logger.error("Error getting queue names by direction: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get all queues by purpose for filtering operations.
     * 
     * @param direction the queue purpose to filter by
     * @return list of queue names with the specified purpose
     */
    public List<String> getQueueNamesByPurpose(QueuePurpose purpose) {
        try {
            var queueConfigs = queueConfigRepository.findByQueuePurposeAndIsActiveTrue(purpose);
            return queueConfigs.stream()
                .map(MsmqQueueConfig::getQueueName)
                .toList();
        } catch (Exception e) {
            logger.error("Error getting queue names by purpose: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get comprehensive queue information including direction and purpose.
     * 
     * @param queueName the name of the queue
     * @return queue information with direction and purpose details
     */
    public Map<String, Object> getQueueInfo(String queueName) throws MsmqException {
        try {
            // Get MSMQ queue details
            MsmqQueue msmqQueue = getQueue(queueName);
            
            // Get database configuration details
            var queueConfigOpt = queueConfigRepository.findByQueueName(queueName);
            
            Map<String, Object> queueInfo = new HashMap<>();
            queueInfo.put("name", msmqQueue.getName());
            queueInfo.put("path", msmqQueue.getPath());
            queueInfo.put("messageCount", msmqQueue.getMessageCount());
            queueInfo.put("size", msmqQueue.getSize());
            
            if (queueConfigOpt.isPresent()) {
                var queueConfig = queueConfigOpt.get();
                queueInfo.put("direction", queueConfig.getQueueDirection());
                queueInfo.put("purpose", queueConfig.getQueuePurpose());
                queueInfo.put("isActive", queueConfig.getIsActive());
                queueInfo.put("lastSyncTime", queueConfig.getLastSyncTime());
                queueInfo.put("allowsSending", queueConfig.getQueueDirection().allowsSending());
                queueInfo.put("allowsReceiving", queueConfig.getQueueDirection().allowsReceiving());
            } else {
                queueInfo.put("direction", "UNKNOWN");
                queueInfo.put("purpose", "UNKNOWN");
                queueInfo.put("isActive", false);
                queueInfo.put("lastSyncTime", null);
                queueInfo.put("allowsSending", true); // Default to true for unknown queues
                queueInfo.put("allowsReceiving", true);
            }
            
            return queueInfo;
            
        } catch (Exception e) {
            logger.error("Error getting comprehensive queue info for '{}': {}", queueName, e.getMessage());
            throw new MsmqException(ResponseCode.SYSTEM_ERROR, "Failed to get queue information", e);
        }
    }
}
