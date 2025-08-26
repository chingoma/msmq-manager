-- =====================================================
-- MSMQ Manager Database Schema - Initial Migration
-- Version: 1.0.0
-- Description: Creates the initial database schema for MSMQ Manager
-- =====================================================

-- Create sequence for auto-incrementing IDs
CREATE SEQUENCE IF NOT EXISTS msmq_id_seq START 1;

-- =====================================================
-- Queue Configuration Table
-- =====================================================
CREATE TABLE IF NOT EXISTS msmq_queue_config (
    id BIGINT PRIMARY KEY DEFAULT nextval('msmq_id_seq'),
    queue_name VARCHAR(124) NOT NULL UNIQUE,
    queue_path VARCHAR(500) NOT NULL,
    queue_type VARCHAR(50) NOT NULL DEFAULT 'PRIVATE',
    is_transactional BOOLEAN DEFAULT FALSE,
    is_durable BOOLEAN DEFAULT TRUE,
    max_message_size BIGINT DEFAULT 4194304,
    default_timeout INTEGER DEFAULT 60000,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    updated_by VARCHAR(100) DEFAULT 'SYSTEM',
    is_active BOOLEAN DEFAULT TRUE,
    description TEXT
);

-- =====================================================
-- Message Audit Log Table
-- =====================================================
CREATE TABLE IF NOT EXISTS msmq_message_audit (
    id BIGINT PRIMARY KEY DEFAULT nextval('msmq_id_seq'),
    request_id VARCHAR(36) NOT NULL,
    queue_name VARCHAR(124) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    message_id VARCHAR(100),
    message_body TEXT,
    message_size INTEGER,
    operation_status VARCHAR(20) NOT NULL,
    error_message TEXT,
    response_code VARCHAR(10),
    operation_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processing_time_ms INTEGER,
    source_ip VARCHAR(45),
    user_agent TEXT,
    additional_metadata JSONB
);

-- =====================================================
-- Connection Session Table
-- =====================================================
CREATE TABLE IF NOT EXISTS msmq_connection_session (
    id BIGINT PRIMARY KEY DEFAULT nextval('msmq_id_seq'),
    session_id VARCHAR(36) NOT NULL UNIQUE,
    connection_host VARCHAR(255) NOT NULL,
    connection_port INTEGER NOT NULL,
    connection_status VARCHAR(20) NOT NULL,
    connection_type VARCHAR(20) DEFAULT 'TCP',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    connection_metadata JSONB
);

-- =====================================================
-- Performance Metrics Table
-- =====================================================
CREATE TABLE IF NOT EXISTS msmq_performance_metrics (
    id BIGINT PRIMARY KEY DEFAULT nextval('msmq_id_seq'),
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    metric_unit VARCHAR(20),
    queue_name VARCHAR(124),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    metric_type VARCHAR(50) NOT NULL,
    tags JSONB
);

-- =====================================================
-- Error Statistics Table
-- =====================================================
CREATE TABLE IF NOT EXISTS msmq_error_statistics (
    id BIGINT PRIMARY KEY DEFAULT nextval('msmq_id_seq'),
    error_code VARCHAR(10) NOT NULL,
    error_message TEXT,
    error_count INTEGER DEFAULT 1,
    first_occurrence TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_occurrence TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    queue_name VARCHAR(124),
    operation_type VARCHAR(20),
    severity VARCHAR(20) DEFAULT 'MEDIUM'
);

-- =====================================================
-- System Health Table
-- =====================================================
CREATE TABLE IF NOT EXISTS msmq_system_health (
    id BIGINT PRIMARY KEY DEFAULT nextval('msmq_id_seq'),
    health_check_name VARCHAR(100) NOT NULL,
    health_status VARCHAR(20) NOT NULL,
    health_details TEXT,
    last_check_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    response_time_ms INTEGER,
    error_message TEXT,
    component_name VARCHAR(100)
);

-- =====================================================
-- Basic Indexes for Performance
-- =====================================================

-- Queue configuration indexes
CREATE INDEX IF NOT EXISTS idx_queue_config_name ON msmq_queue_config(queue_name);
CREATE INDEX IF NOT EXISTS idx_queue_config_active ON msmq_queue_config(is_active);

-- Message audit indexes
CREATE INDEX IF NOT EXISTS idx_message_audit_request_id ON msmq_message_audit(request_id);
CREATE INDEX IF NOT EXISTS idx_message_audit_queue_name ON msmq_message_audit(queue_name);
CREATE INDEX IF NOT EXISTS idx_message_audit_timestamp ON msmq_message_audit(operation_timestamp);

-- Connection session indexes
CREATE INDEX IF NOT EXISTS idx_connection_session_id ON msmq_connection_session(session_id);
CREATE INDEX IF NOT EXISTS idx_connection_session_status ON msmq_connection_session(connection_status);

-- Performance metrics indexes
CREATE INDEX IF NOT EXISTS idx_performance_metrics_name ON msmq_performance_metrics(metric_name);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_timestamp ON msmq_performance_metrics(timestamp);

-- Error statistics indexes
CREATE INDEX IF NOT EXISTS idx_error_statistics_code ON msmq_error_statistics(error_code);
CREATE INDEX IF NOT EXISTS idx_error_statistics_queue ON msmq_error_statistics(queue_name);

-- System health indexes
CREATE INDEX IF NOT EXISTS idx_system_health_name ON msmq_system_health(health_check_name);
CREATE INDEX IF NOT EXISTS idx_system_health_status ON msmq_system_health(health_status);

-- =====================================================
-- Initial Data
-- =====================================================

-- Insert default queue configuration
INSERT INTO msmq_queue_config (queue_name, queue_path, queue_type, description) 
VALUES ('default-queue', '.\private$\default-queue', 'PRIVATE', 'Default MSMQ queue for testing')
ON CONFLICT (queue_name) DO NOTHING;

-- Insert default system health check
INSERT INTO msmq_system_health (health_check_name, health_status, health_details, component_name)
VALUES ('database-connection', 'UP', 'Database connection established successfully', 'PostgreSQL')
ON CONFLICT DO NOTHING;
