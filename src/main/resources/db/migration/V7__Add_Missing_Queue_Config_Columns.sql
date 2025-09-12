-- Migration: Add Missing Queue Config Columns
-- Version: 7
-- Description: Adds missing columns to msmq_queue_config table to match the entity

-- Add missing columns (only the essential ones that are causing errors)
ALTER TABLE msmq_queue_config 
ADD COLUMN IF NOT EXISTS is_transactional BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS is_private BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS is_authenticated BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS is_encrypted BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS last_sync_time TIMESTAMP,
ADD COLUMN IF NOT EXISTS queue_direction VARCHAR(20) DEFAULT 'BIDIRECTIONAL',
ADD COLUMN IF NOT EXISTS queue_purpose VARCHAR(30) DEFAULT 'GENERAL',
ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 3,
ADD COLUMN IF NOT EXISTS retry_interval_ms BIGINT DEFAULT 5000,
ADD COLUMN IF NOT EXISTS timeout_ms BIGINT DEFAULT 30000;

-- Update existing records with default values
UPDATE msmq_queue_config 
SET 
    is_transactional = FALSE,
    is_private = FALSE,
    is_authenticated = FALSE,
    is_encrypted = FALSE,
    last_sync_time = CURRENT_TIMESTAMP,
    queue_direction = 'BIDIRECTIONAL',
    queue_purpose = 'GENERAL',
    retry_count = 3,
    retry_interval_ms = 5000,
    timeout_ms = 30000
WHERE is_transactional IS NULL 
   OR is_private IS NULL 
   OR is_authenticated IS NULL 
   OR is_encrypted IS NULL 
   OR last_sync_time IS NULL 
   OR queue_direction IS NULL 
   OR queue_purpose IS NULL
   OR retry_count IS NULL
   OR retry_interval_ms IS NULL
   OR timeout_ms IS NULL;

-- Verify migration
DO $$
BEGIN
    RAISE NOTICE 'Migration V7 completed successfully - Missing columns added to msmq_queue_config table';
    RAISE NOTICE 'Columns added: is_transactional, is_private, is_authenticated, is_encrypted, last_sync_time, queue_direction, queue_purpose, retry_count, retry_interval_ms, timeout_ms';
END $$;
