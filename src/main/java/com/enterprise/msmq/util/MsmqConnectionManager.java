package com.enterprise.msmq.util;

import com.enterprise.msmq.dto.ConnectionStatus;
import com.enterprise.msmq.enums.ResponseCode;
import com.enterprise.msmq.exception.MsmqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages MSMQ connection lifecycle and status.
 * 
 * This class handles connection establishment, maintenance, and monitoring
 * for the MSMQ service. It provides connection status information and
 * manages connection retry logic.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class MsmqConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(MsmqConnectionManager.class);

    @Autowired
    private PowerShellMsmqConnectionManager powerShellMsmqConnectionManager;
    
    private final String host;
    private final int port;
    private final long timeout;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private LocalDateTime connectedAt;
    private LocalDateTime disconnectedAt;
    private int connectionAttempts = 0;
    private ConnectionStatus connectionStatus;

    /**
     * Default constructor.
     * Initializes connection manager with default settings.
     */
    public MsmqConnectionManager() {
        this.host = "localhost";
        this.port = 1801;
        this.timeout = 30000L;
        this.connectionStatus = new ConnectionStatus(false, "DISCONNECTED", host, port);
    }

    /**
     * Constructor with custom connection parameters.
     * 
     * @param host the MSMQ host
     * @param port the MSMQ port
     * @param timeout the connection timeout in milliseconds
     */
    public MsmqConnectionManager(String host, int port, long timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.connectionStatus = new ConnectionStatus(false, "DISCONNECTED", host, port);
    }

    /**
     * Establishes a connection to the MSMQ service using real MSMQ implementation.
     * 
     * @throws MsmqException if connection fails
     */
    public void connect() throws MsmqException {
        logger.info("Establishing connection to MSMQ service at {}:{}", host, port);
        
        try {
            // Update connection status
            connectionStatus.setStatus("CONNECTING");
            connectionAttempts++;
            connectionStatus.setRetryCount(connectionAttempts);
            
            // Use PowerShell MSMQ connection manager
            boolean connected = powerShellMsmqConnectionManager.connect();
            
            if (connected) {
                // Update connection status
                this.connected.set(true);
                connectedAt = LocalDateTime.now();
                connectionStatus.setStatus("CONNECTED");
                connectionStatus.setLastConnected(System.currentTimeMillis());
                connectionStatus.setConnected(true);
                
                logger.info("Successfully connected to MSMQ service at {}:{}", host, port);
            } else {
                connectionStatus.setStatus("ERROR");
                connectionStatus.setErrorMessage("Real MSMQ connection failed");
                this.connected.set(false);
                logger.error("Failed to connect to MSMQ service at {}:{}", host, port);
                throw new MsmqException(ResponseCode.CONNECTION_ERROR, "Real MSMQ connection failed");
            }
            
        } catch (Exception e) {
            connectionStatus.setStatus("ERROR");
            connectionStatus.setErrorMessage("Connection failed: " + e.getMessage());
            connected.set(false);
            logger.error("Failed to connect to MSMQ service at {}:{}", host, port, e);
            throw new MsmqException(ResponseCode.CONNECTION_ERROR, "Failed to connect to MSMQ service", e);
        }
    }

    /**
     * Disconnects from the MSMQ service using real MSMQ implementation.
     * 
     * @throws MsmqException if disconnection fails
     */
    public void disconnect() throws MsmqException {
        logger.info("Disconnecting from MSMQ service");
        
        try {
            // Update connection status
            connectionStatus.setStatus("DISCONNECTING");
            
            // Use PowerShell MSMQ connection manager
            powerShellMsmqConnectionManager.disconnect();
            
            // Update connection status
            connected.set(false);
            disconnectedAt = LocalDateTime.now();
            connectionStatus.setStatus("DISCONNECTED");
            connectionStatus.setLastDisconnected(System.currentTimeMillis());
            connectionStatus.setConnected(false);
            
            logger.info("Successfully disconnected from MSMQ service");
            
        } catch (Exception e) {
            connectionStatus.setStatus("ERROR");
            connectionStatus.setErrorMessage("Disconnection failed: " + e.getMessage());
            logger.error("Failed to disconnect from MSMQ service", e);
            throw new MsmqException(ResponseCode.CONNECTION_ERROR, "Failed to disconnect from MSMQ service", e);
        }
    }

    /**
     * Reconnects to the MSMQ service.
     * 
     * @throws MsmqException if reconnection fails
     */
    public void reconnect() throws MsmqException {
        logger.info("Reconnecting to MSMQ service");
        
        try {
            // Disconnect first if connected
            if (connected.get()) {
                disconnect();
            }
            
            // Wait a bit before reconnecting
            Thread.sleep(1000);
            
            // Attempt to connect
            connect();
            
            logger.info("Successfully reconnected to MSMQ service");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MsmqException(ResponseCode.CONNECTION_ERROR, "Reconnection interrupted", e);
        } catch (Exception e) {
            logger.error("Failed to reconnect to MSMQ service", e);
            throw new MsmqException(ResponseCode.CONNECTION_ERROR, "Failed to reconnect to MSMQ service", e);
        }
    }

    /**
     * Checks if the connection is currently active.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected.get() && powerShellMsmqConnectionManager.isConnected();
    }

    /**
     * Gets the current connection status.
     * 
     * @return connection status information
     */
    public ConnectionStatus getConnectionStatus() {
        // Update status based on current connection state
        if (connected.get()) {
            connectionStatus.setStatus("CONNECTED");
        } else {
            connectionStatus.setStatus("DISCONNECTED");
        }
        return connectionStatus;
    }

    /**
     * Gets the connection host.
     * 
     * @return the host address
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the connection port.
     * 
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the connection timeout.
     * 
     * @return the timeout in milliseconds
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Gets the number of connection attempts.
     * 
     * @return the attempt count
     */
    public int getConnectionAttempts() {
        return connectionAttempts;
    }

    /**
     * Gets the timestamp when the connection was established.
     * 
     * @return the connection timestamp
     */
    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }

    /**
     * Gets the timestamp when the connection was disconnected.
     * 
     * @return the disconnection timestamp
     */
    public LocalDateTime getDisconnectedAt() {
        return disconnectedAt;
    }

    /**
     * Resets connection statistics.
     */
    public void resetConnectionStats() {
        connectionAttempts = 0;
        connectionStatus.setRetryCount(0);
        connectionStatus.setErrorMessage(null);
    }
    
    /**
     * Gets the PowerShell MSMQ connection manager for direct operations.
     * 
     * @return the PowerShell MSMQ connection manager
     */
    public PowerShellMsmqConnectionManager getPowerShellMsmqConnectionManager() {
        return powerShellMsmqConnectionManager;
    }
}
