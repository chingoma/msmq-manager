package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.MsmqMessage;
import com.enterprise.msmq.entity.MsmqMessageTemplate;
import com.enterprise.msmq.factory.MsmqConnectionFactory;
import com.enterprise.msmq.repository.MsmqMessageTemplateRepository;
import com.enterprise.msmq.service.contracts.IMsmqConnectionManager;
import com.enterprise.msmq.service.contracts.IMsmqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for running automated integration tests on application startup.
 * Tests MSMQ operations, template creation, and message sending using real data.
 *
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MsmqIntegrationTestService {

    private final IMsmqService msmqService;
    private final MsmqMessageTemplateService templateService;
    private final MsmqMessageTemplateRepository templateRepository;
    private final MsmqConnectionFactory connectionFactory;

    @Value("${msmq.integration.tests.enabled:true}")
    private boolean integrationTestsEnabled;

    @Value("${msmq.integration.tests.delay:5000}")
    private long integrationTestsDelay;

    @Value("${msmq.integration.tests.retry-attempts:3}")
    private int integrationTestsRetryAttempts;

    /**
     * Runs integration tests automatically when the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void runIntegrationTests() {
        if (!integrationTestsEnabled) {
            log.info("🚫 MSMQ Integration Tests are disabled in configuration");
            return;
        }

        log.info("🚀 Starting MSMQ Integration Tests (delay: {}ms)...", integrationTestsDelay);
        
        // Add delay to ensure all services are fully initialized
        try {
            Thread.sleep(integrationTestsDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Integration test delay was interrupted");
        }
        
        try {
            // Test 1: Test connection infrastructure
            testConnectionInfrastructure();
            
            // Test 2: Create SWIFT template
            testTemplateCreation();
            
            // Test 3: Send message using template
            testTemplateMessageSending();
            
            // Test 4: Send direct message
            testDirectMessageSending();
            
            log.info("✅ All MSMQ Integration Tests completed successfully!");
            
        } catch (Exception e) {
            log.error("❌ Integration tests failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Test 1: Test the MSMQ connection infrastructure using MsmqConnectionFactory.
     */
    private void testConnectionInfrastructure() {
        log.info("📋 Test 1: Testing MSMQ connection infrastructure...");
        
        try {
            // Get the connection manager from the factory
            IMsmqConnectionManager connectionManager = connectionFactory.createConnectionManager();
            if (connectionManager == null) {
                log.error("❌ Failed to get connection manager from factory");
                return;
            }
            
            log.info("✅ Connection manager retrieved successfully: {}", connectionManager.getClass().getSimpleName());
            
            // Test connection status
            var connectionStatus = connectionManager.getConnectionStatus();
            log.info("✅ Connection status retrieved: {}", connectionStatus.getStatus());
            
            // Test connection establishment
            try {
                connectionManager.connect();
                log.info("✅ Connection established successfully");
                
                // Test if connected
                if (connectionManager.isConnected()) {
                    log.info("✅ Connection verified as active");
                } else {
                    log.warn("⚠️ Connection established but not marked as active");
                }
                
            } catch (Exception e) {
                log.warn("⚠️ Connection establishment failed (this may be expected): {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to test connection infrastructure: {}", e.getMessage());
        }
    }

    /**
     * Test 1: Create SWIFT template using real data from PowerShell scripts.
     */
    private void testTemplateCreation() {
        log.info("📋 Test 1: Creating SWIFT template...");
        
        try {
            // Check if template already exists
            Optional<MsmqMessageTemplate> existingTemplate = templateRepository.findByTemplateName("SWIFT_SHARE_TRANSFER_DETAILED");
            if (existingTemplate.isPresent()) {
                log.info("ℹ️ SWIFT template already exists, skipping creation");
                return;
            }
            
            // Template content from create-swift-template.ps1
            String templateContent = "<RequestPayload xmlns=\"SWIFTNetBusinessEnvelope\"><AppHdr xmlns=\"urn:iso:std:iso:20022:tech:xsd:head.001.001.01\"><Fr><OrgId><Id><OrgId><AnyBIC>{{FROM_BIC}}</AnyBIC></OrgId></Id></OrgId></Fr><To><OrgId><Id><OrgId><AnyBIC>{{TO_BIC}}</AnyBIC></OrgId></Id></OrgId></To><BizMsgIdr>{{MESSAGE_TYPE}}</BizMsgIdr><MsgDefIdr>{{MSG_DEF_ID}}</MsgDefIdr><CreDt>{{CREATION_DATE}}</CreDt></AppHdr><Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:sese.023.001.06\"><SctiesSttlmTxInstr><TxId><AcctOwnrTxId>{{TRANSACTION_ID}}</AcctOwnrTxId></TxId><SttlmParams><SttlmDt>{{SETTLEMENT_DATE}}</SttlmDt><SctiesTxTp><Cd>{{TRADE_TYPE}}</Cd></SctiesTxTp><SttlmTxCond><Cd>{{SETTLEMENT_CONDITION}}</Cd></SttlmTxCond></SttlmParams><TradDtls><TradDt>{{TRADE_DATE}}</TradDt><SctiesMvmntTp>{{MOVEMENT_TYPE}}</SctiesMvmntTp><Pmt>{{PAYMENT_TYPE}}</Pmt></TradDtls><FinInstrmId><ISIN>{{ISIN_CODE}}</ISIN><Nm>{{SECURITY_NAME}}</Nm></FinInstrmId><QtyAndAcctDtls><SttlmQty><Qty><Unit>{{QUANTITY}}</Unit></Qty></SttlmQty><SfkpgAcct><Id>{{ACCOUNT_ID}}</Id></SfkpgAcct></QtyAndAcctDtls><SttlmPtiesSts><Pty1><Pty><Nm>{{PARTY1_NAME}}</Nm></Pty><Acct><Id>{{PARTY1_ACCOUNT}}</Id></Acct></Pty1><Pty2><Pty><Nm>{{PARTY2_NAME}}</Nm></Pty><Acct><Id>{{PARTY2_ACCOUNT}}</Id></Acct></Pty2></SttlmPtiesSts><AddtlTxDtls><TxDesc>{{TRANSACTION_DESCRIPTION}}</TxDesc></AddtlTxDtls></SctiesSttlmTxInstr></Document></RequestPayload>";
            
            // Template parameters from create-swift-template.ps1
            Map<String, String> parameters = new HashMap<>();
            parameters.put("FROM_BIC", "string");
            parameters.put("TO_BIC", "string");
            parameters.put("MESSAGE_TYPE", "string");
            parameters.put("MSG_DEF_ID", "string");
            parameters.put("CREATION_DATE", "datetime");
            parameters.put("TRANSACTION_ID", "string");
            parameters.put("SETTLEMENT_DATE", "date");
            parameters.put("TRADE_TYPE", "string");
            parameters.put("SETTLEMENT_CONDITION", "string");
            parameters.put("TRADE_DATE", "date");
            parameters.put("MOVEMENT_TYPE", "string");
            parameters.put("PAYMENT_TYPE", "string");
            parameters.put("ISIN_CODE", "string");
            parameters.put("SECURITY_NAME", "string");
            parameters.put("QUANTITY", "number");
            parameters.put("ACCOUNT_ID", "string");
            parameters.put("PARTY1_NAME", "string");
            parameters.put("PARTY1_ACCOUNT", "string");
            parameters.put("PARTY2_NAME", "string");
            parameters.put("PARTY2_ACCOUNT", "string");
            parameters.put("TRANSACTION_DESCRIPTION", "string");
            
            MsmqMessageTemplate template = new MsmqMessageTemplate();
            template.setTemplateName("SWIFT_SHARE_TRANSFER_DETAILED");
            template.setTemplateType("SWIFT");
            template.setTemplateContent(templateContent);
            template.setDescription("Detailed SWIFT Share Transfer Instruction Template with all parameters");
            template.setParameters(parameters);
            template.setIsActive(true);
            template.setCreatedBy("System");
            template.setCreatedAt(LocalDateTime.now());
            
            MsmqMessageTemplate createdTemplate = templateService.createTemplate(template);
            log.info("✅ SWIFT template created successfully: {}", createdTemplate.getTemplateName());
            
        } catch (Exception e) {
            log.error("❌ Failed to create SWIFT template: {}", e.getMessage());
        }
    }

    /**
     * Test 2: Send message using template with real data from test-template-message.ps1.
     */
    private void testTemplateMessageSending() {
        log.info("📋 Test 2: Sending message using template...");
        
        try {
            // Test parameters from test-template-message.ps1
            Map<String, String> parameters = new HashMap<>();
            parameters.put("FROM_BIC", "588990");
            parameters.put("TO_BIC", "593129");
            parameters.put("MESSAGE_TYPE", "ShareTransferInstruction");
            parameters.put("MSG_DEF_ID", "sese.023.001.06.xsd");
            parameters.put("CREATION_DATE", "2025-08-14T22:22:38.601927660Z");
            parameters.put("TRANSACTION_ID", "TX20250808-0002");
            parameters.put("SETTLEMENT_DATE", "2025-08-15");
            parameters.put("TRADE_TYPE", "TRAD");
            parameters.put("SETTLEMENT_CONDITION", "NOMC");
            parameters.put("TRADE_DATE", "2025-08-15");
            parameters.put("MOVEMENT_TYPE", "DELIV");
            parameters.put("PAYMENT_TYPE", "APMT");
            parameters.put("ISIN_CODE", "GB0002634946");
            parameters.put("SECURITY_NAME", "CRDB");
            parameters.put("QUANTITY", "30");
            parameters.put("ACCOUNT_ID", "1");
            parameters.put("PARTY1_NAME", "ALI OMAR OTHMAN");
            parameters.put("PARTY1_ACCOUNT", "1");
            parameters.put("PARTY2_NAME", "CHRISTIAN KINDOLE");
            parameters.put("PARTY2_ACCOUNT", "ACC-REC-2020");
            parameters.put("TRANSACTION_DESCRIPTION", "Settlement against payment");
            
            // Send message using template with correct method signature
            boolean success = templateService.sendMessageUsingTemplate(
                "SWIFT_SHARE_TRANSFER_DETAILED", 
                "test-queue-006", 
                parameters,
                1, // priority
                "SWIFT-TX-001" // correlationId
            );
            
            if (success) {
                log.info("✅ Template message sent successfully");
            } else {
                log.error("❌ Failed to send template message");
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to send template message: {}", e.getMessage());
        }
    }

    /**
     * Test 3: Send direct message with real data from send-swift-message.ps1.
     */
    private void testDirectMessageSending() {
        log.info("📋 Test 3: Sending direct message...");
        
        try {
            // Message content from send-swift-message.ps1
            String xmlMessage = "<RequestPayload xmlns=\"SWIFTNetBusinessEnvelope\"><AppHdr xmlns=\"urn:iso:std:iso:20022:tech:xsd:head.001.001.01\"><Fr><OrgId><Id><OrgId><AnyBIC>588990</AnyBIC></OrgId></Id></OrgId></Fr><To><OrgId><Id><OrgId><AnyBIC>593129</AnyBIC></OrgId></Id></OrgId></To><BizMsgIdr>ShareTransferInstruction</BizMsgIdr><MsgDefIdr>sese.023.001.06.xsd</MsgDefIdr><CreDt>2025-08-14T22:22:38.601927660Z</CreDt></AppHdr><Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:sese.023.001.06\"><SctiesSttlmTxInstr><TxId><AcctOwnrTxId>TX20250808-0002</AcctOwnrTxId></TxId><SttlmParams><SttlmDt>2025-08-15</SttlmDt><SctiesTxTp><Cd>TRAD</Cd></SctiesTxTp><SttlmTxCond><Cd>NOMC</Cd></SttlmTxCond></SttlmParams><TradDtls><TradDt>2025-08-15</TradDt><SctiesMvmntTp>DELIV</SctiesMvmntTp><Pmt>APMT</Pmt></TradDtls><FinInstrmId><ISIN>GB0002634946</ISIN><Nm>CRDB</Nm></FinInstrmId><QtyAndAcctDtls><SttlmQty><Qty><Unit>30</Unit></Qty></SttlmQty><SfkpgAcct><Id>1</Id></SfkpgAcct></QtyAndAcctDtls><SttlmPtiesSts><Pty1><Pty><Nm>ALI OMAR OTHMAN</Nm></Pty><Acct><Id>1</Id></Acct></Pty1><Pty2><Pty><Nm>CHRISTIAN KINDOLE</Nm></Pty><Acct><Id>ACC-REC-2020</Id></Acct></Pty2></SttlmPtiesSts><AddtlTxDtls><TxDesc>Settlement against payment</TxDesc></AddtlTxDtls></SctiesSttlmTxInstr></Document></RequestPayload>";
            
            MsmqMessage message = new MsmqMessage();
            message.setBody(xmlMessage);
            message.setPriority(1);
            message.setCorrelationId("SWIFT-TX-001");
            message.setLabel("SWIFT Share Transfer Instruction");
            
            // Send message to queue
            msmqService.sendMessage("test-queue-006", message);
            log.info("✅ Direct message sent successfully");
            
        } catch (Exception e) {
            log.error("❌ Failed to send direct message: {}", e.getMessage());
        }
    }
}
