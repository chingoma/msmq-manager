-- =====================================================
-- MSMQ Manager Database Schema - Additional Indexes and Constraints
-- Version: 1.0.1
-- Description: Adds additional performance optimizations and constraints
-- =====================================================

-- =====================================================
-- Additional Performance Indexes
-- =====================================================

-- Composite index for message audit queries by queue and time range
CREATE INDEX IF NOT EXISTS idx_message_audit_queue_time ON msmq_message_audit(queue_name, operation_timestamp);

-- Composite index for performance metrics by metric name and time
CREATE INDEX IF NOT EXISTS idx_performance_metrics_name_time ON msmq_performance_metrics(metric_name, timestamp);

-- =====================================================
-- Additional Constraints
-- =====================================================

-- Add check constraints for data validation (using DO block to handle existing constraints)
DO $$
BEGIN
    -- Queue name length constraint
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_queue_name_length') THEN
        ALTER TABLE msmq_queue_config ADD CONSTRAINT chk_queue_name_length 
        CHECK (length(queue_name) > 0 AND length(queue_name) <= 124);
    END IF;
    
    -- Queue path length constraint
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_queue_path_length') THEN
        ALTER TABLE msmq_queue_config ADD CONSTRAINT chk_queue_path_length 
        CHECK (length(queue_path) > 0 AND length(queue_path) <= 500);
    END IF;
    
    -- Max message size constraint
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_max_message_size') THEN
        ALTER TABLE msmq_queue_config ADD CONSTRAINT chk_max_message_size 
        CHECK (max_message_size > 0 AND max_message_size <= 104857600);
    END IF;
    
    -- Default timeout constraint
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_default_timeout') THEN
        ALTER TABLE msmq_queue_config ADD CONSTRAINT chk_default_timeout 
        CHECK (default_timeout >= 0 AND default_timeout <= 300000);
    END IF;
    
    -- Operation type constraint
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_operation_type') THEN
        ALTER TABLE msmq_message_audit ADD CONSTRAINT chk_operation_type 
        CHECK (operation_type IN ('SEND', 'RECEIVE', 'PEEK', 'DELETE', 'PURGE'));
    END IF;
    
    -- Operation status constraint
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_operation_status') THEN
        ALTER TABLE msmq_message_audit ADD CONSTRAINT chk_operation_status 
        CHECK (operation_status IN ('SUCCESS', 'FAILED', 'TIMEOUT', 'PENDING'));
    END IF;
    
    -- Connection status constraint
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_connection_status') THEN
        ALTER TABLE msmq_connection_session ADD CONSTRAINT chk_connection_status 
        CHECK (connection_status IN ('CONNECTED', 'DISCONNECTED', 'ERROR', 'CONNECTING'));
    END IF;
    
    -- Connection port constraint
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_connection_port') THEN
        ALTER TABLE msmq_connection_session ADD CONSTRAINT chk_connection_port 
        CHECK (connection_port > 0 AND connection_port <= 65535);
    END IF;
    
    -- Metric value constraint
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_metric_value') THEN
        ALTER TABLE msmq_performance_metrics ADD CONSTRAINT chk_metric_value 
        CHECK (metric_value >= 0);
    END IF;
    
    -- Error count constraint
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_error_count') THEN
        ALTER TABLE msmq_error_statistics ADD CONSTRAINT chk_error_count 
        CHECK (error_count > 0);
    END IF;
    
    -- Severity constraint
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_severity') THEN
        ALTER TABLE msmq_error_statistics ADD CONSTRAINT chk_severity 
        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));
    END IF;
    
    -- Health status constraint
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_health_status') THEN
        ALTER TABLE msmq_system_health ADD CONSTRAINT chk_health_status 
        CHECK (health_status IN ('UP', 'DOWN', 'DEGRADED', 'UNKNOWN'));
    END IF;
END $$;

-- =====================================================
-- Additional Views for Common Queries
-- =====================================================

-- View for queue statistics
CREATE OR REPLACE VIEW v_queue_statistics AS
SELECT 
    qc.queue_name,
    qc.queue_path,
    qc.is_active,
    COUNT(ma.id) as total_operations,
    COUNT(CASE WHEN ma.operation_status = 'SUCCESS' THEN 1 END) as successful_operations,
    COUNT(CASE WHEN ma.operation_status = 'FAILED' THEN 1 END) as failed_operations,
    AVG(ma.processing_time_ms) as avg_processing_time_ms,
    MAX(ma.operation_timestamp) as last_operation_time
FROM msmq_queue_config qc
LEFT JOIN msmq_message_audit ma ON qc.queue_name = ma.queue_name
GROUP BY qc.id, qc.queue_name, qc.queue_path, qc.is_active;

-- View for error summary
CREATE OR REPLACE VIEW v_error_summary AS
SELECT 
    error_code,
    error_message,
    SUM(error_count) as total_occurrences,
    COUNT(DISTINCT queue_name) as affected_queues,
    MAX(last_occurrence) as last_occurrence,
    severity
FROM msmq_error_statistics
GROUP BY error_code, error_message, severity
ORDER BY total_occurrences DESC;

-- View for system health overview
CREATE OR REPLACE VIEW v_system_health_overview AS
SELECT 
    component_name,
    health_check_name,
    health_status,
    health_details,
    last_check_time,
    response_time_ms
FROM msmq_system_health
WHERE last_check_time >= CURRENT_TIMESTAMP - INTERVAL '1 hour'
ORDER BY component_name, last_check_time DESC;
