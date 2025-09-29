package com.enterprise.msmq.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing mailing lists for notification distribution.
 * Maps to the mailing_lists table.
 *
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "mailing_lists")
@Data
@EqualsAndHashCode(callSuper = false)
public class MailingList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "list_name", length = 100, nullable = false, unique = true)
    private String listName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ElementCollection
    @CollectionTable(
        name = "mailing_list_recipients",
        joinColumns = @JoinColumn(name = "mailing_list_id")
    )
    @Column(name = "email_address")
    private Set<String> recipients = new HashSet<>();

    @ElementCollection(targetClass = AlertSeverity.class)
    @CollectionTable(
        name = "mailing_list_alert_severities",
        joinColumns = @JoinColumn(name = "mailing_list_id")
    )
    @Column(name = "alert_severities")
    @Enumerated(EnumType.STRING)
    private Set<AlertSeverity> alertSeverities = new HashSet<>();

    @ElementCollection(targetClass = AlertType.class)
    @CollectionTable(
        name = "mailing_list_alert_types",
        joinColumns = @JoinColumn(name = "mailing_list_id")
    )
    @Column(name = "alert_types")
    @Enumerated(EnumType.STRING)
    private Set<AlertType> alertTypes = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Gets the email addresses for this mailing list.
     * This is an alias for getRecipients() to maintain compatibility with existing services.
     *
     * @return Set of email addresses
     */
    public Set<String> getEmailAddresses() {
        return recipients;
    }

    /**
     * Sets the email addresses for this mailing list.
     * This is an alias for setRecipients() to maintain compatibility with existing services.
     *
     * @param emailAddresses Set of email addresses
     */
    public void setEmailAddresses(Set<String> emailAddresses) {
        this.recipients = emailAddresses;
    }

    /**
     * Determines if this mailing list should receive a specific alert.
     *
     * @param severity The alert severity
     * @param alertType The alert type
     * @return true if this mailing list should receive the alert
     */
    public boolean shouldReceiveAlert(com.enterprise.msmq.enums.AlertSeverity severity, com.enterprise.msmq.enums.AlertType alertType) {
        // Convert from enum to our internal enum types
        AlertSeverity internalSeverity = convertToInternalSeverity(severity);
        AlertType internalAlertType = convertToInternalAlertType(alertType);

        // If no specific severities are configured, accept all
        boolean severityMatch = alertSeverities.isEmpty() || alertSeverities.contains(internalSeverity);

        // If no specific alert types are configured, accept all
        boolean alertTypeMatch = alertTypes.isEmpty() || alertTypes.contains(internalAlertType);

        return severityMatch && alertTypeMatch;
    }

    /**
     * Converts external AlertSeverity enum to internal enum
     */
    private AlertSeverity convertToInternalSeverity(com.enterprise.msmq.enums.AlertSeverity severity) {
        if (severity == null) return null;
        try {
            return AlertSeverity.valueOf(severity.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Converts external AlertType enum to internal enum
     */
    private AlertType convertToInternalAlertType(com.enterprise.msmq.enums.AlertType alertType) {
        if (alertType == null) return null;
        try {
            return AlertType.valueOf(alertType.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Enumeration of alert severity levels
     */
    public enum AlertSeverity {
        INFO, WARNING, ERROR
    }

    /**
     * Enumeration of alert types
     */
    public enum AlertType {
        QUEUE_CREATED,
        QUEUE_DELETED,
        QUEUE_INACTIVE_TOO_LONG,
        QUEUE_UNHEALTHY,
        PERFORMANCE_DEGRADATION,
        SYNC_FAILURE,
        SYSTEM_ERROR
    }
}
