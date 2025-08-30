package com.enterprise.msmq.factory;

import com.enterprise.msmq.enums.ConnectionType;
import com.enterprise.msmq.service.contracts.IMsmqQueueManager;
import com.enterprise.msmq.service.NativeMsmqQueueManager;
import com.enterprise.msmq.service.PowerShellMsmqQueueManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory for creating MSMQ Queue Manager instances.
 * <p>
 * This factory creates the appropriate queue manager implementation
 * based on the configured connection type, maintaining consistency
 * with the connection factory pattern.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MsmqQueueManagerFactory {

    private final PowerShellMsmqQueueManager powershellQueueManager;
    private final NativeMsmqQueueManager nativeQueueManager;

    /**
     * Connection type configuration for MSMQ operations.
     */
    @Value("${msmq.connection.type:POWERSHELL}")
    private ConnectionType connectionType;

    /**
     * Creates the appropriate queue manager based on configuration.
     * 
     * @return the configured queue manager implementation
     */
    public IMsmqQueueManager createQueueManager() {
        log.debug("Creating queue manager for connection type: {}", connectionType);

        return switch (connectionType) {
            case POWERSHELL -> {
                log.debug("Using PowerShell-based queue manager");
                yield powershellQueueManager;
            }
            case NATIVE -> {
                log.debug("Using Native MSMQ queue manager");
                yield nativeQueueManager;
            }
        };
    }

    /**
     * Creates a queue manager for a specific connection type.
     * 
     * @param connectionType the specific connection type to use
     * @return the requested queue manager implementation
     */
    public IMsmqQueueManager createQueueManager(ConnectionType connectionType) {
        log.debug("Creating queue manager for specific connection type: {}", connectionType);

        return switch (connectionType) {
            case POWERSHELL -> {
                log.debug("Using PowerShell-based queue manager");
                yield powershellQueueManager;
            }
            case NATIVE -> {
                log.debug("Using Native MSMQ queue manager");
                yield nativeQueueManager;
            }
        };
    }

    /**
     * Gets the currently configured connection type.
     * 
     * @return the configured connection type
     */
    public ConnectionType getConfiguredConnectionType() {
        return connectionType;
    }
}
