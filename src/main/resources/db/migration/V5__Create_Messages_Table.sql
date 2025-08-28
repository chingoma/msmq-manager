-- Migration to create messages table for storing MSMQ messages
-- This replaces in-memory storage with proper database persistence

CREATE TABLE IF NOT EXISTS msmq_messages (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(255) UNIQUE NOT NULL,
    queue_name VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(255),
    label VARCHAR(500),
    body TEXT NOT NULL,
    priority INTEGER DEFAULT 0,
    message_size BIGINT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6),
    expires_at TIMESTAMP(6),
    is_processed BOOLEAN DEFAULT FALSE,
    processing_status VARCHAR(50) DEFAULT 'PENDING',
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    error_message TEXT,
    additional_properties JSONB,
    
    -- Foreign key to queue config
    CONSTRAINT fk_messages_queue_config 
        FOREIGN KEY (queue_name) 
        REFERENCES msmq_queue_config(queue_name) 
        ON DELETE CASCADE
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_messages_queue_name ON msmq_messages(queue_name);
CREATE INDEX IF NOT EXISTS idx_messages_message_id ON msmq_messages(message_id);
CREATE INDEX IF NOT EXISTS idx_messages_correlation_id ON msmq_messages(correlation_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON msmq_messages(created_at);
CREATE INDEX IF NOT EXISTS idx_messages_processing_status ON msmq_messages(processing_status);
CREATE INDEX IF NOT EXISTS idx_messages_is_processed ON msmq_messages(is_processed);

-- Add constraints
ALTER TABLE msmq_messages ADD CONSTRAINT chk_priority_range CHECK (priority >= 0 AND priority <= 7);
ALTER TABLE msmq_messages ADD CONSTRAINT chk_retry_count CHECK (retry_count >= 0);
ALTER TABLE msmq_messages ADD CONSTRAINT chk_max_retries CHECK (max_retries >= 0);

-- Add comments
COMMENT ON TABLE msmq_messages IS 'Stores MSMQ messages with full persistence instead of in-memory storage';
COMMENT ON COLUMN msmq_messages.message_id IS 'Unique identifier for the message';
COMMENT ON COLUMN msmq_messages.queue_name IS 'Name of the queue this message belongs to';
COMMENT ON COLUMN msmq_messages.correlation_id IS 'Correlation ID for message tracking';
COMMENT ON COLUMN msmq_messages.body IS 'Message body content';
COMMENT ON COLUMN msmq_messages.priority IS 'Message priority (0-7)';
COMMENT ON COLUMN msmq_messages.processing_status IS 'Current processing status (PENDING, PROCESSING, COMPLETED, FAILED)';
COMMENT ON COLUMN msmq_messages.additional_properties IS 'Additional message properties stored as JSON';
