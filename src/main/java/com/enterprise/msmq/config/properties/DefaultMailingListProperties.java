package com.enterprise.msmq.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for default mailing list settings.
 * Binds to mailing.list.default.* properties from application.properties.
 *
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "mailing.list.default")
public class DefaultMailingListProperties {

    private String name;
    private String description;
    private List<String> recipients;
    private List<String> alertSeverities;
    private List<String> alertTypes;
}
