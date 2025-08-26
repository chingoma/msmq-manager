package com.enterprise.msmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot Application class for MSMQ Manager.
 * 
 * This application provides enterprise-grade MSMQ (Microsoft Message Queuing) management
 * capabilities including queue operations, message processing, monitoring, and logging.
 * 
 * Features:
 * - MSMQ queue read/write operations
 * - Connection and session management
 * - Message parsing and transformation
 * - API monitoring and metrics
 * - Comprehensive logging and error handling
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableAspectJAutoProxy
@EnableAsync
@EnableScheduling
public class MsmqManagerApplication {

    /**
     * Main method to bootstrap the Spring Boot application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MsmqManagerApplication.class, args);
    }
}
