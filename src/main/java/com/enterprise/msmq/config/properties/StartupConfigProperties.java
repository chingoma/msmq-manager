package com.enterprise.msmq.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for startup behavior.
 * Binds to startup.config.* properties from application.properties.
 *
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "startup.config")
public class StartupConfigProperties {

    private Boolean initializeDefaults = true;
    private Boolean updateExisting = false;
    private Boolean skipIfExists = true;
}
