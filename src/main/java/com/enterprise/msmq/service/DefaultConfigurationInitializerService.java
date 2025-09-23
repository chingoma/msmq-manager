package com.enterprise.msmq.service;

import com.enterprise.msmq.config.properties.DefaultEmailProperties;
import com.enterprise.msmq.config.properties.DefaultMailingListProperties;
import com.enterprise.msmq.config.properties.StartupConfigProperties;
import com.enterprise.msmq.entity.EmailConfiguration;
import com.enterprise.msmq.entity.MailingList;
import com.enterprise.msmq.repository.EmailConfigurationRepository;
import com.enterprise.msmq.repository.MailingListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for initializing default email configurations and mailing lists during startup.
 * This service creates default configurations if they don't exist, based on application properties.
 *
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultConfigurationInitializerService {

    private final EmailConfigurationRepository emailConfigRepository;
    private final MailingListRepository mailingListRepository;
    private final DefaultEmailProperties defaultEmailProperties;
    private final DefaultMailingListProperties defaultMailingListProperties;
    private final StartupConfigProperties startupConfigProperties;

    /**
     * Initializes default email configuration and mailing lists.
     * Called during application startup.
     */
    @Transactional
    public void initializeDefaultConfigurations() {
        if (!startupConfigProperties.getInitializeDefaults()) {
            log.info("Default configuration initialization is disabled");
            return;
        }

        log.info("🚀 Starting default configuration initialization...");

        try {
            initializeDefaultEmailConfiguration();
            initializeDefaultMailingList();
            log.info("✅ Default configuration initialization completed successfully");
        } catch (Exception e) {
            log.error("❌ Failed to initialize default configurations", e);
            throw new RuntimeException("Failed to initialize default configurations", e);
        }
    }

    /**
     * Initializes the default email configuration if it doesn't exist.
     */
    private void initializeDefaultEmailConfiguration() {
        log.info("Initializing default email configuration...");

        if (startupConfigProperties.getSkipIfExists() && emailConfigRepository.existsByIsDefaultTrue()) {
            log.info("Default email configuration already exists, skipping initialization");
            return;
        }

        if (!validateEmailProperties()) {
            log.warn("Default email properties are not properly configured, skipping email configuration initialization");
            return;
        }

        // Check if we need to update existing or create new
        EmailConfiguration existingDefault = emailConfigRepository.findByIsDefaultTrue().orElse(null);

        if (existingDefault != null && !startupConfigProperties.getUpdateExisting()) {
            log.info("Default email configuration exists and update is disabled, skipping");
            return;
        }

        EmailConfiguration emailConfig = existingDefault != null ? existingDefault : new EmailConfiguration();

        // Set configuration values from properties
        emailConfig.setConfigName("Default System Configuration");
        emailConfig.setSmtpHost(defaultEmailProperties.getHost());
        emailConfig.setSmtpPort(defaultEmailProperties.getPort());
        emailConfig.setUsername(defaultEmailProperties.getUsername());
        emailConfig.setPassword(defaultEmailProperties.getPassword());
        emailConfig.setFromEmail(defaultEmailProperties.getFromEmail());
        emailConfig.setFromName(defaultEmailProperties.getFromName());
        emailConfig.setUseTls(defaultEmailProperties.getUseTls());
        emailConfig.setUseSsl(defaultEmailProperties.getUseSsl());
        emailConfig.setConnectionTimeout(defaultEmailProperties.getConnectionTimeout());
        emailConfig.setReadTimeout(defaultEmailProperties.getReadTimeout());
        emailConfig.setIsActive(true);
        emailConfig.setIsDefault(true);
        emailConfig.setCreatedBy("SYSTEM_STARTUP");
        emailConfig.setUpdatedBy("SYSTEM_STARTUP");

        emailConfigRepository.save(emailConfig);

        String action = existingDefault != null ? "updated" : "created";
        log.info("✅ Default email configuration {} successfully", action);
    }

    /**
     * Initializes the default mailing list if it doesn't exist.
     */
    private void initializeDefaultMailingList() {
        log.info("Initializing default mailing list...");

        String listName = defaultMailingListProperties.getName();
        if (!StringUtils.hasText(listName)) {
            log.warn("Default mailing list name is not configured, skipping mailing list initialization");
            return;
        }

        if (startupConfigProperties.getSkipIfExists() && mailingListRepository.existsByListName(listName)) {
            log.info("Default mailing list '{}' already exists, skipping initialization", listName);
            return;
        }

        // Check if we need to update existing or create new
        MailingList existingList = mailingListRepository.findByListName(listName).orElse(null);

        if (existingList != null && !startupConfigProperties.getUpdateExisting()) {
            log.info("Default mailing list '{}' exists and update is disabled, skipping", listName);
            return;
        }

        MailingList mailingList = existingList != null ? existingList : new MailingList();

        // Set mailing list values from properties
        mailingList.setListName(listName);
        mailingList.setDescription(defaultMailingListProperties.getDescription());
        mailingList.setIsActive(true);
        mailingList.setCreatedBy("SYSTEM_STARTUP");
        mailingList.setUpdatedBy("SYSTEM_STARTUP");

        // Set recipients
        if (defaultMailingListProperties.getRecipients() != null && !defaultMailingListProperties.getRecipients().isEmpty()) {
            Set<String> recipients = defaultMailingListProperties.getRecipients().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
            mailingList.setRecipients(recipients);
            log.info("Added {} recipients to default mailing list", recipients.size());
        }

        // Set alert severities
        if (defaultMailingListProperties.getAlertSeverities() != null) {
            Set<MailingList.AlertSeverity> severities = defaultMailingListProperties.getAlertSeverities().stream()
                .filter(StringUtils::hasText)
                .map(severity -> {
                    try {
                        return MailingList.AlertSeverity.valueOf(severity.trim().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid alert severity '{}', ignoring", severity);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
            mailingList.setAlertSeverities(severities);
            log.info("Added {} alert severities to default mailing list: {}", severities.size(), severities);
        }

        // Set alert types
        if (defaultMailingListProperties.getAlertTypes() != null) {
            Set<MailingList.AlertType> alertTypes = defaultMailingListProperties.getAlertTypes().stream()
                .filter(StringUtils::hasText)
                .map(type -> {
                    try {
                        return MailingList.AlertType.valueOf(type.trim().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid alert type '{}', ignoring", type);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
            mailingList.setAlertTypes(alertTypes);
            log.info("Added {} alert types to default mailing list: {}", alertTypes.size(), alertTypes);
        }

        mailingListRepository.save(mailingList);

        String action = existingList != null ? "updated" : "created";
        log.info("✅ Default mailing list '{}' {} successfully with {} recipients",
                listName, action, mailingList.getRecipients().size());
    }

    /**
     * Validates that required email properties are configured.
     */
    private boolean validateEmailProperties() {
        if (!StringUtils.hasText(defaultEmailProperties.getHost())) {
            log.warn("Email SMTP host is not configured");
            return false;
        }

        if (defaultEmailProperties.getPort() == null) {
            log.warn("Email SMTP port is not configured");
            return false;
        }

        if (!StringUtils.hasText(defaultEmailProperties.getFromEmail())) {
            log.warn("Email from address is not configured");
            return false;
        }

        return true;
    }

    /**
     * Gets initialization status information.
     */
    public InitializationStatus getInitializationStatus() {
        InitializationStatus status = new InitializationStatus();

        status.setInitializeDefaultsEnabled(startupConfigProperties.getInitializeDefaults());
        status.setUpdateExistingEnabled(startupConfigProperties.getUpdateExisting());
        status.setSkipIfExistsEnabled(startupConfigProperties.getSkipIfExists());
        status.setDefaultEmailConfigExists(emailConfigRepository.existsByIsDefaultTrue());

        String defaultListName = defaultMailingListProperties.getName();
        if (StringUtils.hasText(defaultListName)) {
            status.setDefaultMailingListExists(mailingListRepository.existsByListName(defaultListName));
            status.setDefaultMailingListName(defaultListName);
        }

        return status;
    }

    /**
     * Data class for initialization status information.
     */
    @lombok.Data
    public static class InitializationStatus {
        private Boolean initializeDefaultsEnabled;
        private Boolean updateExistingEnabled;
        private Boolean skipIfExistsEnabled;
        private Boolean defaultEmailConfigExists;
        private Boolean defaultMailingListExists;
        private String defaultMailingListName;
    }
}
