package com.enterprise.msmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for MSMQ remote queue operations.
 * 
 * This class binds to the msmq.remote.* properties in application.properties
 * and provides type-safe access to remote MSMQ configuration settings.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
@ConfigurationProperties(prefix = "msmq.remote")
@Data
public class MsmqRemoteProperties {
    
    /**
     * Whether remote queue operations are enabled.
     * When true, getAllQueues() will fetch from remote machine.
     * When false, getAllQueues() will fetch from local machine.
     */
    private boolean enabled = false;
    
    /**
     * The remote MSMQ server hostname or IP address.
     * Used for remote queue operations when enabled is true.
     */
    private String host = "localhost";
    
    /**
     * The remote MSMQ server port.
     * Default MSMQ port is 1801.
     */
    private int port = 1801;
    
    /**
     * Connection timeout for remote operations in milliseconds.
     * Default is 30000ms (30 seconds).
     */
    private long timeout = 30000;
    
    /**
     * Number of retry attempts for remote operations.
     * Default is 3 attempts.
     */
    private int retryAttempts = 3;
    
    /**
     * Whether to use TCP protocol for remote connections.
     * When true, uses TCP: format for remote queue paths.
     * When false, uses OS: format (native MSMQ protocol).
     */
    private boolean useTcp = true;
    
    /**
     * Comma-separated list of queue names to check on remote server.
     * If empty, will check common queue names.
     */
    private String queueNames = "securities-settlement-queue,testqueue,orders-queue,settlements-queue";
    
    /**
     * Gets the full remote server address in the format host:port.
     * 
     * @return the remote server address
     */
    public String getRemoteAddress() {
        return host + ":" + port;
    }
    
    /**
     * Gets the remote queue path prefix based on protocol setting.
     * 
     * @return the appropriate path prefix (TCP: or OS:)
     */
    public String getPathPrefix() {
        return useTcp ? "TCP:" : "OS:";
    }
}
