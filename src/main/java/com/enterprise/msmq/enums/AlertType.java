package com.enterprise.msmq.enums;

/**
 * Alert types for different queue events.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
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
