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

-- Insert sample SWIFT template
INSERT INTO msmq_message_templates (
    template_name, 
    template_type, 
    template_content, 
    description, 
    parameters
) VALUES (
    'SWIFT_SHARE_TRANSFER',
    'SWIFT',
    '<RequestPayload xmlns="SWIFTNetBusinessEnvelope"><AppHdr xmlns="urn:iso:std:iso:20022:tech:xsd:head.001.001.01"><Fr><OrgId><Id><OrgId><AnyBIC>{{FROM_BIC}}</AnyBIC></OrgId></Id></OrgId></Fr><To><OrgId><Id><OrgId><AnyBIC>{{TO_BIC}}</AnyBIC></OrgId></Id></OrgId></To><BizMsgIdr>{{MESSAGE_TYPE}}</BizMsgIdr><MsgDefIdr>{{MSG_DEF_ID}}</MsgDefIdr><CreDt>{{CREATION_DATE}}</CreDt></AppHdr><Document xmlns="urn:iso:std:iso:20022:tech:xsd:sese.023.001.06"><SctiesSttlmTxInstr><TxId><AcctOwnrTxId>{{TRANSACTION_ID}}</AcctOwnrTxId></TxId><SttlmParams><SttlmDt>{{SETTLEMENT_DATE}}</SttlmDt><SctiesTxTp><Cd>{{TRADE_TYPE}}</Cd></SctiesTxTp><SttlmTxCond><Cd>{{SETTLEMENT_CONDITION}}</Cd></SttlmTxCond></SttlmParams><TradDtls><TradDt>{{TRADE_DATE}}</TradDt><SctiesMvmntTp>{{MOVEMENT_TYPE}}</SctiesMvmntTp><Pmt>{{PAYMENT_TYPE}}</Pmt></TradDtls><FinInstrmId><ISIN>{{ISIN_CODE}}</ISIN><Nm>{{SECURITY_NAME}}</Nm></FinInstrmId><QtyAndAcctDtls><SttlmQty><Qty><Unit>{{QUANTITY}}</Unit></Qty></SttlmQty><SfkpgAcct><Id>{{ACCOUNT_ID}}</Id></SfkpgAcct></QtyAndAcctDtls><SttlmPtiesSts><Pty1><Pty><Nm>{{PARTY1_NAME}}</Nm></Pty><Acct><Id>{{PARTY1_ACCOUNT}}</Id></Acct></Pty1><Pty2><Pty><Nm>{{PARTY2_NAME}}</Nm></Pty><Acct><Id>{{PARTY2_ACCOUNT}}</Id></Acct></Pty2></SttlmPtiesSts><AddtlTxDtls><TxDesc>{{TRANSACTION_DESCRIPTION}}</TxDesc></AddtlTxDtls></SctiesSttlmTxInstr></Document></RequestPayload>',
    'SWIFT Share Transfer Instruction Template',
    '{"FROM_BIC": "string", "TO_BIC": "string", "MESSAGE_TYPE": "string", "MSG_DEF_ID": "string", "CREATION_DATE": "datetime", "TRANSACTION_ID": "string", "SETTLEMENT_DATE": "date", "TRADE_TYPE": "string", "SETTLEMENT_CONDITION": "string", "TRADE_DATE": "date", "MOVEMENT_TYPE": "string", "PAYMENT_TYPE": "string", "ISIN_CODE": "string", "SECURITY_NAME": "string", "QUANTITY": "number", "ACCOUNT_ID": "string", "PARTY1_NAME": "string", "PARTY1_ACCOUNT": "string", "PARTY2_NAME": "string", "PARTY2_ACCOUNT": "string", "TRANSACTION_DESCRIPTION": "string"}'
);

-- Insert sample JSON template
INSERT INTO msmq_message_templates (
    template_name, 
    template_type, 
    template_content, 
    description, 
    parameters
) VALUES (
    'JSON_NOTIFICATION',
    'JSON',
    '{"type": "{{NOTIFICATION_TYPE}}", "content": "{{MESSAGE_CONTENT}}", "level": "{{PRIORITY_LEVEL}}", "timestamp": "{{TIMESTAMP}}", "source": "{{SOURCE_SYSTEM}}"}',
    'JSON Notification Template',
    '{"NOTIFICATION_TYPE": "string", "MESSAGE_CONTENT": "string", "PRIORITY_LEVEL": "string", "TIMESTAMP": "datetime", "SOURCE_SYSTEM": "string"}'
);

-- Insert sample text template
INSERT INTO msmq_message_templates (
    template_name, 
    template_type, 
    template_content, 
    description, 
    parameters
) VALUES (
    'TEXT_ALERT',
    'TEXT',
    'ALERT: {{ALERT_TYPE}} - {{ALERT_MESSAGE}} at {{TIMESTAMP}} from {{SOURCE}}',
    'Text Alert Template',
    '{"ALERT_TYPE": "string", "ALERT_MESSAGE": "string", "TIMESTAMP": "datetime", "SOURCE": "string"}'
);
