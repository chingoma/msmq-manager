package com.enterprise.msmq.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for default email SMTP settings.
 * Binds to email.smtp.default.* properties from application.properties.
 *
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "email.smtp.default")
public class DefaultEmailProperties {

    private String host;
    private Integer port;
    private String username;
    private String password;
    private String fromEmail;
    private String fromName;
    private Boolean useTls = true;
    private Boolean useSsl = false;
    private Integer connectionTimeout = 5000;
    private Integer readTimeout = 5000;
}
