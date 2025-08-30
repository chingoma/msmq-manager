-- Create message templates table
CREATE TABLE msmq_message_templates (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL UNIQUE,
    template_type VARCHAR(100) NOT NULL,
    template_content TEXT NOT NULL,
    description TEXT,
    parameters JSONB,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Create index on template name for fast lookups
CREATE INDEX idx_message_templates_name ON msmq_message_templates(template_name);

-- Create index on template type for filtering
CREATE INDEX idx_message_templates_type ON msmq_message_templates(template_type);
