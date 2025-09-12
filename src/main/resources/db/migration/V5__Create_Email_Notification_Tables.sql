-- Migration: Create Email Notification Tables
-- Version: V6
-- Description: Creates tables for email SMTP configuration and mailing lists

-- Create email_configurations table
CREATE TABLE email_configurations (
    id BIGSERIAL PRIMARY KEY,
    config_name VARCHAR(100) NOT NULL UNIQUE,
    smtp_host VARCHAR(255) NOT NULL,
    smtp_port INTEGER NOT NULL,
    username VARCHAR(255),
    password VARCHAR(255),
    from_email VARCHAR(255) NOT NULL,
    from_name VARCHAR(255),
    use_tls BOOLEAN NOT NULL DEFAULT true,
    use_ssl BOOLEAN NOT NULL DEFAULT false,
    connection_timeout INTEGER DEFAULT 5000,
    read_timeout INTEGER DEFAULT 5000,
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create mailing_lists table
CREATE TABLE mailing_lists (
    id BIGSERIAL PRIMARY KEY,
    list_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create mailing_list_recipients table (for email addresses)
CREATE TABLE mailing_list_recipients (
    mailing_list_id BIGINT NOT NULL,
    email_address VARCHAR(255) NOT NULL,
    PRIMARY KEY (mailing_list_id, email_address),
    FOREIGN KEY (mailing_list_id) REFERENCES mailing_lists(id) ON DELETE CASCADE
);

-- Create mailing_list_alert_severities table
CREATE TABLE mailing_list_alert_severities (
    mailing_list_id BIGINT NOT NULL,
    alert_severities VARCHAR(20) NOT NULL,
    PRIMARY KEY (mailing_list_id, alert_severities),
    FOREIGN KEY (mailing_list_id) REFERENCES mailing_lists(id) ON DELETE CASCADE,
    CHECK (alert_severities IN ('INFO', 'WARNING', 'ERROR'))
);

-- Create mailing_list_alert_types table
CREATE TABLE mailing_list_alert_types (
    mailing_list_id BIGINT NOT NULL,
    alert_types VARCHAR(50) NOT NULL,
    PRIMARY KEY (mailing_list_id, alert_types),
    FOREIGN KEY (mailing_list_id) REFERENCES mailing_lists(id) ON DELETE CASCADE,
    CHECK (alert_types IN ('QUEUE_CREATED', 'QUEUE_DELETED', 'QUEUE_INACTIVE_TOO_LONG', 'QUEUE_UNHEALTHY', 'PERFORMANCE_DEGRADATION', 'SYNC_FAILURE', 'SYSTEM_ERROR'))
);

-- Create indexes for better performance
CREATE INDEX idx_email_configs_active ON email_configurations(is_active);
CREATE INDEX idx_email_configs_default ON email_configurations(is_default);
CREATE INDEX idx_email_configs_smtp_host ON email_configurations(smtp_host);
CREATE INDEX idx_mailing_lists_active ON mailing_lists(is_active);
CREATE INDEX idx_mailing_list_recipients_email ON mailing_list_recipients(email_address);

-- Insert default email configuration (Gmail example)
INSERT INTO email_configurations (
    config_name, smtp_host, smtp_port, username, password, 
    from_email, from_name, use_tls, use_ssl, is_default, 
    created_by, updated_by
) VALUES (
    'Default Gmail', 'smtp.gmail.com', 587, 'your-email@gmail.com', 'your-app-password',
    'your-email@gmail.com', 'MSMQ Monitoring System', true, false, true,
    'SYSTEM', 'SYSTEM'
);

-- Insert default mailing list for all alerts
INSERT INTO mailing_lists (list_name, description, created_by, updated_by) 
VALUES ('System Administrators', 'Default mailing list for all system alerts', 'SYSTEM', 'SYSTEM');

-- Insert admin email to default mailing list
INSERT INTO mailing_list_recipients (mailing_list_id, email_address) 
VALUES (1, 'admin@yourcompany.com');

-- Add comment to tables
COMMENT ON TABLE email_configurations IS 'Stores SMTP configuration for sending email notifications';
COMMENT ON TABLE mailing_lists IS 'Stores mailing list definitions for alert notifications';
COMMENT ON TABLE mailing_list_recipients IS 'Stores email addresses for each mailing list';
COMMENT ON TABLE mailing_list_alert_severities IS 'Stores alert severity filters for each mailing list';
COMMENT ON TABLE mailing_list_alert_types IS 'Stores alert type filters for each mailing list';
