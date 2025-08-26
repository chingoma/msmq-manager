package com.enterprise.msmq.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing MSMQ connection status.
 * Contains information about the current connection state and health.
 *
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
public class ConnectionStatus {

    private boolean connected;
    private String status;
    private String host;
    private int port;
    private long lastConnected;
    private long lastDisconnected;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime timestamp;

    /**
     * Default constructor.
     */
    public ConnectionStatus() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor with connection details.
     *
     * @param connected whether the connection is active
     * @param status the connection status
     * @param host the MSMQ host
     * @param port the MSMQ port
     */
    public ConnectionStatus(boolean connected, String status, String host, int port) {
        this();
        this.connected = connected;
        this.status = status;
        this.host = host;
        this.port = port;
    }

    // Getters and Setters
    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getLastConnected() {
        return lastConnected;
    }

    public void setLastConnected(long lastConnected) {
        this.lastConnected = lastConnected;
    }

    public long getLastDisconnected() {
        return lastDisconnected;
    }

    public void setLastDisconnected(long lastDisconnected) {
        this.lastDisconnected = lastDisconnected;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ConnectionStatus{" +
                "connected=" + connected +
                ", status='" + status + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", timestamp=" + timestamp +
                '}';
    }
}
