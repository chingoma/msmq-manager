package com.enterprise.msmq.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for Swagger UI documentation.
 * Provides comprehensive API documentation for the MSMQ Manager application.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configures the OpenAPI documentation with application information and server details.
     * 
     * @return the configured OpenAPI object
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MSMQ Manager API")
                .description("Enterprise-grade Microsoft Message Queuing (MSMQ) Management Application API. " +
                           "Provides comprehensive management capabilities for MSMQ queues, messages, " +
                           "connections, and monitoring.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Enterprise MSMQ Team")
                    .email("msmq-team@enterprise.com")
                    .url("https://enterprise.com/msmq"))
                .license(new License()
                    .name("Enterprise License")
                    .url("https://enterprise.com/license")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8081/api/v1")
                    .description("Development Server"),
                new Server()
                    .url("https://staging-msmq-server:8081/api/v1")
                    .description("Staging Server"),
                new Server()
                    .url("https://prod-msmq-server:8081/api/v1")
                    .description("Production Server")
            ));
    }
}
