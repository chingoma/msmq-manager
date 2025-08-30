package com.enterprise.msmq.service.contracts;

import com.enterprise.msmq.dto.MsmqMessage;
import com.enterprise.msmq.dto.MsmqQueue;

import java.util.List;
import java.util.Optional;

/**
 * Interface for MSMQ queue management operations.
 * Defines the contract for queue CRUD operations and message handling.
 * Implementations can use different technologies (PowerShell, Native, etc.)
 *
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
public interface IMsmqQueueManager {

    /**
     * Creates a new MSMQ queue.
     *
     * @param queue the queue configuration
     * @return true if queue created successfully, false otherwise
     */
    boolean createQueue(MsmqQueue queue);

    /**
     * Deletes an existing MSMQ queue.
     *
     * @param queuePath the path/name of the queue to delete
     * @return true if queue deleted successfully, false otherwise
     */
    boolean deleteQueue(String queuePath);

    /**
     * Checks if a queue exists.
     *
     * @param queuePath the path/name of the queue to check
     * @return true if queue exists, false otherwise
     */
    boolean queueExists(String queuePath);

    /**
     * Sends a message to a specific queue.
     *
     * @param queuePath the path/name of the target queue
     * @param message the message to send
     * @return true if message sent successfully, false otherwise
     */
    boolean sendMessage(String queuePath, MsmqMessage message);

    /**
     * Sends a message to a specific queue with raw content.
     *
     * @param queuePath the path/name of the target queue
     * @param messageBody the raw message content
     * @return true if message sent successfully, false otherwise
     */
    boolean sendMessage(String queuePath, String messageBody);

    /**
     * Receives a message from a specific queue.
     *
     * @param queuePath the path/name of the source queue
     * @return the received message, or empty if no message available
     */
    Optional<MsmqMessage> receiveMessage(String queuePath);

    /**
     * Peeks at a message in a queue without removing it.
     *
     * @param queuePath the path/name of the queue to peek
     * @return the peeked message, or empty if no message available
     */
    Optional<MsmqMessage> peekMessage(String queuePath);

    /**
     * Purges all messages from a queue.
     *
     * @param queuePath the path/name of the queue to purge
     * @return true if queue purged successfully, false otherwise
     */
    boolean purgeQueue(String queuePath);

    /**
     * Gets all available queues.
     *
     * @return list of available queues
     */
    List<MsmqQueue> getAllQueues();

    /**
     * Gets a specific queue by path/name.
     *
     * @param queuePath the path/name of the queue
     * @return the queue if found, or empty otherwise
     */
    Optional<MsmqQueue> getQueue(String queuePath);

    /**
     * Gets the count of messages in a queue.
     *
     * @param queuePath the path/name of the queue
     * @return the number of messages in the queue, or -1 if error
     */
    long getMessageCount(String queuePath);

    /**
     * Tests the queue manager connectivity.
     *
     * @return true if connectivity test passes, false otherwise
     */
    boolean testConnectivity();

    /**
     * Updates an existing queue configuration.
     *
     * @param queuePath the path/name of the queue to update
     * @param queue the updated queue configuration
     * @return true if queue updated successfully, false otherwise
     */
    boolean updateQueue(String queuePath, MsmqQueue queue);

    /**
     * Gets queue statistics.
     *
     * @param queuePath the path/name of the queue
     * @return the queue statistics, or empty if not found
     */
    Optional<MsmqQueue> getQueueStatistics(String queuePath);

    /**
     * Receives a message from a specific queue with timeout.
     *
     * @param queuePath the path/name of the source queue
     * @param timeout the timeout in milliseconds
     * @return the received message, or empty if no message available or timeout
     */
    Optional<MsmqMessage> receiveMessage(String queuePath, long timeout);

    /**
     * Peeks at a message in a queue without removing it, with timeout.
     *
     * @param queuePath the path/name of the queue to peek
     * @param timeout the timeout in milliseconds
     * @return the peeked message, or empty if no message available or timeout
     */
    Optional<MsmqMessage> peekMessage(String queuePath, long timeout);
}
