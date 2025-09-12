package com.enterprise.msmq.entity;

import com.enterprise.msmq.enums.AlertSeverity;
import com.enterprise.msmq.enums.AlertType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity for managing mailing lists and recipient groups.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Entity
@Table(name = "mailing_lists")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailingList {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "list_name", nullable = false, unique = true)
    private String listName;
    
    @Column(name = "description")
    private String description;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "mailing_list_recipients",
        joinColumns = @JoinColumn(name = "mailing_list_id")
    )
    @Column(name = "email_address")
    private List<String> emailAddresses;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "alert_severities")
    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "mailing_list_alert_severities",
        joinColumns = @JoinColumn(name = "mailing_list_id")
    )
    private List<AlertSeverity> alertSeverities;
    
    @Column(name = "alert_types")
    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "mailing_list_alert_types",
        joinColumns = @JoinColumn(name = "mailing_list_id")
    )
    private List<AlertType> alertTypes;
    
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
    
    /**
     * Check if this mailing list should receive alerts for the given severity and type.
     * 
     * @param severity the alert severity
     * @param type the alert type
     * @return true if the mailing list should receive this alert
     */
    public boolean shouldReceiveAlert(AlertSeverity severity, AlertType type) {
        if (!isActive) {
            return false;
        }
        
        // If no specific severities/types are configured, receive all
        if ((alertSeverities == null || alertSeverities.isEmpty()) && 
            (alertTypes == null || alertTypes.isEmpty())) {
            return true;
        }
        
        // Check severity filter
        if (alertSeverities != null && !alertSeverities.isEmpty() && 
            !alertSeverities.contains(severity)) {
            return false;
        }
        
        // Check type filter
        if (alertTypes != null && !alertTypes.isEmpty() && 
            !alertTypes.contains(type)) {
            return false;
        }
        
        return true;
    }
}
