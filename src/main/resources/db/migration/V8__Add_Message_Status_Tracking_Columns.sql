-- Migration to add message status tracking columns for securities settlement
-- Adds support for common reference ID tracking and enhanced status management

-- Add new columns for enhanced status tracking
ALTER TABLE msmq_messages 
ADD COLUMN IF NOT EXISTS common_reference_id VARCHAR(50),
ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(100),
ADD COLUMN IF NOT EXISTS movement_type VARCHAR(20),
ADD COLUMN IF NOT EXISTS linked_transaction_id VARCHAR(100),
ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP(6),
ADD COLUMN IF NOT EXISTS received_at TIMESTAMP(6),
ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP(6),
ADD COLUMN IF NOT EXISTS environment VARCHAR(20),
ADD COLUMN IF NOT EXISTS template_name VARCHAR(100);

-- Add indexes for better performance on new columns
CREATE INDEX IF NOT EXISTS idx_messages_common_reference_id ON msmq_messages(common_reference_id);
CREATE INDEX IF NOT EXISTS idx_messages_transaction_id ON msmq_messages(transaction_id);
CREATE INDEX IF NOT EXISTS idx_messages_movement_type ON msmq_messages(movement_type);
CREATE INDEX IF NOT EXISTS idx_messages_linked_transaction_id ON msmq_messages(linked_transaction_id);
CREATE INDEX IF NOT EXISTS idx_messages_environment ON msmq_messages(environment);
CREATE INDEX IF NOT EXISTS idx_messages_template_name ON msmq_messages(template_name);
CREATE INDEX IF NOT EXISTS idx_messages_sent_at ON msmq_messages(sent_at);
CREATE INDEX IF NOT EXISTS idx_messages_processed_at ON msmq_messages(processed_at);

-- Add composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_messages_common_ref_movement ON msmq_messages(common_reference_id, movement_type);
CREATE INDEX IF NOT EXISTS idx_messages_status_environment ON msmq_messages(processing_status, environment);
CREATE INDEX IF NOT EXISTS idx_messages_sent_not_processed ON msmq_messages(sent_at, processed_at) 
WHERE sent_at IS NOT NULL AND processed_at IS NULL;

-- Add constraints for data integrity
ALTER TABLE msmq_messages 
ADD CONSTRAINT chk_movement_type CHECK (movement_type IN ('RECE', 'DELI', 'GENERAL', 'SYSTEM') OR movement_type IS NULL);

ALTER TABLE msmq_messages 
ADD CONSTRAINT chk_environment CHECK (environment IN ('local', 'remote') OR environment IS NULL);

-- Add unique constraint on transaction_id
ALTER TABLE msmq_messages 
ADD CONSTRAINT uk_messages_transaction_id UNIQUE (transaction_id);

-- Add comments for new columns
COMMENT ON COLUMN msmq_messages.common_reference_id IS 'Common reference ID linking paired messages (e.g., RECE/DELI)';
COMMENT ON COLUMN msmq_messages.transaction_id IS 'Unique transaction identifier for the message';
COMMENT ON COLUMN msmq_messages.movement_type IS 'Type of movement (RECE, DELI, GENERAL, SYSTEM)';
COMMENT ON COLUMN msmq_messages.linked_transaction_id IS 'Transaction ID of linked/paired message';
COMMENT ON COLUMN msmq_messages.sent_at IS 'Timestamp when message was sent to queue';
COMMENT ON COLUMN msmq_messages.received_at IS 'Timestamp when message was received from queue';
COMMENT ON COLUMN msmq_messages.processed_at IS 'Timestamp when message processing was completed';
COMMENT ON COLUMN msmq_messages.environment IS 'Environment where message was sent (local, remote)';
COMMENT ON COLUMN msmq_messages.template_name IS 'Name of the template used to generate the message';

-- Update existing records to have default values where appropriate
UPDATE msmq_messages 
SET environment = 'local' 
WHERE environment IS NULL;

UPDATE msmq_messages 
SET movement_type = 'GENERAL' 
WHERE movement_type IS NULL;

-- Add a function to generate transaction IDs (if needed)
CREATE OR REPLACE FUNCTION generate_transaction_id() 
RETURNS VARCHAR(100) AS $$
BEGIN
    RETURN TO_CHAR(CURRENT_DATE, 'YYMMDD') || 
           UPPER(SUBSTRING(MD5(RANDOM()::TEXT), 1, 6));
END;
$$ LANGUAGE plpgsql;

-- Add a function to generate common reference IDs (if needed)
CREATE OR REPLACE FUNCTION generate_common_reference_id() 
RETURNS VARCHAR(50) AS $$
BEGIN
    RETURN FLOOR(RANDOM() * 10)::TEXT || 
           UPPER(SUBSTRING(MD5(RANDOM()::TEXT), 1, 8));
END;
$$ LANGUAGE plpgsql;
