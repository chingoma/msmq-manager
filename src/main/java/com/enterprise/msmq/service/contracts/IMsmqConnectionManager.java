package com.enterprise.msmq.service.contracts;

import com.enterprise.msmq.exception.MsmqException;
import com.enterprise.msmq.dto.ConnectionStatus;

/**
 * Interface defining MSMQ connection management operations.
 */
public interface IMsmqConnectionManager {

    /**
     * Gets the host configuration for the MSMQ service.
     *
     * @return the configured host
     */
    String getHost();

    /**
     * Gets the port configuration for the MSMQ service.
     *
     * @return the configured port
     */
    int getPort();

    /**
     * Gets the timeout configuration in milliseconds.
     *
     * @return the configured timeout
     */
    int getTimeout();

    /**
     * Gets the number of retry attempts configured.
     *
     * @return the configured retry attempts
     */
    int getRetryAttempts();

    /**
     * Connects to the MSMQ service.
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
     * Reconnects to the MSMQ service.
     *
     * @throws MsmqException if reconnection fails
     */
    void reconnect() throws MsmqException;

    /**
     * Checks if the connection is currently active.
     *
     * @return true if connected, false otherwise
     */
    boolean isConnected();

    /**
     * Gets the current connection status.
     *
     * @return connection status information
     */
    ConnectionStatus getConnectionStatus();
}
