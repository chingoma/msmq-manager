package com.enterprise.msmq.enums;

/**
 * Enumeration for MSMQ queue purpose classification.
 * Defines the business purpose and type of messages each queue handles.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
public enum QueuePurpose {
    
    /**
     * General purpose queue for mixed message types.
     * Default purpose for most queues.
     */
    GENERAL("General"),
    
    /**
     * Queue specifically for SWIFT financial messages.
     * Handles SWIFT MT and MX message formats.
     */
    SWIFT_MESSAGES("SWIFT Messages"),
    
    /**
     * Queue for system notifications and alerts.
     * Handles system events, monitoring alerts, etc.
     */
    SYSTEM_NOTIFICATIONS("System Notifications"),
    
    /**
     * Queue for data synchronization operations.
     * Handles data sync, replication, etc.
     */
    DATA_SYNC("Data Synchronization"),
    
    /**
     * Queue for error handling and dead letter messages.
     * Handles failed messages, retries, etc.
     */
    ERROR_HANDLING("Error Handling"),
    
    /**
     * Queue for audit and compliance messages.
     * Handles audit trails, compliance logs, etc.
     */
    AUDIT_LOGS("Audit Logs"),
    
    /**
     * Queue for high-priority urgent messages.
     * Handles critical alerts, emergency notifications, etc.
     */
    URGENT_MESSAGES("Urgent Messages"),
    
    /**
     * Queue for batch processing operations.
     * Handles bulk data processing, batch jobs, etc.
     */
    BATCH_PROCESSING("Batch Processing");
    
    private final String displayName;
    
    QueuePurpose(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Get the human-readable display name for this purpose.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if this purpose is for financial messages.
     * 
     * @return true if this is a financial message queue
     */
    public boolean isFinancial() {
        return this == SWIFT_MESSAGES;
    }
    
    /**
     * Check if this purpose is for system operations.
     * 
     * @return true if this is a system operation queue
     */
    public boolean isSystemOperation() {
        return this == SYSTEM_NOTIFICATIONS || 
               this == AUDIT_LOGS || 
               this == ERROR_HANDLING;
    }
    
    /**
     * Check if this purpose is for data operations.
     * 
     * @return true if this is a data operation queue
     */
    public boolean isDataOperation() {
        return this == DATA_SYNC || 
               this == BATCH_PROCESSING;
    }
    
    /**
     * Check if this purpose requires high priority.
     * 
     * @return true if this queue requires high priority handling
     */
    public boolean requiresHighPriority() {
        return this == URGENT_MESSAGES || 
               this == ERROR_HANDLING || 
               this == SYSTEM_NOTIFICATIONS;
    }
    
    /**
     * Check if this purpose is for compliance.
     * 
     * @return true if this queue is for compliance purposes
     */
    public boolean isCompliance() {
        return this == AUDIT_LOGS || 
               this == SWIFT_MESSAGES;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
