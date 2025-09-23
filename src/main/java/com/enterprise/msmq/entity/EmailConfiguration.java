package com.enterprise.msmq.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * Entity representing email SMTP configuration for sending notifications.
 * Maps to the email_configurations table.
 *
 * @author Enterprise Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "email_configurations")
@Data
@EqualsAndHashCode(callSuper = false)
public class EmailConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_name", length = 100, nullable = false, unique = true)
    private String configName;

    @Column(name = "smtp_host", length = 255, nullable = false)
    private String smtpHost;

    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "from_email", length = 255, nullable = false)
    private String fromEmail;

    @Column(name = "from_name", length = 255)
    private String fromName;

    @Column(name = "use_tls", nullable = false)
    private Boolean useTls = true;

    @Column(name = "use_ssl", nullable = false)
    private Boolean useSsl = false;

    @Column(name = "connection_timeout")
    private Integer connectionTimeout = 5000;

    @Column(name = "read_timeout")
    private Integer readTimeout = 5000;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
