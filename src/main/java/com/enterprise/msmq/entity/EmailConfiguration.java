package com.enterprise.msmq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for storing email SMTP configuration.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Entity
@Table(name = "email_configurations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "config_name", nullable = false, unique = true)
    private String configName;
    
    @Column(name = "smtp_host", nullable = false)
    private String smtpHost;
    
    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort;
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "password")
    private String password;
    
    @Column(name = "from_email", nullable = false)
    private String fromEmail;
    
    @Column(name = "from_name")
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
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
