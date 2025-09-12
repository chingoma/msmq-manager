package com.enterprise.msmq.service;

import com.enterprise.msmq.entity.EmailConfiguration;
import com.enterprise.msmq.repository.EmailConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing email configurations.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmailConfigurationService {
    
    private final EmailConfigurationRepository emailConfigRepository;
    
    /**
     * Save or update email configuration.
     * 
     * @param config the configuration to save
     * @return saved configuration
     */
    public EmailConfiguration saveConfiguration(EmailConfiguration config) {
        // If this is set as default, unset other defaults
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            emailConfigRepository.clearDefaultFlags();
        }
        
        EmailConfiguration saved = emailConfigRepository.save(config);
        log.info("Saved email configuration: {}", saved.getConfigName());
        return saved;
    }
    
    /**
     * Get email configuration by ID.
     * 
     * @param id the configuration ID
     * @return configuration if found
     */
    public Optional<EmailConfiguration> getConfigurationById(Long id) {
        return emailConfigRepository.findById(id);
    }
    
    /**
     * Get email configuration by name.
     * 
     * @param configName the configuration name
     * @return configuration if found
     */
    public Optional<EmailConfiguration> getConfigurationByName(String configName) {
        return emailConfigRepository.findByConfigName(configName);
    }
    
    /**
     * Get default email configuration.
     * 
     * @return default configuration if found
     */
    public EmailConfiguration getDefaultConfiguration() {
        return emailConfigRepository.findByIsDefaultTrueAndIsActiveTrue()
            .orElse(null);
    }
    
    /**
     * Get all active email configurations.
     * 
     * @return list of active configurations
     */
    public List<EmailConfiguration> getActiveConfigurations() {
        return emailConfigRepository.findByIsActiveTrue();
    }
    
    /**
     * Get all email configurations.
     * 
     * @return list of all configurations
     */
    public List<EmailConfiguration> getAllConfigurations() {
        return emailConfigRepository.findAll();
    }
    
    /**
     * Delete email configuration.
     * 
     * @param id the configuration ID
     */
    public void deleteConfiguration(Long id) {
        Optional<EmailConfiguration> config = emailConfigRepository.findById(id);
        if (config.isPresent()) {
            emailConfigRepository.deleteById(id);
            log.info("Deleted email configuration: {}", config.get().getConfigName());
        }
    }
    
    /**
     * Activate email configuration.
     * 
     * @param id the configuration ID
     */
    public void activateConfiguration(Long id) {
        emailConfigRepository.findById(id).ifPresent(config -> {
            config.setIsActive(true);
            emailConfigRepository.save(config);
            log.info("Activated email configuration: {}", config.getConfigName());
        });
    }
    
    /**
     * Deactivate email configuration.
     * 
     * @param id the configuration ID
     */
    public void deactivateConfiguration(Long id) {
        emailConfigRepository.findById(id).ifPresent(config -> {
            config.setIsActive(false);
            emailConfigRepository.save(config);
            log.info("Deactivated email configuration: {}", config.getConfigName());
        });
    }
    
    /**
     * Set configuration as default.
     * 
     * @param id the configuration ID
     */
    public void setAsDefault(Long id) {
        // Clear other default flags
        emailConfigRepository.clearDefaultFlags();
        
        // Set this one as default
        emailConfigRepository.findById(id).ifPresent(config -> {
            config.setIsDefault(true);
            emailConfigRepository.save(config);
            log.info("Set email configuration as default: {}", config.getConfigName());
        });
    }
    
    /**
     * Test email configuration by sending a test email.
     * 
     * @param id the configuration ID
     * @param testRecipient the test recipient email
     * @return true if test successful
     */
    public boolean testConfiguration(Long id, String testRecipient) {
        Optional<EmailConfiguration> config = emailConfigRepository.findById(id);
        if (config.isPresent()) {
            // This would call the email notification service to send test email
            log.info("Testing email configuration: {} with recipient: {}", 
                config.get().getConfigName(), testRecipient);
            return true; // Placeholder - actual test would be implemented
        }
        return false;
    }
    
    /**
     * Count active email configurations.
     * 
     * @return count of active configurations
     */
    public long countActiveConfigurations() {
        return emailConfigRepository.countByIsActiveTrue();
    }
    
    /**
     * Validate email configuration.
     * 
     * @param config the configuration to validate
     * @return validation result
     */
    public boolean validateConfiguration(EmailConfiguration config) {
        if (config.getSmtpHost() == null || config.getSmtpHost().trim().isEmpty()) {
            return false;
        }
        
        if (config.getSmtpPort() == null || config.getSmtpPort() <= 0) {
            return false;
        }
        
        if (config.getFromEmail() == null || config.getFromEmail().trim().isEmpty()) {
            return false;
        }
        
        return true;
    }
}
