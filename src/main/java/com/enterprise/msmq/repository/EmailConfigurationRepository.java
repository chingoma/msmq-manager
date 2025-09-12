package com.enterprise.msmq.repository;

import com.enterprise.msmq.entity.EmailConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for email configurations.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Repository
public interface EmailConfigurationRepository extends JpaRepository<EmailConfiguration, Long> {
    
    /**
     * Find email configuration by configuration name.
     * 
     * @param configName the configuration name
     * @return email configuration if found
     */
    Optional<EmailConfiguration> findByConfigName(String configName);
    
    /**
     * Find default and active email configuration.
     * 
     * @return default email configuration if found
     */
    Optional<EmailConfiguration> findByIsDefaultTrueAndIsActiveTrue();
    
    /**
     * Find all active email configurations.
     * 
     * @return list of active email configurations
     */
    java.util.List<EmailConfiguration> findByIsActiveTrue();
    
    /**
     * Count active email configurations.
     * 
     * @return count of active configurations
     */
    long countByIsActiveTrue();
    
    /**
     * Clear default flags from all configurations.
     */
    @Modifying
    @Query("UPDATE EmailConfiguration e SET e.isDefault = false")
    void clearDefaultFlags();
    
    /**
     * Find email configurations by SMTP host.
     * 
     * @param smtpHost the SMTP host
     * @return list of configurations with the specified host
     */
    java.util.List<EmailConfiguration> findBySmtpHost(String smtpHost);
    
    /**
     * Find email configurations by from email.
     * 
     * @param fromEmail the from email address
     * @return list of configurations with the specified from email
     */
    java.util.List<EmailConfiguration> findByFromEmail(String fromEmail);
    
    /**
     * Check if configuration name exists.
     * 
     * @param configName the configuration name
     * @return true if exists
     */
    boolean existsByConfigName(String configName);
}
