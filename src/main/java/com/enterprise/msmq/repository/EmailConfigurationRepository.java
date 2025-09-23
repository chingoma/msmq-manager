package com.enterprise.msmq.repository;

import com.enterprise.msmq.entity.EmailConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Repository interface for EmailConfiguration entities.
 * Provides database access methods for email configuration management.
 *
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface EmailConfigurationRepository extends JpaRepository<EmailConfiguration, Long> {

    /**
     * Finds the default email configuration.
     *
     * @return Optional containing the default email configuration if found
     */
    Optional<EmailConfiguration> findByIsDefaultTrue();

    /**
     * Finds the default active email configuration.
     *
     * @return Optional containing the default active email configuration if found
     */
    Optional<EmailConfiguration> findByIsDefaultTrueAndIsActiveTrue();

    /**
     * Finds an email configuration by its name.
     *
     * @param configName the configuration name
     * @return Optional containing the email configuration if found
     */
    Optional<EmailConfiguration> findByConfigName(String configName);

    /**
     * Finds all active email configurations.
     *
     * @return List of active email configurations
     */
    @Query("SELECT e FROM EmailConfiguration e WHERE e.isActive = true")
    java.util.List<EmailConfiguration> findAllActive();

    /**
     * Finds all active email configurations.
     *
     * @return List of active email configurations
     */
    java.util.List<EmailConfiguration> findByIsActiveTrue();

    /**
     * Checks if a default configuration exists.
     *
     * @return true if a default configuration exists, false otherwise
     */
    boolean existsByIsDefaultTrue();

    /**
     * Checks if a configuration with the given name exists.
     *
     * @param configName the configuration name
     * @return true if configuration exists, false otherwise
     */
    boolean existsByConfigName(String configName);

    /**
     * Counts active email configurations.
     *
     * @return count of active email configurations
     */
    long countByIsActiveTrue();

    /**
     * Clears all default flags from email configurations.
     */
    @Modifying
    @Transactional
    @Query("UPDATE EmailConfiguration e SET e.isDefault = false WHERE e.isDefault = true")
    void clearDefaultFlags();
}
