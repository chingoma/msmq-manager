package com.enterprise.msmq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Application configuration class for MSMQ Manager.
 * Provides beans and configuration for the application.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Configuration
@EnableAsync
@EnableScheduling
public class ApplicationConfig {

    /**
     * Configures the primary ObjectMapper with proper serialization settings.
     * 
     * @return the configured ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Enable pretty printing for development
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Handle Java 8 time types
        mapper.registerModule(new JavaTimeModule());
        
        // Disable writing dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Fail on unknown properties
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        return mapper;
    }

    /**
     * Configures the async task executor for background operations.
     * 
     * @return the configured task executor
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("MSMQ-Async-");
        executor.initialize();
        return executor;
    }

    /**
     * Configures the MSMQ operation task executor for queue operations.
     * 
     * @return the configured task executor
     */
    @Bean("msmqTaskExecutor")
    public Executor msmqTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("MSMQ-Queue-");
        executor.initialize();
        return executor;
    }
}
