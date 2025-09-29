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
     */
    void purgeQueue(String queuePath);

    /**
     * Gets all available queues.
     *
     * @return list of available queues
     */
    List<MsmqQueue> getAllQueues();

    /**
     * Gets all available queues from a remote MSMQ server.
     *
     * @param remoteHost the remote MSMQ server hostname or IP
     * @return list of available queues from remote server
     */
    List<MsmqQueue> getAllRemoteQueues(String remoteHost);

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

    /**
     * Sends a raw message body to a remote MSMQ queue (TCP/UNC/FormatName).
     *
     * @param remoteQueuePath the full remote queue path (e.g., TCP:ip\\private$\\QueueName)
     * @param messageBody the message content to send
     * @return true if message sent successfully, false otherwise
     */
    boolean sendMessageToRemote(String remoteQueuePath, String messageBody);

    /**
     * Sends a MsmqMessage object to a remote MSMQ queue (TCP/UNC/FormatName).
     *
     * @param remoteQueuePath the full remote queue path (e.g., TCP:ip\\private$\\QueueName)
     * @param message the MsmqMessage object to send
     * @return true if message sent successfully, false otherwise
     */
    boolean sendMessageToRemote(String remoteQueuePath, MsmqMessage message);

    String convertToTcpPath(String queuePath);

    /**
     * Sends a raw message body to a remote MSMQ queue by specifying machine and queue name.
     *
     * @param remoteMachine the remote machine name or IP
     * @param queueName the queue name (without path prefix)
     * @param messageBody the message content to send
     * @return true if message sent successfully, false otherwise
     */
    boolean sendMessageToRemote(String remoteMachine, String queueName, String messageBody);

    /**
     * Sends a MsmqMessage object to a remote MSMQ queue by specifying machine and queue name.
     *
     * @param remoteMachine the remote machine name or IP
     * @param queueName the queue name (without path prefix)
     * @param message the MsmqMessage object to send
     * @return true if message sent successfully, false otherwise
     */
    boolean sendMessageToRemote(String remoteMachine, String queueName, MsmqMessage message);

}
