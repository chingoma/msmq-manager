-- Migration to fix table name mismatch
-- Drop the incorrectly named table created by Hibernate
-- Keep the correctly named table from Flyway migrations

-- Drop the incorrectly named table if it exists
DROP TABLE IF EXISTS msmq_queue_configs CASCADE;

-- Ensure the correct table exists with proper structure
-- (This will be handled by the existing V1 migration)

-- Add any missing columns or constraints if needed
-- (This ensures data consistency)
