package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.ConnectionStatus;
import com.enterprise.msmq.enums.ResponseCode;
import com.enterprise.msmq.exception.MsmqException;
import com.enterprise.msmq.platform.windows.MsmqNativeInterface;
import com.enterprise.msmq.platform.windows.MsmqConstants;
import com.enterprise.msmq.service.contracts.IMsmqConnectionManager;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;

@Component
public class NativeMsmqConnectionService implements IMsmqConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(NativeMsmqConnectionService.class);

    @Value("${msmq.connection.host:localhost}")
    private String msmqHost;

    @Value("${msmq.connection.port:1801}")
    private int msmqPort;

    @Value("${msmq.connection.retry-attempts:3}")
    private int retryAttempts;

    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    /**
     * Map to store queue handles for open queues.
     */
    private final Map<String, Pointer> queueHandles = new ConcurrentHashMap<>();

    /**
     * Establishes a connection to the MSMQ service.
     *
     * @throws MsmqException if connection fails
     */
    @Override
    public void connect() throws MsmqException {
        logger.info("Establishing connection to MSMQ service at {}:{}", msmqHost, msmqPort);

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                logger.info("Connection attempt {} of {} to MSMQ service at {}:{}",
                    attempt, retryAttempts, msmqHost, msmqPort);

                if (testConnection()) {
                    isConnected.set(true);
                    logger.info("Successfully connected to MSMQ service at {}:{}", msmqHost, msmqPort);
                    return;
                }
            } catch (Exception e) {
                logger.error("Attempt {} failed: {}", attempt, e.getMessage());
                if (attempt == retryAttempts) {
                    logger.error("Failed to establish connection to MSMQ service at {}:{}", msmqHost, msmqPort);
                    throw new MsmqException(ResponseCode.CONNECTION_ERROR,
                        "Failed to establish connection to MSMQ service", e);
                }
            }
        }
        throw new MsmqException(ResponseCode.CONNECTION_ERROR, "Failed to establish connection to MSMQ service");
    }

    private boolean testConnection() {
        try {
            logger.debug("Testing MSMQ connection...");

            // Test 1: Verify native library can be loaded
            try {
                // Just checking if accessing INSTANCE throws an exception
                MsmqNativeInterface.INSTANCE.toString();
                logger.debug("MSMQ native library loaded successfully");
            } catch (Exception e) {
                logger.error("Failed to load MSMQ native library: {}", e.getMessage(), e);
                return false;
            }

            // Test 2: Try to create a temporary test queue using the correct path format
            String testQueuePath = ".\\private$\\msmq-test-" + System.currentTimeMillis();
            PointerByReference queueHandleRef = new PointerByReference();

            logger.debug("Attempting to create test queue: {}", testQueuePath);
            int result = MsmqNativeInterface.INSTANCE.MQCreateQueue(
                    null, testQueuePath, queueHandleRef
            );

            if (result == MsmqNativeInterface.MQ_OK) {
                Pointer handle = queueHandleRef.getValue();
                if (handle != null) {
                    MsmqNativeInterface.INSTANCE.MQDeleteQueue(testQueuePath);
                    logger.debug("MSMQ connection test successful - test queue created and deleted");
                }
                return true;
            } else if (result == MsmqConstants.MQ_ERROR_ACCESS_DENIED) {
                logger.debug("MSMQ connection test successful - service responding but access denied");
                return true; // Service is responding
            } else if (result == MsmqConstants.MQ_ERROR_INVALID_PARAMETER) {
                logger.error("MSMQ service responding but invalid parameter for queue path: {}", testQueuePath);
                return true;
            } else {
                logger.error("MSMQ connection test failed with error code: 0x{}", Integer.toHexString(result));
                return false;
            }

        } catch (Exception e) {
            logger.error("MSMQ connection test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Returns whether the service is currently connected.
     *
     * @return true if connected, false otherwise
     */
    @Override
    public boolean isConnected() {
        return isConnected.get();
    }

    /**
     * Gets the current connection status information.
     *
     * @return connection status details
     */
    @Override
    public ConnectionStatus getConnectionStatus() {
        ConnectionStatus status = new ConnectionStatus();
        status.setConnected(isConnected.get());
        status.setStatus(isConnected.get() ? "CONNECTED" : "DISCONNECTED");
        status.setRetryCount(0); // Reset count since we're just checking status

        if (!isConnected.get()) {
            boolean testResult = testConnection();
            if (testResult) {
                status.setStatus("AVAILABLE");
            } else {
                status.setStatus("UNAVAILABLE");
            }
        }

        return status;
    }

    /**
     * Disconnects from the MSMQ service.
     *
     * @throws MsmqException if disconnection fails
     */
    @Override
    public void disconnect() throws MsmqException {
        try {
            queueHandles.keySet().forEach(this::closeQueue);
            isConnected.set(false);
            logger.info("Disconnected from MSMQ service");
        } catch (Exception e) {
            logger.error("Error disconnecting from MSMQ service: {}", e.getMessage(), e);
            throw new MsmqException(ResponseCode.CONNECTION_ERROR,
                "Failed to disconnect from MSMQ service", e);
        }
    }

    /**
     * Reconnects to the MSMQ service by disconnecting and connecting again.
     *
     * @throws MsmqException if reconnection fails
     */
    @Override
    public void reconnect() throws MsmqException {
        logger.info("Reconnecting to MSMQ service");

        try {
            // Disconnect first if already connected
            if (isConnected.get()) {
                disconnect();
            }

            // Wait briefly before reconnecting
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MsmqException(ResponseCode.CONNECTION_ERROR, "Reconnection interrupted", e);
            }

            // Attempt to connect
            connect();

            logger.info("Successfully reconnected to MSMQ service");

        } catch (Exception e) {
            logger.error("Failed to reconnect to MSMQ service: {}", e.getMessage(), e);
            throw new MsmqException(ResponseCode.CONNECTION_ERROR,
                "Failed to reconnect to MSMQ service", e);
        }
    }

    public boolean createQueue(String queuePath) {
        try {
            if (!isConnected.get()) {
                try {
                    connect();
                } catch (MsmqException e) {
                    logger.error("Failed to connect while creating queue: {}", e.getMessage());
                    return false;
                }
            }

            String formattedPath = ".\\private$\\" + queuePath.replace("private$\\", "");
            PointerByReference queueHandleRef = new PointerByReference();
            int result = MsmqNativeInterface.INSTANCE.MQCreateQueue(
                    null, formattedPath, queueHandleRef
            );

            if (result == MsmqNativeInterface.MQ_OK) {
                logger.info("Successfully created queue: {}", formattedPath);
                return true;
            } else {
                logger.error("Failed to create queue: {}, error code: 0x{}", formattedPath, Integer.toHexString(result));
                return false;
            }

        } catch (Exception e) {
            logger.error("Error creating queue {}: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    public void closeQueue(String queuePath) {
        try {
            Pointer handle = queueHandles.get(queuePath);
            if (handle != null) {
                int result = MsmqNativeInterface.INSTANCE.MQCloseQueue(handle);
                if (result == MsmqNativeInterface.MQ_OK) {
                    queueHandles.remove(queuePath);
                    logger.debug("Successfully closed queue: {}", queuePath);
                } else {
                    logger.error("Failed to close queue: {}, error code: 0x{}", queuePath, Integer.toHexString(result));
                }
            }

        } catch (Exception e) {
            logger.error("Error closing queue {}: {}", queuePath, e.getMessage(), e);
        }
    }

    /**
     * Tests connection by trying to access the user's existing queue.
     *
     * @return true if successful
     */
    private boolean testExistingQueue() {
        try {
            logger.debug("Testing connection to existing queue: nmb_to_dse");

            // Try multiple path formats to find the working one
            String[] pathFormats = {
                ".\\private$\\nmb_to_dse",                             // MSMQ Native API format (CORRECT - from MsmqQueueManager)
                "private$\\nmb_to_dse",                                // Without . prefix
                "itr00ictl135\\private$\\nmb_to_dse",                  // Full machine path
                "itr00ictl135\\private\\nmb_to_dse",                   // Without $
                "itr00ictl135\\nmb_to_dse",                            // Direct queue
                "nmb_to_dse"                                           // Just queue name
            };

            for (String pathFormat : pathFormats) {
                logger.debug("Trying path format: {}", pathFormat);

                PointerByReference queueHandleRef = new PointerByReference();
                int result = MsmqNativeInterface.INSTANCE.MQOpenQueue(
                        pathFormat,
                        MsmqConstants.MQ_PEEK_ACCESS,
                        MsmqConstants.MQ_DENY_NONE,
                        queueHandleRef
                );

                if (result == MsmqNativeInterface.MQ_OK) {
                    Pointer handle = queueHandleRef.getValue();
                    if (handle != null) {
                        MsmqNativeInterface.INSTANCE.MQCloseQueue(handle);
                        logger.debug("Successfully connected to existing queue with path: {}", pathFormat);
                        return true;
                    } else {
                        logger.error("MSMQ queue opened but handle is null for path: {}", pathFormat);
                    }
                } else if (result == MsmqConstants.MQ_ERROR_ACCESS_DENIED) {
                    logger.debug("Successfully connected to existing queue (access denied) with path: {}", pathFormat);
                    return true;
                } else {
                    logger.debug("Failed to access existing queue with path '{}', error code: 0x{} ({})",
                            pathFormat, Integer.toHexString(result), getMsmqErrorDescription(result));
                }
            }

            logger.debug("All path formats failed for existing queue test");
            return false;

        } catch (Exception e) {
            logger.debug("Error testing existing queue: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns a description of the MSMQ error code.
     *
     * @param errorCode The MSMQ error code
     * @return A string description of the error
     */
    private String getMsmqErrorDescription(int errorCode) {
        return switch (errorCode) {
            case MsmqConstants.MQ_ERROR_ILLEGAL_QUEUE_PATHNAME -> "MQ_ERROR_ILLEGAL_QUEUE_PATHNAME - Illegal queue pathname";
            case MsmqConstants.MQ_ERROR_ACCESS_DENIED -> "MQ_ERROR_ACCESS_DENIED - Access denied";
            case MsmqConstants.MQ_ERROR_INVALID_PARAMETER -> "MQ_ERROR_INVALID_PARAMETER - Invalid parameter";
            default -> "Unknown MSMQ error";
        };
    }

    /**
     * Gets the number of retry attempts for connection.
     *
     * @return The number of retry attempts.
     */
    @Override
    public int getRetryAttempts() {
        return retryAttempts;
    }

    /**
     * Gets the host configuration for the MSMQ service.
     *
     * @return the configured host
     */
    @Override
    public String getHost() {
        return msmqHost;
    }

    /**
     * Gets the port configuration for the MSMQ service.
     *
     * @return the configured port
     */
    @Override
    public int getPort() {
        return msmqPort;
    }

    /**
     * Gets the connection timeout configuration.
     *
     * @return the configured timeout in milliseconds
     */
    @Override
    public int getTimeout() {
        return 30000; // Default timeout for native implementation
    }
}

