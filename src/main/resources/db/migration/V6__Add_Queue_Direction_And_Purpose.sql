-- =====================================================
-- Migration: Add Queue Direction and Purpose
-- Version: 7
-- Description: Adds queue direction and purpose classification to msmq_queue_config table
-- =====================================================

-- Add new columns for queue classification
ALTER TABLE msmq_queue_config 
ADD COLUMN queue_direction VARCHAR(20) DEFAULT 'BIDIRECTIONAL',
ADD COLUMN queue_purpose VARCHAR(50) DEFAULT 'GENERAL';

-- Add constraints to ensure valid values
ALTER TABLE msmq_queue_config 
ADD CONSTRAINT chk_queue_direction 
CHECK (queue_direction IN ('INCOMING_ONLY', 'OUTGOING_ONLY', 'BIDIRECTIONAL'));

ALTER TABLE msmq_queue_config 
ADD CONSTRAINT chk_queue_purpose 
CHECK (queue_purpose IN ('GENERAL', 'SWIFT_MESSAGES', 'SYSTEM_NOTIFICATIONS', 'DATA_SYNC', 'ERROR_HANDLING', 'AUDIT_LOGS', 'URGENT_MESSAGES', 'BATCH_PROCESSING'));

-- Add indexes for better query performance
CREATE INDEX idx_queue_direction ON msmq_queue_config(queue_direction);
CREATE INDEX idx_queue_purpose ON msmq_queue_config(queue_purpose);

-- Add composite index for common queries
CREATE INDEX idx_queue_direction_active ON msmq_queue_config(queue_direction, is_active);

-- Add comments for documentation
COMMENT ON COLUMN msmq_queue_config.queue_direction IS 'Queue direction: INCOMING_ONLY, OUTGOING_ONLY, or BIDIRECTIONAL';
COMMENT ON COLUMN msmq_queue_config.queue_purpose IS 'Queue purpose: GENERAL, SWIFT_MESSAGES, SYSTEM_NOTIFICATIONS, etc.';

-- Update existing records to have sensible defaults
-- All existing queues will be set to BIDIRECTIONAL and GENERAL by default
UPDATE msmq_queue_config 
SET queue_direction = 'BIDIRECTIONAL', 
    queue_purpose = 'GENERAL' 
WHERE queue_direction IS NULL OR queue_purpose IS NULL;

-- Verify the migration
DO $$
BEGIN
    -- Check if columns were added successfully
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'msmq_queue_config' 
        AND column_name = 'queue_direction'
    ) THEN
        RAISE EXCEPTION 'Column queue_direction was not added successfully';
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'msmq_queue_config' 
        AND column_name = 'queue_purpose'
    ) THEN
        RAISE EXCEPTION 'Column queue_purpose was not added successfully';
    END IF;
    
    -- Check if constraints were added successfully
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.check_constraints 
        WHERE constraint_name = 'chk_queue_direction'
    ) THEN
        RAISE EXCEPTION 'Constraint chk_queue_direction was not added successfully';
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.check_constraints 
        WHERE constraint_name = 'chk_queue_purpose'
    ) THEN
        RAISE EXCEPTION 'Constraint chk_queue_purpose was not added successfully';
    END IF;
    
    RAISE NOTICE 'Migration V7 completed successfully - Queue direction and purpose columns added';
END $$;
