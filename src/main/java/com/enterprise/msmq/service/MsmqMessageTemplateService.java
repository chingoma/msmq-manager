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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
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
        // Pretty-print the merged XML
        String prettyXml = prettyPrintXml(mergedContent);
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
                    messageSent = queueManager.sendMessageToRemote(queueName, message);
                } else if(environment.equalsIgnoreCase("local")) {
                    messageSent = queueManager.sendMessage(queueName, message);
                } else {
                    throw new IllegalArgumentException("Invalid environment: " + environment);
                }
            } else {
                // Default to remote if environment not specified
                messageSent = queueManager.sendMessageToRemote(queueName, message);
            }

            if (messageSent) {
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
     * Minify XML: output as a single line with no spaces between tags or lines.
     * @param xml the raw XML string
     * @return minified XML string (valid XML, single line, no newlines)
     */
    public static String prettyPrintXml(String xml) {
        try {
            // Parse XML into DOM
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            doc.normalizeDocument();

            // Set up transformer for minified output
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

            StringWriter stringWriter = new StringWriter();
            transformer.transform(new javax.xml.transform.dom.DOMSource(doc), new StreamResult(stringWriter));
            String minified = stringWriter.toString();

            // Remove ALL whitespace between tags (newlines, spaces, tabs)
            minified = minified.replaceAll(">\\s+<", "><");
            // Remove any remaining newlines
            minified = minified.replaceAll("\\r?\\n", "");

            return minified.trim();
        } catch (Exception e) {
            logger.warn("Failed to minify XML, sending as-is", e);
            return xml;
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
