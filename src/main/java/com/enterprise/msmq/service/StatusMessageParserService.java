package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.StatusMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for parsing SWIFT status messages from MSMQ queues.
 * Handles XML parsing and extraction of key tracking information.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatusMessageParserService {
    
    private static final DateTimeFormatter SWIFT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter SWIFT_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'");
    
    /**
     * Parse a SWIFT status message from XML content.
     * 
     * @param xmlContent the raw XML message content
     * @param sourceQueue the queue name where the message was received
     * @return parsed StatusMessageDto or null if parsing fails
     */
    public StatusMessageDto parseStatusMessage(String xmlContent, String sourceQueue) {
        try {
            log.debug("Parsing status message from queue: {}", sourceQueue);
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            Document document = builder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));
            
            StatusMessageDto.StatusMessageDtoBuilder dtoBuilder = StatusMessageDto.builder()
                    .rawMessageContent(xmlContent)
                    .receivedAt(LocalDateTime.now())
                    .sourceQueue(sourceQueue);
            
            // Parse message header
            parseMessageHeader(document, dtoBuilder);
            
            // Parse transaction identification
            parseTransactionIdentification(document, dtoBuilder);
            
            // Parse status information
            parseStatusInformation(document, dtoBuilder);
            
            // Parse transaction details
            parseTransactionDetails(document, dtoBuilder);
            
            // Parse settlement parties
            parseSettlementParties(document, dtoBuilder);
            
            StatusMessageDto result = dtoBuilder.build();
            log.info("Successfully parsed status message - Common ID: {}, Status: {}, Movement Type: {}", 
                    result.getCommonId(), result.getProcessingStatus(), result.getSecuritiesMovementType());
            
            return result;
            
        } catch (Exception e) {
            log.error("Failed to parse status message from queue: {}", sourceQueue, e);
            return null;
        }
    }
    
    private void parseMessageHeader(Document document, StatusMessageDto.StatusMessageDtoBuilder builder) {
        try {
            // Parse AppHdr section
            NodeList appHdrList = document.getElementsByTagName("AppHdr");
            if (appHdrList.getLength() > 0) {
                Element appHdr = (Element) appHdrList.item(0);
                
                // Business Message ID
                String bizMsgIdr = getElementText(appHdr, "BizMsgIdr");
                builder.businessMessageId(bizMsgIdr);
                
                // Message Definition ID
                String msgDefIdr = getElementText(appHdr, "MsgDefIdr");
                builder.messageDefinitionId(msgDefIdr);
                
                // Creation Date
                String creDt = getElementText(appHdr, "CreDt");
                if (creDt != null && !creDt.isEmpty()) {
                    try {
                        builder.creationDate(LocalDateTime.parse(creDt, SWIFT_DATETIME_FORMAT));
                    } catch (Exception e) {
                        log.warn("Failed to parse creation date: {}", creDt);
                    }
                }
                
                // From BIC
                String fromBic = getElementText(appHdr, "Fr/OrgId/Id/OrgId/AnyBIC");
                builder.fromBic(fromBic);
                
                // To BIC
                String toBic = getElementText(appHdr, "To/OrgId/Id/OrgId/AnyBIC");
                builder.toBic(toBic);
            }
        } catch (Exception e) {
            log.warn("Failed to parse message header", e);
        }
    }
    
    private void parseTransactionIdentification(Document document, StatusMessageDto.StatusMessageDtoBuilder builder) {
        try {
            NodeList txIdList = document.getElementsByTagName("TxId");
            if (txIdList.getLength() > 0) {
                Element txId = (Element) txIdList.item(0);
                
                String acctOwnrTxId = getElementText(txId, "AcctOwnrTxId");
                builder.accountOwnerTransactionId(acctOwnrTxId);
                
                String acctSvcrTxId = getElementText(txId, "AcctSvcrTxId");
                builder.accountServicerTransactionId(acctSvcrTxId);
                
                String cmonId = getElementText(txId, "CmonId");
                builder.commonId(cmonId);
            }
            
            // Parse Trade IDs
            NodeList tradIdList = document.getElementsByTagName("TradId");
            List<String> tradeIds = new ArrayList<>();
            for (int i = 0; i < tradIdList.getLength(); i++) {
                String tradeId = tradIdList.item(i).getTextContent();
                if (tradeId != null && !tradeId.trim().isEmpty()) {
                    tradeIds.add(tradeId.trim());
                }
            }
            builder.tradeIds(tradeIds);
            
        } catch (Exception e) {
            log.warn("Failed to parse transaction identification", e);
        }
    }
    
    private void parseStatusInformation(Document document, StatusMessageDto.StatusMessageDtoBuilder builder) {
        try {
            NodeList prcgStsList = document.getElementsByTagName("PrcgSts");
            if (prcgStsList.getLength() > 0) {
                Element prcgSts = (Element) prcgStsList.item(0);
                
                String statusId = getElementText(prcgSts, "Prtry/PrtrySts/Id");
                builder.statusCode(statusId);
                
                String statusIssuer = getElementText(prcgSts, "Prtry/PrtrySts/Issr");
                builder.statusIssuer(statusIssuer);
                
                String additionalReason = getElementText(prcgSts, "Prtry/PrtryRsn/AddtlRsnInf");
                builder.additionalReasonInfo(additionalReason);
                
                // Map status code to processing status
                String processingStatus = mapStatusCodeToProcessingStatus(statusId, additionalReason);
                builder.processingStatus(processingStatus);
            }
        } catch (Exception e) {
            log.warn("Failed to parse status information", e);
        }
    }
    
    private void parseTransactionDetails(Document document, StatusMessageDto.StatusMessageDtoBuilder builder) {
        try {
            NodeList txDtlsList = document.getElementsByTagName("TxDtls");
            if (txDtlsList.getLength() > 0) {
                Element txDtls = (Element) txDtlsList.item(0);
                
                // Account Owner
                String acctOwnrId = getElementText(txDtls, "AcctOwnr/PrtryId/Id");
                builder.accountOwnerId(acctOwnrId);
                
                String acctOwnrIssuer = getElementText(txDtls, "AcctOwnr/PrtryId/Issr");
                builder.accountOwnerIssuer(acctOwnrIssuer);
                
                String acctOwnrScheme = getElementText(txDtls, "AcctOwnr/PrtryId/SchmeNm");
                builder.accountOwnerScheme(acctOwnrScheme);
                
                // Safekeeping Account
                String sfkpgAcctId = getElementText(txDtls, "SfkpgAcct/Id");
                builder.safekeepingAccountId(sfkpgAcctId);
                
                String sfkpgAcctNm = getElementText(txDtls, "SfkpgAcct/Nm");
                builder.safekeepingAccountName(sfkpgAcctNm);
                
                // Financial Instrument
                String isin = getElementText(txDtls, "FinInstrmId/ISIN");
                builder.isin(isin);
                
                String instrumentDesc = getElementText(txDtls, "FinInstrmId/Desc");
                builder.instrumentDescription(instrumentDesc);
                
                // Settlement Quantity
                String sttlmQty = getElementText(txDtls, "SttlmQty/Qty/Unit");
                if (sttlmQty != null && !sttlmQty.isEmpty()) {
                    try {
                        builder.settlementQuantity(Long.parseLong(sttlmQty));
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse settlement quantity: {}", sttlmQty);
                    }
                }
                
                // Settlement Amount
                String sttlmAmt = getElementText(txDtls, "SttlmAmt/Amt");
                builder.settlementAmount(sttlmAmt);
                
                String currency = getElementText(txDtls, "SttlmAmt/Amt");
                if (currency != null && currency.contains("Ccy=")) {
                    currency = currency.substring(currency.indexOf("Ccy=\"") + 5);
                    currency = currency.substring(0, currency.indexOf("\""));
                    builder.currency(currency);
                }
                
                String cdtDbtInd = getElementText(txDtls, "SttlmAmt/CdtDbtInd");
                builder.creditDebitIndicator(cdtDbtInd);
                
                // Dates
                String xpctdSttlmDt = getElementText(txDtls, "XpctdSttlmDt/Dt");
                if (xpctdSttlmDt != null && !xpctdSttlmDt.isEmpty()) {
                    try {
                        builder.expectedSettlementDate(LocalDateTime.parse(xpctdSttlmDt + "T00:00:00"));
                    } catch (Exception e) {
                        log.warn("Failed to parse expected settlement date: {}", xpctdSttlmDt);
                    }
                }
                
                String xpctdValDt = getElementText(txDtls, "XpctdValDt/Dt");
                if (xpctdValDt != null && !xpctdValDt.isEmpty()) {
                    try {
                        builder.expectedValueDate(LocalDateTime.parse(xpctdValDt + "T00:00:00"));
                    } catch (Exception e) {
                        log.warn("Failed to parse expected value date: {}", xpctdValDt);
                    }
                }
                
                String sttlmDt = getElementText(txDtls, "SttlmDt/Dt/Dt");
                if (sttlmDt != null && !sttlmDt.isEmpty()) {
                    try {
                        builder.settlementDate(LocalDateTime.parse(sttlmDt + "T00:00:00"));
                    } catch (Exception e) {
                        log.warn("Failed to parse settlement date: {}", sttlmDt);
                    }
                }
                
                String tradDt = getElementText(txDtls, "TradDt/Dt/Dt");
                if (tradDt != null && !tradDt.isEmpty()) {
                    try {
                        builder.tradeDate(LocalDateTime.parse(tradDt + "T00:00:00"));
                    } catch (Exception e) {
                        log.warn("Failed to parse trade date: {}", tradDt);
                    }
                }
                
                // Securities Movement Type
                String sctiesMvmntTp = getElementText(txDtls, "SctiesMvmntTp");
                builder.securitiesMovementType(sctiesMvmntTp);
                
                // Payment Type
                String pmt = getElementText(txDtls, "Pmt");
                builder.paymentType(pmt);
            }
        } catch (Exception e) {
            log.warn("Failed to parse transaction details", e);
        }
    }
    
    private void parseSettlementParties(Document document, StatusMessageDto.StatusMessageDtoBuilder builder) {
        try {
            // Receiving Settlement Parties
            String receivingDepository = getElementText(document, "RcvgSttlmPties/Dpstry/Id/AnyBIC");
            builder.receivingDepository(receivingDepository);
            
            String receivingProcessingId = getElementText(document, "RcvgSttlmPties/Dpstry/PrcgId");
            builder.receivingProcessingId(receivingProcessingId);
            
            // Delivering Settlement Parties
            String deliveringDepository = getElementText(document, "DlvrgSttlmPties/Dpstry/Id/AnyBIC");
            builder.deliveringDepository(deliveringDepository);
            
            String deliveringProcessingId = getElementText(document, "DlvrgSttlmPties/Dpstry/PrcgId");
            builder.deliveringProcessingId(deliveringProcessingId);
            
        } catch (Exception e) {
            log.warn("Failed to parse settlement parties", e);
        }
    }
    
    private String getElementText(Element parent, String xpath) {
        try {
            String[] parts = xpath.split("/");
            Element current = parent;
            
            for (int i = 0; i < parts.length - 1; i++) {
                NodeList children = current.getElementsByTagName(parts[i]);
                if (children.getLength() > 0) {
                    current = (Element) children.item(0);
                } else {
                    return null;
                }
            }
            
            NodeList children = current.getElementsByTagName(parts[parts.length - 1]);
            if (children.getLength() > 0) {
                return children.item(0).getTextContent();
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private String getElementText(Document document, String xpath) {
        try {
            String[] parts = xpath.split("/");
            Element current = document.getDocumentElement();
            
            for (String part : parts) {
                NodeList children = current.getElementsByTagName(part);
                if (children.getLength() > 0) {
                    current = (Element) children.item(0);
                } else {
                    return null;
                }
            }
            
            return current.getTextContent();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Map SWIFT status codes to internal processing status.
     * 
     * @param statusCode the SWIFT status code
     * @param additionalReason additional reason information
     * @return mapped processing status
     */
    private String mapStatusCodeToProcessingStatus(String statusCode, String additionalReason) {
        if (statusCode == null) {
            return "UNKNOWN";
        }
        
        switch (statusCode) {
            case "0201":
                return "MATCHED";
            case "0202":
                return "SETTLED";
            case "0203":
                return "FAILED";
            case "0204":
                return "CANCELLED";
            case "0205":
                return "PENDING";
            case "0206":
                return "REJECTED";
            default:
                log.warn("Unknown status code: {} with reason: {}", statusCode, additionalReason);
                return "UNKNOWN";
        }
    }
}
