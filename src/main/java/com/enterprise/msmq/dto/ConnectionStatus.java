package com.enterprise.msmq.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for MSMQ connection status information.
 */
@Data
@NoArgsConstructor
public class ConnectionStatus {
    private boolean connected;
    private String status;
    private String errorMessage;
    private long lastConnected;
    private long lastDisconnected;
    private long lastError;
    private String host;
    private int port;
    private int retryCount;
    private String type;
    private String version;
}
