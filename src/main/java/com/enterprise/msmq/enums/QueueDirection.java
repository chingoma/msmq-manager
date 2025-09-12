package com.enterprise.msmq.enums;

/**
 * Enumeration for MSMQ queue direction classification.
 * Defines whether a queue is for incoming messages, outgoing messages, or both.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
public enum QueueDirection {
    
    /**
     * Queue that only receives messages (incoming only).
     * Used for input queues, data ingestion, etc.
     */
    INCOMING_ONLY("Incoming Only"),
    
    /**
     * Queue that only sends messages (outgoing only).
     * Used for output queues, notifications, etc.
     */
    OUTGOING_ONLY("Outgoing Only"),
    
    /**
     * Queue that both sends and receives messages.
     * Used for general purpose, processing queues, etc.
     */
    BIDIRECTIONAL("Bidirectional");
    
    private final String displayName;
    
    QueueDirection(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Get the human-readable display name for this direction.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if this direction allows sending messages.
     * 
     * @return true if messages can be sent to this queue
     */
    public boolean allowsSending() {
        return this == OUTGOING_ONLY || this == BIDIRECTIONAL;
    }
    
    /**
     * Check if this direction allows receiving messages.
     * 
     * @return true if messages can be received from this queue
     */
    public boolean allowsReceiving() {
        return this == INCOMING_ONLY || this == BIDIRECTIONAL;
    }
    
    /**
     * Check if this direction is incoming only.
     * 
     * @return true if this is an incoming-only queue
     */
    public boolean isIncomingOnly() {
        return this == INCOMING_ONLY;
    }
    
    /**
     * Check if this direction is outgoing only.
     * 
     * @return true if this is an outgoing-only queue
     */
    public boolean isOutgoingOnly() {
        return this == OUTGOING_ONLY;
    }
    
    /**
     * Check if this direction is bidirectional.
     * 
     * @return true if this is a bidirectional queue
     */
    public boolean isBidirectional() {
        return this == BIDIRECTIONAL;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
