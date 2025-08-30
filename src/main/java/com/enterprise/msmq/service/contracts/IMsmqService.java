package com.enterprise.msmq.service.contracts;

import com.enterprise.msmq.dto.MsmqMessage;
import com.enterprise.msmq.dto.MsmqQueue;
import com.enterprise.msmq.dto.ConnectionStatus;
import com.enterprise.msmq.dto.SystemHealth;
import com.enterprise.msmq.dto.PerformanceMetrics;
import com.enterprise.msmq.dto.ErrorStatistics;
import com.enterprise.msmq.dto.HealthCheckResult;
import com.enterprise.msmq.exception.MsmqException;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for MSMQ operations.
 * 
 * This interface defines all business operations for MSMQ management including
 * queue operations, message operations, and connection management. All business
 * logic is implemented in the service layer as per enterprise requirements.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface IMsmqService {

    // Queue Management Operations

    /**
     * Creates a new MSMQ queue.
     * 
     * @param queue the queue information
     * @return the created queue
     * @throws MsmqException if queue creation fails
     */
    MsmqQueue createQueue(MsmqQueue queue) throws MsmqException;

    /**
     * Deletes an existing MSMQ queue.
     * 
     * @param queueName the name of the queue to delete
     * @throws MsmqException if queue deletion fails
     */
    void deleteQueue(String queueName) throws MsmqException;

    /**
     * Retrieves information about a specific queue.
     * 
     * @param queueName the name of the queue
     * @return the queue information
     * @throws MsmqException if queue retrieval fails
     */
    MsmqQueue getQueue(String queueName) throws MsmqException;

    /**
     * Lists all available MSMQ queues.
     * 
     * @return list of all queues
     * @throws MsmqException if queue listing fails
     */
    List<MsmqQueue> listQueues() throws MsmqException;

    /**
     * Updates queue properties.
     * 
     * @param queueName the name of the queue to update
     * @param queue the updated queue information
     * @return the updated queue
     * @throws MsmqException if queue update fails
     */
    MsmqQueue updateQueue(String queueName, MsmqQueue queue) throws MsmqException;

    /**
     * Checks if a queue exists.
     * 
     * @param queueName the name of the queue to check
     * @return true if the queue exists
     * @throws MsmqException if check operation fails
     */
    boolean queueExists(String queueName) throws MsmqException;

    /**
     * Gets queue statistics and health information.
     * 
     * @param queueName the name of the queue
     * @return queue statistics
     * @throws MsmqException if statistics retrieval fails
     */
    MsmqQueue getQueueStatistics(String queueName) throws MsmqException;

    // Message Operations

    /**
     * Sends a message to a specific queue.
     * 
     * @param queueName the destination queue name
     * @param message the message to send
     * @return the sent message with updated information
     * @throws MsmqException if message sending fails
     */
    MsmqMessage sendMessage(String queueName, MsmqMessage message) throws MsmqException;

    /**
     * Receives a message from a specific queue.
     * 
     * @param queueName the source queue name
     * @param timeout the timeout in milliseconds
     * @return the received message, or empty if no message available
     * @throws MsmqException if message receiving fails
     */
    Optional<MsmqMessage> receiveMessage(String queueName, long timeout) throws MsmqException;

    /**
     * Receives a message from a specific queue with default timeout.
     * 
     * @param queueName the source queue name
     * @return the received message, or empty if no message available
     * @throws MsmqException if message receiving fails
     */
    Optional<MsmqMessage> receiveMessage(String queueName) throws MsmqException;

    /**
     * Peeks at a message in the queue without removing it.
     * 
     * @param queueName the queue name
     * @param timeout the timeout in milliseconds
     * @return the peeked message, or empty if no message available
     * @throws MsmqException if message peeking fails
     */
    Optional<MsmqMessage> peekMessage(String queueName, long timeout) throws MsmqException;

    /**
     * Peeks at a message in the queue with default timeout.
     * 
     * @param queueName the queue name
     * @return the peeked message, or empty if no message available
     * @throws MsmqException if message peeking fails
     */
    Optional<MsmqMessage> peekMessage(String queueName) throws MsmqException;

    /**
     * Purges all messages from a queue.
     * 
     * @param queueName the queue name
     * @throws MsmqException if queue purging fails
     */
    void purgeQueue(String queueName) throws MsmqException;

    /**
     * Gets the number of messages in a queue.
     * 
     * @param queueName the queue name
     * @return the message count
     * @throws MsmqException if count retrieval fails
     */
    long getMessageCount(String queueName) throws MsmqException;

    // Connection and Session Management

    /**
     * Establishes a connection to the MSMQ service.
     * 
     * @throws MsmqException if connection fails
     */
    void connect() throws MsmqException;

    /**
     * Disconnects from the MSMQ service.
     * 
     * @throws MsmqException if disconnection fails
     */
    void disconnect() throws MsmqException;

    /**
     * Checks if the connection to MSMQ is active.
     * 
     * @return true if connected
     */
    boolean isConnected();

    /**
     * Gets connection status and health information.
     * 
     * @return connection status information
     */
    ConnectionStatus getConnectionStatus();

    /**
     * Reconnects to the MSMQ service.
     * 
     * @throws MsmqException if reconnection fails
     */
    void reconnect() throws MsmqException;

    // Monitoring and Health Operations

    /**
     * Gets overall MSMQ system health status.
     * 
     * @return system health information
     */
    SystemHealth getSystemHealth();

    /**
     * Gets performance metrics for MSMQ operations.
     * 
     * @return performance metrics
     */
    PerformanceMetrics getPerformanceMetrics();

    /**
     * Gets error statistics and recent errors.
     * 
     * @return error statistics
     */
    ErrorStatistics getErrorStatistics();

    // Utility Operations

    /**
     * Validates MSMQ configuration.
     * 
     * @return true if configuration is valid
     */
    boolean validateConfiguration();

    /**
     * Gets MSMQ version information.
     * 
     * @return version information
     */
    String getMsmqVersion();

    /**
     * Performs a health check on the MSMQ service.
     * 
     * @return health check result
     */
    HealthCheckResult performHealthCheck();
}
