package com.enterprise.msmq.factory;

import com.enterprise.msmq.enums.ConnectionType;
import com.enterprise.msmq.service.contracts.IMsmqConnectionManager;
import com.enterprise.msmq.service.NativeMsmqConnectionService;
import com.enterprise.msmq.service.PowerShellMsmqConnectionService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory for creating MSMQ connection instances based on configuration.
 */
@Component
public class MsmqConnectionFactory {

    /**
     * -- GETTER --
     *  Gets the currently configured connection type.
     *
     */
    @Getter
    @Value("${msmq.connection.type:POWERSHELL}")
    private ConnectionType connectionType;

    private final NativeMsmqConnectionService nativeMsmqConnectionService;
    private final PowerShellMsmqConnectionService powerShellMsmqConnectionService;

    public MsmqConnectionFactory(
            NativeMsmqConnectionService nativeMsmqConnectionService,
            PowerShellMsmqConnectionService powerShellMsmqConnectionService) {
        this.nativeMsmqConnectionService = nativeMsmqConnectionService;
        this.powerShellMsmqConnectionService = powerShellMsmqConnectionService;
    }

    /**
     * Creates an MSMQ connection manager based on the configured connection type.
     *
     * @return the appropriate connection manager implementation
     */
    public IMsmqConnectionManager createConnectionManager() {
        return switch (connectionType) {
            case NATIVE -> nativeMsmqConnectionService;
            case POWERSHELL -> powerShellMsmqConnectionService;
        };
    }

}
