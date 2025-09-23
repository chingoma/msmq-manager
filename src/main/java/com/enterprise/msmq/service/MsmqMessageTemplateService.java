package com.enterprise.msmq.service;

import com.enterprise.msmq.entity.MsmqMessageTemplate;
import com.enterprise.msmq.repository.MsmqMessageTemplateRepository;
import com.enterprise.msmq.factory.MsmqQueueManagerFactory;
import com.enterprise.msmq.service.contracts.IMsmqQueueManager;
import com.enterprise.msmq.dto.MsmqMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.StringWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/**
 * Service for managing MSMQ Message Templates.
 */
@Service
@RequiredArgsConstructor
public class MsmqMessageTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(MsmqMessageTemplateService.class);
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    private final MsmqMessageTemplateRepository templateRepository;
    private final MsmqQueueManagerFactory queueManagerFactory;
    private final MessageStatusService messageStatusService;

    /**
     * Create a new message template.
     */
    public MsmqMessageTemplate createTemplate(MsmqMessageTemplate template) {
        if (templateRepository.existsByTemplateName(template.getTemplateName())) {
            throw new IllegalArgumentException("Template name already exists: " + template.getTemplateName());
        }
        
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        template.setIsActive(true);
        
        MsmqMessageTemplate savedTemplate = templateRepository.save(template);
        logger.info("Created message template: {}", savedTemplate.getTemplateName());
        return savedTemplate;
    }

    /**
     * Get all active templates.
     */
    public List<MsmqMessageTemplate> getAllActiveTemplates() {
        return templateRepository.findByIsActiveTrue();
    }

    /**
     * Get template by name.
     */
    public Optional<MsmqMessageTemplate> getTemplateByName(String templateName) {
        return templateRepository.findByTemplateName(templateName);
    }

    /**
     * Get templates by type.
     */
    public List<MsmqMessageTemplate> getTemplatesByType(String templateType) {
        return templateRepository.findByTemplateTypeAndIsActiveTrue(templateType);
    }

    /**
     * Search templates by name.
     */
    public List<MsmqMessageTemplate> searchTemplatesByName(String name) {
        return templateRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Update an existing template.
     */
    public MsmqMessageTemplate updateTemplate(Long id, MsmqMessageTemplate template) {
        MsmqMessageTemplate existingTemplate = templateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found with id: " + id));
        
        existingTemplate.setTemplateContent(template.getTemplateContent());
        existingTemplate.setDescription(template.getDescription());
        existingTemplate.setParameters(template.getParameters());
        existingTemplate.setTemplateType(template.getTemplateType());
        existingTemplate.setUpdatedAt(LocalDateTime.now());
        existingTemplate.setUpdatedBy(template.getUpdatedBy());
        
        MsmqMessageTemplate updatedTemplate = templateRepository.save(existingTemplate);
        logger.info("Updated message template: {}", updatedTemplate.getTemplateName());
        return updatedTemplate;
    }

    /**
     * Delete a template (soft delete).
     */
    public void deleteTemplate(Long id) {
        MsmqMessageTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Template not found with id: " + id));
        
        template.setIsActive(false);
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.save(template);
        
        logger.info("Deleted message template: {}", template.getTemplateName());
    }

    /**
     * Send message using template and parameters with tracking information.
     */
    public boolean sendMessageUsingTemplate(String templateName, String queueName, Map<String, String> parameters, String environment, Integer priority, String correlationId, String transactionId, String movementType, String linkedTransactionId) {
        MsmqMessageTemplate template = templateRepository.findByTemplateName(templateName)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateName));
        
        if (!template.getIsActive()) {
            throw new IllegalArgumentException("Template is not active: " + templateName);
        }
        
        // Merge template with parameters
        String mergedContent = mergeTemplateWithParameters(template.getTemplateContent(), parameters);
        // Focus on proper XML prettification
        String prettyXml = prettyFormatXml(mergedContent);
        // Create MsmqMessage object
        MsmqMessage message = new MsmqMessage();
        message.setBody(prettyXml);
        message.setPriority(priority != null ? priority : 1);
        message.setCorrelationId(correlationId);
        message.setLabel("Template Message: " + templateName);
        
        // Send message to MSMQ
        try {
            IMsmqQueueManager queueManager = queueManagerFactory.createQueueManager();
            boolean messageSent;

            if(environment != null) {
                if(environment.equalsIgnoreCase("remote")) {
                    // For remote, check if queueName is already a FormatName path
                    if (queueName.toUpperCase().startsWith("FORMATNAME:")) {
                        messageSent = queueManager.sendMessageToRemote(queueName, message);
                    } else {
                        // Use two-parameter method for simple queue names
                        messageSent = queueManager.sendMessageToRemote("192.168.2.170", queueName, message);
                    }
                } else if(environment.equalsIgnoreCase("local")) {
                    messageSent = queueManager.sendMessage(queueName, message);
                } else {
                    throw new IllegalArgumentException("Invalid environment: " + environment);
                }
            } else {
                // Default to remote if environment not specified
                // Check if queueName is already a FormatName path
                if (queueName.toUpperCase().startsWith("FORMATNAME:")) {
                    messageSent = queueManager.sendMessageToRemote(queueName, message);
                } else {
                    // Use two-parameter method for simple queue names
                    messageSent = queueManager.sendMessageToRemote("192.168.2.170", queueName, message);
                }
            }

            if (messageSent) {
                // Store message in database for status tracking with full tracking info
                com.enterprise.msmq.entity.MsmqMessage entityMessage = com.enterprise.msmq.entity.MsmqMessage.builder()
                        .messageId(UUID.randomUUID().toString())
                        .queueName(queueName)
                        .correlationId(correlationId)
                        .label(message.getLabel())
                        .body(message.getBody())
                        .priority(message.getPriority())
                        .messageSize((long) message.getBody().getBytes().length)
                        .environment(environment)
                        .templateName(templateName)
                        .commonReferenceId(correlationId)  // Set common reference ID
                        .transactionId(transactionId)
                        .movementType(movementType)
                        .linkedTransactionId(linkedTransactionId)
                        .build();
                messageStatusService.storeMessage(entityMessage);
                
                logger.info("Successfully sent message using template: {} to queue: {}", templateName, queueName);
                return true;
            } else {
                logger.error("Failed to send message using template: {} to queue: {}", templateName, queueName);
                return false;
            }
        } catch (Exception e) {
            logger.error("Failed to send message using template: {} to queue: {}", templateName, queueName, e);
            return false;
        }
    }

    /**
     * Send message using template and parameters.
     */
    public boolean sendMessageUsingTemplate(String templateName, String queueName, Map<String, String> parameters, String environment, Integer priority, String correlationId) {
        MsmqMessageTemplate template = templateRepository.findByTemplateName(templateName)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateName));
        
        if (!template.getIsActive()) {
            throw new IllegalArgumentException("Template is not active: " + templateName);
        }
        
        // Merge template with parameters
        String mergedContent = mergeTemplateWithParameters(template.getTemplateContent(), parameters);
        // Focus on proper XML prettification
        String prettyXml = prettyFormatXml(mergedContent);
        // Create MsmqMessage object
        MsmqMessage message = new MsmqMessage();
        message.setBody(prettyXml);
        message.setPriority(priority != null ? priority : 1);
        message.setCorrelationId(correlationId);
        message.setLabel("Template Message: " + templateName);
        
        // Send message to MSMQ
        try {
            IMsmqQueueManager queueManager = queueManagerFactory.createQueueManager();
            boolean messageSent;

            if(environment != null) {
                if(environment.equalsIgnoreCase("remote")) {
                    // For remote, check if queueName is already a FormatName path
                    if (queueName.toUpperCase().startsWith("FORMATNAME:")) {
                        messageSent = queueManager.sendMessageToRemote(queueName, message);
                    } else {
                        // Use two-parameter method for simple queue names
                        messageSent = queueManager.sendMessageToRemote("192.168.2.170", queueName, message);
                    }
                } else if(environment.equalsIgnoreCase("local")) {
                    messageSent = queueManager.sendMessage(queueName, message);
                } else {
                    throw new IllegalArgumentException("Invalid environment: " + environment);
                }
            } else {
                // Default to remote if environment not specified
                // Check if queueName is already a FormatName path
                if (queueName.toUpperCase().startsWith("FORMATNAME:")) {
                    messageSent = queueManager.sendMessageToRemote(queueName, message);
                } else {
                    // Use two-parameter method for simple queue names
                    messageSent = queueManager.sendMessageToRemote("192.168.2.170", queueName, message);
                }
            }

            if (messageSent) {
                // Store message in database for status tracking
                com.enterprise.msmq.entity.MsmqMessage entityMessage = com.enterprise.msmq.entity.MsmqMessage.builder()
                        .messageId(UUID.randomUUID().toString())
                        .queueName(queueName)
                        .correlationId(message.getCorrelationId())
                        .label(message.getLabel())
                        .body(message.getBody())
                        .priority(message.getPriority())
                        .messageSize((long) message.getBody().getBytes().length)
                        .environment(environment)
                        .templateName(templateName)
                        .build();
                messageStatusService.storeMessage(entityMessage);
                
                logger.info("Successfully sent message using template: {} to queue: {}", templateName, queueName);
                return true;
            } else {
                logger.error("Failed to send message using template: {} to queue: {}", templateName, queueName);
                return false;
            }
        } catch (Exception e) {
            logger.error("Failed to send message using template: {} to queue: {}", templateName, queueName, e);
            return false;
        }
    }

    /**
     * Merge template content with parameters.
     */
    private String mergeTemplateWithParameters(String templateContent, Map<String, String> parameters) {
        String mergedContent = templateContent;
        Matcher matcher = TEMPLATE_PATTERN.matcher(templateContent);
        
        while (matcher.find()) {
            String placeholder = matcher.group(0); // {{PARAM_NAME}}
            String paramName = matcher.group(1);   // PARAM_NAME
            String paramValue = parameters.get(paramName);
            
            if (paramValue == null) {
                // Handle special cases
                if ("TIMESTAMP".equals(paramName) || "CREATION_DATE".equals(paramName)) {
                    paramValue = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
                } else if ("CURRENT_DATE".equals(paramName) || "SETTLEMENT_DATE".equals(paramName) || "TRADE_DATE".equals(paramName)) {
                    paramValue = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);
                } else {
                    logger.warn("Parameter not found: {}, using empty string", paramName);
                    paramValue = "";
                }
            }
            
            mergedContent = mergedContent.replace(placeholder, paramValue);
        }
        
        return mergedContent;
    }

    /**
     * Simple, focused XML prettification - clean formatting with proper indentation.
     */
    public static String prettyFormatXml(String xml) {
        try {
            // Parse the XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

            // Create transformer for pretty printing
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            
            // Set properties for clean, readable output
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            
            // Transform to string
            StringWriter writer = new StringWriter();
            transformer.transform(new javax.xml.transform.dom.DOMSource(document), new StreamResult(writer));
            
            String result = writer.toString();
            
            // Clean up the result
            result = result.trim();
            
            logger.debug("Successfully formatted XML with proper indentation");
            return result;
            
        } catch (Exception e) {
            logger.warn("Failed to format XML, using original: {}", e.getMessage());
            return xml;
        }
    }

    /**
     * Format XML specifically for parser compatibility using aggressive transformation approaches.
     */
    public static String formatXmlForParserCompatibility(String xml) {
        // Try different transformation approaches in order of aggressiveness
        String[] approaches = {
            "CDATA_WRAP",           // Wrap content in CDATA
            "ESCAPE_SPECIAL_CHARS", // Escape all special characters
            "REMOVE_NAMESPACES",    // Remove all namespace declarations
            "SIMPLE_STRUCTURE",     // Simplify XML structure
            "UTF8_BOM",            // Add UTF-8 BOM
            "WINDOWS_ENCODING",     // Use Windows-specific encoding
            "RAW_NO_DECLARATION"    // Remove XML declaration entirely
        };
        
        for (String approach : approaches) {
            try {
                String transformed = switch (approach) {
                    case "CDATA_WRAP" -> wrapInCData(xml);
                    case "ESCAPE_SPECIAL_CHARS" -> escapeSpecialCharacters(xml);
                    case "REMOVE_NAMESPACES" -> removeNamespaces(xml);
                    case "SIMPLE_STRUCTURE" -> simplifyXmlStructure(xml);
                    case "UTF8_BOM" -> addUtf8Bom(xml);
                    case "WINDOWS_ENCODING" -> useWindowsEncoding(xml);
                    case "RAW_NO_DECLARATION" -> removeXmlDeclaration(xml);
                    default -> xml;
                };
                
                logger.debug("Trying XML transformation approach: {}", approach);
                
                // For now, return the first attempt to test
                if ("REMOVE_NAMESPACES".equals(approach)) {
                    logger.info("Using XML transformation approach: {}", approach);
                    return transformed;
                }
                
            } catch (Exception e) {
                logger.debug("XML transformation approach '{}' failed: {}", approach, e.getMessage());
            }
        }
        
        logger.warn("All XML transformation approaches failed, using original");
        return xml;
    }
    
    /**
     * Remove all namespace declarations and prefixes - some parsers prefer simple XML.
     */
    private static String removeNamespaces(String xml) {
        try {
            String cleaned = xml;
            
            // Remove namespace declarations
            cleaned = cleaned.replaceAll("\\s+xmlns[^=]*=\"[^\"]*\"", "");
            cleaned = cleaned.replaceAll("\\s+xmlns=\"[^\"]*\"", "");
            
            // Remove namespace prefixes from elements
            cleaned = cleaned.replaceAll("<[a-zA-Z0-9]+:", "<");
            cleaned = cleaned.replaceAll("</[a-zA-Z0-9]+:", "</");
            
            // Add basic XML declaration
            if (!cleaned.trim().startsWith("<?xml")) {
                cleaned = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + cleaned.trim();
            }
            
            return cleaned;
        } catch (Exception e) {
            return xml;
        }
    }
    
    /**
     * Wrap XML content in CDATA to avoid parsing issues.
     */
    private static String wrapInCData(String xml) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Message><![CDATA[" + xml + "]]></Message>";
    }
    
    /**
     * Escape special characters that might cause parsing issues.
     */
    private static String escapeSpecialCharacters(String xml) {
        String escaped = xml;
        escaped = escaped.replace("&", "&amp;");
        escaped = escaped.replace("<", "&lt;");
        escaped = escaped.replace(">", "&gt;");
        escaped = escaped.replace("\"", "&quot;");
        escaped = escaped.replace("'", "&apos;");
        return escaped;
    }
    
    /**
     * Simplify XML structure by removing complex nested elements.
     */
    private static String simplifyXmlStructure(String xml) {
        // This is a placeholder - would need specific SWIFT simplification rules
        return xml;
    }
    
    /**
     * Add UTF-8 BOM to help with encoding detection.
     */
    private static String addUtf8Bom(String xml) {
        return "\uFEFF" + xml;
    }
    
    /**
     * Use Windows-specific encoding and line endings.
     */
    private static String useWindowsEncoding(String xml) {
        String windows = xml.replaceAll("\\n", "\\r\\n");
        if (!windows.startsWith("<?xml")) {
            windows = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\r\n" + windows;
        }
        return windows;
    }
    
    /**
     * Remove XML declaration entirely - some parsers prefer this.
     */
    private static String removeXmlDeclaration(String xml) {
        return xml.replaceAll("<\\?xml[^>]*\\?>\\s*", "").trim();
    }

    /**
     * Format XML for maximum parser compatibility.
     * Tries multiple formatting approaches to ensure the XML can be parsed by receiving systems.
     * @param xml the raw XML string
     * @return properly formatted XML string optimized for parser compatibility
     */
    public static String prettyPrintXml(String xml) {
        // Try multiple formatting approaches in order of compatibility
        String[] formattingApproaches = {
            "NO_PROCESSING",      // Send completely unprocessed - bypass prettyPrintXml entirely
            "RAW_TEMPLATE",       // Send template as-is without any XML processing
            "SWIFT_COMPATIBLE",   // SWIFT-specific formatting
            "NO_NAMESPACE_PREFIX", // Remove namespace prefixes that might cause issues
            "MINIMAL_CLEAN",      // Most conservative - minimal changes
            "COMPACT_FORMAT",     // Single line format (some parsers prefer this)
            "STANDARD_FORMAT",    // Standard pretty-printing
            "COMPATIBILITY_MODE", // Maximum compatibility settings
            "AS_IS"              // Last resort - send unchanged
        };
        
        for (String approach : formattingApproaches) {
            try {
                String formattedXml = switch (approach) {
                    case "NO_PROCESSING" -> xml; // Completely unprocessed
                    case "RAW_TEMPLATE" -> xml.trim(); // No processing at all
                    case "SWIFT_COMPATIBLE" -> formatXmlForSwift(xml);
                    case "NO_NAMESPACE_PREFIX" -> formatXmlWithoutNamespacePrefixes(xml);
                    case "MINIMAL_CLEAN" -> formatXmlMinimalClean(xml);
                    case "COMPACT_FORMAT" -> formatXmlCompact(xml);
                    case "STANDARD_FORMAT" -> formatXmlStandard(xml);
                    case "COMPATIBILITY_MODE" -> formatXmlForCompatibility(xml);
                    case "AS_IS" -> xml;
                    default -> xml;
                };
                
                // Validate each formatted version
                if (validateXmlStructure(formattedXml)) {
                    logger.debug("Successfully formatted XML using approach: {}", approach);
                    return formattedXml;
                }
                
            } catch (Exception e) {
                logger.debug("XML formatting approach '{}' failed: {}", approach, e.getMessage());
            }
        }
        
        logger.warn("All XML formatting approaches failed, sending original");
        return xml;
    }
    
    /**
     * Minimal clean formatting - just fix basic issues without heavy processing.
     */
    private static String formatXmlMinimalClean(String xml) throws Exception {
        // Just clean up the XML without heavy DOM processing
        String cleaned = xml.trim();
        
        // Ensure proper XML declaration format
        if (!cleaned.startsWith("<?xml")) {
            cleaned = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + cleaned;
        }
        
        // Remove any BOM (Byte Order Mark) characters that might cause parsing issues
        cleaned = cleaned.replace("\uFEFF", "");
        
        // Normalize line endings to Unix style (more compatible)
        cleaned = cleaned.replaceAll("\\r\\n", "\\n").replaceAll("\\r", "\\n");
        
        // Remove any trailing whitespace that might cause issues
        cleaned = cleaned.replaceAll("\\s+$", "");
        
        return cleaned;
    }
    
    /**
     * SWIFT-specific XML formatting - optimized for SWIFT message processing.
     */
    private static String formatXmlForSwift(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        
        StringWriter stringWriter = new StringWriter();
        transformer.transform(new javax.xml.transform.dom.DOMSource(doc), new StreamResult(stringWriter));
        String result = stringWriter.toString();
        
        // SWIFT-specific cleaning
        result = result.trim();
        
        // Ensure consistent line endings (CRLF for Windows systems)
        result = result.replaceAll("\\n", "\\r\\n");
        
        // Fix any double line endings
        result = result.replaceAll("\\r\\r\\n", "\\r\\n");
        
        return result;
    }
    
    /**
     * Format XML without namespace prefixes that might confuse parsers.
     */
    private static String formatXmlWithoutNamespacePrefixes(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false); // Disable namespace awareness to simplify
        
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        
        StringWriter stringWriter = new StringWriter();
        transformer.transform(new javax.xml.transform.dom.DOMSource(doc), new StreamResult(stringWriter));
        
        return stringWriter.toString().trim();
    }
    
    /**
     * Compact XML formatting - single line with proper declaration.
     */
    private static String formatXmlCompact(String xml) throws Exception {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            doc.normalizeDocument();

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

            StringWriter stringWriter = new StringWriter();
            transformer.transform(new javax.xml.transform.dom.DOMSource(doc), new StreamResult(stringWriter));
        String result = stringWriter.toString();

        // Remove extra whitespace between tags but keep the XML declaration
        result = result.replaceAll(">\\s+<", "><");
        result = result.replaceAll("\\r?\\n", "");
        
        return result.trim();
    }
    
    /**
     * Format XML with maximum compatibility for SWIFT/MSMQ systems.
     */
    private static String formatXmlForCompatibility(String xml) throws Exception {
        // Parse XML into DOM with strict settings
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        dbf.setExpandEntityReferences(false);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        doc.normalizeDocument();

        // Use transformer with conservative settings for maximum compatibility
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
        
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter stringWriter = new StringWriter();
        transformer.transform(new javax.xml.transform.dom.DOMSource(doc), new StreamResult(stringWriter));
        
        String result = stringWriter.toString();
        
        // Clean up any problematic characters or formatting
        result = result.trim();
        
        // Ensure proper line endings (use \n for Unix-style, which is more compatible)
        result = result.replaceAll("\\r\\n", "\\n").replaceAll("\\r", "\\n");
        
        return result;
    }
    
    /**
     * Standard XML formatting as fallback.
     */
    private static String formatXmlStandard(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        
        StringWriter stringWriter = new StringWriter();
        transformer.transform(new javax.xml.transform.dom.DOMSource(doc), new StreamResult(stringWriter));
        
        return stringWriter.toString().trim();
    }
    
    /**
     * Validate that the XML structure is correct and parseable.
     */
    private static boolean validateXmlStructure(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            return true;
        } catch (Exception e) {
            logger.warn("XML validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate template parameters.
     */
    public boolean validateTemplateParameters(String templateName, Map<String, String> parameters) {
        MsmqMessageTemplate template = templateRepository.findByTemplateName(templateName)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateName));
        
        if (template.getParameters() == null) {
            return true; // No parameters defined
        }
        
        // Check if all required parameters are provided
        for (String requiredParam : template.getParameters().keySet()) {
            if (!parameters.containsKey(requiredParam)) {
                logger.warn("Missing required parameter: {} for template: {}", requiredParam, templateName);
                return false;
            }
        }
        
        return true;
    }
}
