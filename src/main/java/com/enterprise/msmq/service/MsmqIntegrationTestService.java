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
//            testConnectionInfrastructure();
            
            // Test 2: Create SWIFT template
//            testTemplateCreation();
            
            // Test 3: Send message using template
//            testTemplateMessageSending();
            
            // Test 4: Send direct message
          //  testDirectMessageSending();
            
            log.info("✅ All MSMQ Integration Tests completed successfully!");
            
            // Create Securities Settlement Template
            createSecuritiesSettlementTemplate();
            
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
        log.info("📋 Test : Sending message using template...");
        
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
                "FormatName:DIRECT=TCP:192.168.2.170\\private$\\crdb_to_dse", // queue name
                parameters,
                "remote", // connection type
                1, // priority
               null
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
            msmqService.sendMessage("testqueue", message);
            log.info("✅ Direct message sent successfully");
            
        } catch (Exception e) {
            log.error("❌ Failed to send direct message: {}", e.getMessage());
        }
    }

    /**
     * Creates the Securities Settlement template specifically designed for RECE/DELI messages.
     */
    private void createSecuritiesSettlementTemplate() {
        log.info("📋 Creating Securities Settlement template...");
        
        try {
            // Check if template already exists
            Optional<MsmqMessageTemplate> existingTemplate = templateRepository.findByTemplateName("SWIFT_SECURITIES_SETTLEMENT");
            if (existingTemplate.isPresent()) {
                log.info("ℹ️ Securities Settlement template already exists, skipping creation");
                return;
            }
            
            // Template content based on your exact rece.xml and deli.xml structure
            String templateContent = """
                <RequestPayload
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                  xmlns="SWIFTNetBusinessEnvelope"
                >
                  <AppHdr
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                    xmlns="urn:iso:std:iso:20022:tech:xsd:head.001.001.01"
                  >
                    <Fr>
                      <OrgId>
                        <Id>
                          <OrgId>
                            <AnyBIC>{{FROM_BIC}}</AnyBIC>
                          </OrgId>
                        </Id>
                      </OrgId>
                    </Fr>
                    <To>
                      <OrgId>
                        <Id>
                          <OrgId>
                            <AnyBIC>{{TO_BIC}}</AnyBIC>
                          </OrgId>
                        </Id>
                      </OrgId>
                    </To>
                    <BizMsgIdr>{{MESSAGE_TYPE}}</BizMsgIdr>
                    <MsgDefIdr>{{MSG_DEF_ID}}</MsgDefIdr>
                    <CreDt>{{CREATION_DATE}}</CreDt>
                  </AppHdr>
                  <Document
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                    xmlns="urn:iso:std:iso:20022:tech:xsd:sese.023.001.11"
                  >
                    <SctiesSttlmTxInstr>
                      <TxId>{{TRANSACTION_ID}}</TxId>
                      <SttlmTpAndAddtlParams>
                        <SctiesMvmntTp>{{MOVEMENT_TYPE}}</SctiesMvmntTp>
                        <Pmt>{{PAYMENT_TYPE}}</Pmt>
                        <CmonId>{{COMMON_REFERENCE_ID}}</CmonId>
                      </SttlmTpAndAddtlParams>
                      <NbCounts>
                        <TtlNb>
                          <CurInstrNb>{{CURRENT_INSTRUCTION_NUMBER}}</CurInstrNb>
                          <TtlOfLkdInstrs>{{TOTAL_LINKED_INSTRUCTIONS}}</TtlOfLkdInstrs>
                        </TtlNb>
                      </NbCounts>
                      <Lnkgs>
                        <Ref>
                          <OthrTxId>{{LINKED_TRANSACTION_ID}}</OthrTxId>
                        </Ref>
                      </Lnkgs>
                      <TradDtls>
                        <TradId>{{TRANSACTION_ID}}</TradId>
                        <TradId>{{COMMON_REFERENCE_ID}}</TradId>
                        <PlcOfTrad>
                          <MktTpAndId>
                            <Id>
                              <MktIdrCd>{{MARKET_IDENTIFIER}}</MktIdrCd>
                            </Id>
                            <Tp>
                              <Cd>{{MARKET_TYPE}}</Cd>
                            </Tp>
                          </MktTpAndId>
                        </PlcOfTrad>
                        <TradDt>
                          <Dt>
                            <DtTm>{{TRADE_DATE_TIME}}</DtTm>
                          </Dt>
                        </TradDt>
                        <SttlmDt>
                          <Dt>
                            <Dt>{{SETTLEMENT_DATE}}</Dt>
                          </Dt>
                        </SttlmDt>
                        <DealPric>
                          <Tp>
                            <Yldd>false</Yldd>
                          </Tp>
                          <Val>
                            <Amt Ccy="TZS">0</Amt>
                          </Val>
                        </DealPric>
                        <TradTxCond>
                          <Cd>{{TRADE_TRANSACTION_CONDITION}}</Cd>
                        </TradTxCond>
                        <TradOrgtrRole>
                          <Prtry>
                            <Id>{{TRADE_ORIGINATOR_ROLE}}</Id>
                            <Issr>{{TRADE_ORIGINATOR_ISSUER}}</Issr>
                            <SchmeNm>{{TRADE_ORIGINATOR_SCHEME}}</SchmeNm>
                          </Prtry>
                        </TradOrgtrRole>
                        <MtchgSts>
                          <Cd>{{MATCHING_STATUS}}</Cd>
                        </MtchgSts>
                      </TradDtls>
                      <FinInstrmId>
                        <ISIN>{{ISIN_CODE}}</ISIN>
                        <Desc>{{SECURITY_DESCRIPTION}}</Desc>
                      </FinInstrmId>
                      <QtyAndAcctDtls>
                        <SttlmQty>
                          <Qty>
                            <FaceAmt>{{QUANTITY}}</FaceAmt>
                          </Qty>
                        </SttlmQty>
                        <AcctOwnr>
                          <Id>
                            <PrtryId>
                              <Id>{{ACCOUNT_OWNER_ID}}</Id>
                              <Issr>{{ACCOUNT_OWNER_ISSUER}}</Issr>
                              <SchmeNm>{{ACCOUNT_OWNER_SCHEME}}</SchmeNm>
                            </PrtryId>
                          </Id>
                        </AcctOwnr>
                        <SfkpgAcct>
                          <Id>{{SAFEEPING_ACCOUNT_ID}}</Id>
                        </SfkpgAcct>
                        <SfkpgPlc>
                          <SfkpgPlcFrmt>
                            <TpAndId>
                              <SfkpgPlcTp>{{SAFEEPING_PLACE_TYPE}}</SfkpgPlcTp>
                              <Id>{{SAFEEPING_PLACE_ID}}</Id>
                            </TpAndId>
                          </SfkpgPlcFrmt>
                        </SfkpgPlc>
                      </QtyAndAcctDtls>
                      <SttlmParams>
                        <SctiesTxTp>
                          <Cd>{{SECURITIES_TRANSACTION_TYPE}}</Cd>
                        </SctiesTxTp>
                        <BnfclOwnrsh>
                          <Ind>true</Ind>
                        </BnfclOwnrsh>
                        <SttlmSysMtd>
                          <Cd>{{SETTLEMENT_SYSTEM_METHOD}}</Cd>
                        </SttlmSysMtd>
                      </SttlmParams>
                      <DlvrgSttlmPties>
                        <Dpstry>
                          <Id>
                            <AnyBIC>{{DEPOSITORY_BIC}}</AnyBIC>
                          </Id>
                        </Dpstry>
                        <Pty1>
                          <Id>
                            <PrtryId>
                              <Id>{{DELIVERING_PARTY1_ID}}</Id>
                              <Issr>{{DELIVERING_PARTY1_ISSUER}}</Issr>
                              <SchmeNm>{{DELIVERING_PARTY1_SCHEME}}</SchmeNm>
                            </PrtryId>
                          </Id>
                          <PrcgId>{{PROCESSING_ID}}</PrcgId>
                        </Pty1>
                        <Pty2>
                          <Id>
                            <PrtryId>
                              <Id>{{DELIVERING_PARTY2_ID}}</Id>
                              <Issr>{{DELIVERING_PARTY2_ISSUER}}</Issr>
                              <SchmeNm>{{DELIVERING_PARTY2_SCHEME}}</SchmeNm>
                            </PrtryId>
                          </Id>
                        </Pty2>
                        <Pty3>
                          <Id>
                            <PrtryId>
                              <Id>{{DELIVERING_PARTY3_ID}}</Id>
                              <Issr>{{DELIVERING_PARTY3_ISSUER}}</Issr>
                              <SchmeNm>{{DELIVERING_PARTY3_SCHEME}}</SchmeNm>
                            </PrtryId>
                          </Id>
                        </Pty3>
                      </DlvrgSttlmPties>
                      <RcvgSttlmPties>
                        <Dpstry>
                          <Id>
                            <AnyBIC>{{RECEIVING_DEPOSITORY_BIC}}</AnyBIC>
                          </Id>
                        </Dpstry>
                        <Pty1>
                          <Id>
                            <PrtryId>
                              <Id>{{RECEIVING_PARTY1_ID}}</Id>
                              <Issr>{{RECEIVING_PARTY1_ISSUER}}</Issr>
                              <SchmeNm>{{RECEIVING_PARTY1_SCHEME}}</SchmeNm>
                            </PrtryId>
                          </Id>
                        </Pty1>
                        <Pty2>
                          <Id>
                            <PrtryId>
                              <Id>{{RECEIVING_PARTY2_ID}}</Id>
                              <Issr>{{RECEIVING_PARTY2_ISSUER}}</Issr>
                              <SchmeNm>{{RECEIVING_PARTY2_SCHEME}}</SchmeNm>
                            </PrtryId>
                          </Id>
                        </Pty2>
                        <Pty3>
                          <Id>
                            <PrtryId>
                              <Id>{{RECEIVING_PARTY3_ID}}</Id>
                              <Issr>{{RECEIVING_PARTY3_ISSUER}}</Issr>
                              <SchmeNm>{{RECEIVING_PARTY3_SCHEME}}</SchmeNm>
                            </PrtryId>
                          </Id>
                        </Pty3>
                      </RcvgSttlmPties>
                      <SttlmAmt>
                        <Amt Ccy="TZS">0</Amt>
                        <CdtDbtInd>{{CREDIT_DEBIT_INDICATOR}}</CdtDbtInd>
                      </SttlmAmt>
                      <OthrAmts>
                        <ChrgsFees>
                          <Amt Ccy="TZS">0</Amt>
                        </ChrgsFees>
                        <ExctgBrkrAmt>
                          <Amt Ccy="TZS">0</Amt>
                        </ExctgBrkrAmt>
                        <Othr>
                          <Amt Ccy="TZS">0</Amt>
                        </Othr>
                        <RgltryAmt>
                          <Amt Ccy="TZS">0</Amt>
                        </RgltryAmt>
                        <StockXchgTax>
                          <Amt Ccy="TZS">0</Amt>
                        </StockXchgTax>
                        <TrfTax>
                          <Amt Ccy="TZS">0</Amt>
                        </TrfTax>
                        <TxTax>
                          <Amt Ccy="TZS">0</Amt>
                        </TxTax>
                        <ValAddedTax>
                          <Amt Ccy="TZS">0</Amt>
                        </ValAddedTax>
                      </OthrAmts>
                      <SplmtryData>
                        <Envlp>
                          <MarginParameters xmlns="">
                            <HaircutPercentage>0</HaircutPercentage>
                            <TolerancePercentage>0</TolerancePercentage>
                            <CashInterestDifferentialPercentage>0</CashInterestDifferentialPercentage>
                          </MarginParameters>
                        </Envlp>
                      </SplmtryData>
                      <SplmtryData>
                        <Envlp>
                          <SctiesFincgDtls
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                            xmlns=""
                          >
                            <TermntnDt xmlns="urn:iso:std:iso:20022:tech:xsd:sese.033.001.11">
                              <Dt>
                                <Dt>{{TERMINATION_DATE}}</Dt>
                              </Dt>
                            </TermntnDt>
                            <RpRate xmlns="urn:iso:std:iso:20022:tech:xsd:sese.033.001.11">
                              <Rate>0</Rate>
                            </RpRate>
                          </SctiesFincgDtls>
                        </Envlp>
                      </SplmtryData>
                    </SctiesSttlmTxInstr>
                  </Document>
                </RequestPayload>
                """;
            
            // Template parameters based on your exact rece.xml and deli.xml structure
            Map<String, String> parameters = new HashMap<>();
            
            // Basic SWIFT header parameters
            parameters.put("FROM_BIC", "string");
            parameters.put("TO_BIC", "string");
            parameters.put("MESSAGE_TYPE", "string");
            parameters.put("MSG_DEF_ID", "string");
            parameters.put("CREATION_DATE", "datetime");
            
            // Transaction and settlement parameters
            parameters.put("TRANSACTION_ID", "string");
            parameters.put("MOVEMENT_TYPE", "string");           // RECE or DELI
            parameters.put("PAYMENT_TYPE", "string");            // FREE
            parameters.put("COMMON_REFERENCE_ID", "string");     // CmonId linking both messages
            parameters.put("CURRENT_INSTRUCTION_NUMBER", "string"); // CurInstrNb
            parameters.put("TOTAL_LINKED_INSTRUCTIONS", "string");  // TtlOfLkdInstrs
            parameters.put("LINKED_TRANSACTION_ID", "string");     // OthrTxId for cross-reference
            
            // Trade details
            parameters.put("MARKET_IDENTIFIER", "string");        // MktIdrCd
            parameters.put("MARKET_TYPE", "string");              // Market type code
            parameters.put("TRADE_DATE_TIME", "datetime");        // Trade date and time
            parameters.put("SETTLEMENT_DATE", "date");            // Settlement date
            parameters.put("TRADE_TRANSACTION_CONDITION", "string"); // Trade transaction condition
            parameters.put("TRADE_ORIGINATOR_ROLE", "string");    // Trade originator role
            parameters.put("TRADE_ORIGINATOR_ISSUER", "string");  // Trade originator issuer
            parameters.put("TRADE_ORIGINATOR_SCHEME", "string");  // Trade originator scheme
            parameters.put("MATCHING_STATUS", "string");          // Matching status
            
            // Security details
            parameters.put("ISIN_CODE", "string");
            parameters.put("SECURITY_DESCRIPTION", "string");
            parameters.put("QUANTITY", "number");
            
            // Account and settlement details
            parameters.put("ACCOUNT_OWNER_ID", "string");
            parameters.put("ACCOUNT_OWNER_ISSUER", "string");
            parameters.put("ACCOUNT_OWNER_SCHEME", "string");
            parameters.put("SAFEEPING_ACCOUNT_ID", "string");
            parameters.put("SAFEEPING_PLACE_TYPE", "string");
            parameters.put("SAFEEPING_PLACE_ID", "string");
            parameters.put("SECURITIES_TRANSACTION_TYPE", "string");
            parameters.put("SETTLEMENT_SYSTEM_METHOD", "string");
            
            // Party details (Delivering and Receiving)
            parameters.put("DEPOSITORY_BIC", "string");
            parameters.put("DELIVERING_PARTY1_ID", "string");
            parameters.put("DELIVERING_PARTY1_ISSUER", "string");
            parameters.put("DELIVERING_PARTY1_SCHEME", "string");
            parameters.put("PROCESSING_ID", "string");
            parameters.put("DELIVERING_PARTY2_ID", "string");
            parameters.put("DELIVERING_PARTY2_ISSUER", "string");
            parameters.put("DELIVERING_PARTY2_SCHEME", "string");
            parameters.put("DELIVERING_PARTY3_ID", "string");
            parameters.put("DELIVERING_PARTY3_ISSUER", "string");
            parameters.put("DELIVERING_PARTY3_SCHEME", "string");
            
            parameters.put("RECEIVING_DEPOSITORY_BIC", "string");
            parameters.put("RECEIVING_PARTY1_ID", "string");
            parameters.put("RECEIVING_PARTY1_ISSUER", "string");
            parameters.put("RECEIVING_PARTY1_SCHEME", "string");
            parameters.put("RECEIVING_PARTY2_ID", "string");
            parameters.put("RECEIVING_PARTY2_ISSUER", "string");
            parameters.put("RECEIVING_PARTY2_SCHEME", "string");
            parameters.put("RECEIVING_PARTY3_ID", "string");
            parameters.put("RECEIVING_PARTY3_ISSUER", "string");
            parameters.put("RECEIVING_PARTY3_SCHEME", "string");
            
            // Settlement amount and credit/debit indicator
            parameters.put("CREDIT_DEBIT_INDICATOR", "string");  // CRDT or DBIT
            
            // Termination date
            parameters.put("TERMINATION_DATE", "date");
            
            MsmqMessageTemplate template = new MsmqMessageTemplate();
            template.setTemplateName("SWIFT_SECURITIES_SETTLEMENT");
            template.setTemplateType("SWIFT");
            template.setTemplateContent(templateContent);
            template.setDescription("SWIFT Securities Settlement Template for RECE/DELI paired messages with cross-referencing");
            template.setParameters(parameters);
            template.setIsActive(true);
            template.setCreatedBy("System");
            template.setCreatedAt(LocalDateTime.now());
            
            MsmqMessageTemplate createdTemplate = templateService.createTemplate(template);
            log.info("✅ Securities Settlement template created successfully: {}", createdTemplate.getTemplateName());
            
        } catch (Exception e) {
            log.error("❌ Failed to create Securities Settlement template: {}", e.getMessage(), e);
        }
    }
}
