package com.enterprise.msmq.util;

import com.enterprise.msmq.platform.windows.MsmqNativeInterface;
import com.enterprise.msmq.platform.windows.MsmqConstants;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class RealMsmqConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(RealMsmqConnectionManager.class);

    @Value("${msmq.connection.host:localhost}")
    private String msmqHost;

    @Value("${msmq.connection.port:1801}")
    private int msmqPort;

    @Value("${msmq.connection.timeout:30000}")
    private int connectionTimeout;

    @Value("${msmq.connection.retry-attempts:3}")
    private int retryAttempts;

    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, Pointer> queueHandles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastActivity = new ConcurrentHashMap<>();

    public boolean connect() {
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                logger.info("Connection attempt {} of {} to MSMQ service at {}:{}", attempt, retryAttempts, msmqHost, msmqPort);
                if (testConnection()) {
                    isConnected.set(true);
                    logger.info("Successfully connected to MSMQ service at {}:{}", msmqHost, msmqPort);
                    return true;
                }
            } catch (Exception e) {
                logger.error("Attempt {} failed: {}", attempt, e.getMessage());
                if (attempt == retryAttempts) {
                    logger.error("Failed to establish connection to MSMQ service at {}:{}", msmqHost, msmqPort);
                    return false;
                }
            }
        }
        return false;
    }

    private boolean testConnection() {
        try {
            logger.debug("Testing MSMQ connection...");

            // Test 1: Verify native library can be loaded
            try {
                MsmqNativeInterface.INSTANCE.getClass();
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
                    return true;
                } else {
                    logger.error("MSMQ queue created but handle is null for path: {}", testQueuePath);
                    logger.debug("Falling back to test existing queue");
                    return testExistingQueue();
                }
            } else if (result == MsmqConstants.MQ_ERROR_ACCESS_DENIED) {
                logger.debug("MSMQ connection test successful - service responding but access denied");
                return true; // Service is responding
            } else if (result == MsmqConstants.MQ_ERROR_INVALID_PARAMETER) {
                logger.error("MSMQ service responding but invalid parameter for queue path: {}", testQueuePath);
                logger.debug("Falling back to test existing queue");
                return testExistingQueue();
            } else {
                logger.error("MSMQ connection test failed with error code: 0x{}", Integer.toHexString(result));
                logger.debug("Falling back to test existing queue");
                return testExistingQueue();
            }

        } catch (Exception e) {
            logger.error("MSMQ connection test failed: {}", e.getMessage(), e);
            logger.debug("Falling back to test existing queue");
            return testExistingQueue();
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
        status.setQueueHandleCount(queueHandles.size());
        status.setLastActivity(lastActivity.isEmpty() ? 0L :
                lastActivity.values().stream().mapToLong(Long::longValue).max().orElse(0L));

        if (!isConnected.get()) {
            status.setConnectionTestResult(testConnection());
        } else {
            status.setConnectionTestResult(true);
        }

        return status;
    }

    public static class ConnectionStatus {
        private boolean connected;
        private String host;
        private int port;
        private int timeout;
        private int retryAttempts;
        private int queueHandleCount;
        private long lastActivity;
        private boolean connectionTestResult;

        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }

        public int getRetryAttempts() { return retryAttempts; }
        public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }

        public int getQueueHandleCount() { return queueHandleCount; }
        public void setQueueHandleCount(int queueHandleCount) { this.queueHandleCount = queueHandleCount; }

        public long getLastActivity() { return lastActivity; }
        public void setLastActivity(long lastActivity) { this.lastActivity = lastActivity; }

        public boolean isConnectionTestResult() { return connectionTestResult; }
        public void setConnectionTestResult(boolean connectionTestResult) { this.connectionTestResult = connectionTestResult; }
    }

    public void disconnect() {
        try {
            queueHandles.keySet().forEach(this::closeQueue);
            isConnected.set(false);
            logger.info("Disconnected from MSMQ service");
        } catch (Exception e) {
            logger.error("Error disconnecting from MSMQ service: {}", e.getMessage(), e);
        }
    }

    public boolean createQueue(String queuePath) {
        try {
            if (!isConnected.get()) {
                if (!connect()) {
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

    public boolean deleteQueue(String queuePath) {
        try {
            closeQueue(queuePath);

            String formattedPath = ".\\private$\\" + queuePath.replace("private$\\", "");
            int result = MsmqNativeInterface.INSTANCE.MQDeleteQueue(formattedPath);

            if (result == MsmqNativeInterface.MQ_OK) {
                logger.info("Successfully deleted queue: {}", formattedPath);
                return true;
            } else {
                logger.error("Failed to delete queue: {}, error code: 0x{}", formattedPath, Integer.toHexString(result));
                return false;
            }

        } catch (Exception e) {
            logger.error("Error deleting queue {}: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    public Pointer openQueue(String queuePath, int access, int shareMode) {
        try {
            if (!isConnected.get()) {
                logger.warn("MSMQ not connected, attempting to reconnect");
                if (!connect()) {
                    return null;
                }
            }

            String formattedPath = ".\\private$\\" + queuePath.replace("private$\\", "");
            PointerByReference queueHandleRef = new PointerByReference();
            int result = MsmqNativeInterface.INSTANCE.MQOpenQueue(
                    formattedPath, access, shareMode, queueHandleRef
            );

            if (result == MsmqNativeInterface.MQ_OK) {
                Pointer handle = queueHandleRef.getValue();
                queueHandles.put(formattedPath, handle);
                lastActivity.put(formattedPath, System.currentTimeMillis());
                logger.debug("Successfully opened queue: {} with handle: {}", formattedPath, handle);
                return handle;
            } else {
                logger.error("Failed to open queue: {}, error code: 0x{}", formattedPath, Integer.toHexString(result));
                return null;
            }

        } catch (Exception e) {
            logger.error("Error opening queue {}: {}", queuePath, e.getMessage(), e);
            return null;
        }
    }

    public void closeQueue(String queuePath) {
        try {
            Pointer handle = queueHandles.get(queuePath);
            if (handle != null) {
                int result = MsmqNativeInterface.INSTANCE.MQCloseQueue(handle);
                if (result == MsmqNativeInterface.MQ_OK) {
                    queueHandles.remove(queuePath);
                    lastActivity.remove(queuePath);
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
        switch (errorCode) {
            case MsmqConstants.MQ_ERROR_ILLEGAL_QUEUE_PATHNAME:
                return "MQ_ERROR_ILLEGAL_QUEUE_PATHNAME - Illegal queue pathname";
            case MsmqConstants.MQ_ERROR_ACCESS_DENIED:
                return "MQ_ERROR_ACCESS_DENIED - Access denied";
            case MsmqConstants.MQ_ERROR_INVALID_PARAMETER:
                return "MQ_ERROR_INVALID_PARAMETER - Invalid parameter";
            default:
                return "Unknown MSMQ error";
        }
    }
}