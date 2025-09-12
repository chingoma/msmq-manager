package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.QueueAlert;
import com.enterprise.msmq.entity.EmailConfiguration;
import com.enterprise.msmq.entity.MailingList;
import com.enterprise.msmq.enums.AlertSeverity;
import com.enterprise.msmq.enums.AlertType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for sending email notifications for queue alerts.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {
    
    @Value("${msmq.email.notifications.enabled:true}")
    private boolean emailNotificationsEnabled;
    
    @Value("${msmq.email.notifications.max-retries:3}")
    private int maxRetries;
    
    @Value("${msmq.email.notifications.retry-delay-ms:5000}")
    private long retryDelayMs;
    
    private final EmailConfigurationService emailConfigService;
    private final MailingListService mailingListService;
    
    /**
     * Send email notification for a queue alert.
     * 
     * @param alert the alert to send notification for
     */
    public void sendAlertNotification(QueueAlert alert) {
        if (!emailNotificationsEnabled) {
            log.debug("Email notifications are disabled, skipping alert: {}", alert.getMessage());
            return;
        }
        
        try {
            // Get active mailing lists that should receive this alert
            List<MailingList> activeMailingLists = mailingListService.findActiveMailingLists();
            List<MailingList> relevantLists = activeMailingLists.stream()
                .filter(list -> list.shouldReceiveAlert(alert.getSeverity(), alert.getType()))
                .toList();
            
            if (relevantLists.isEmpty()) {
                log.debug("No mailing lists configured to receive alert: {} - {}", 
                    alert.getSeverity(), alert.getType());
                return;
            }
            
            // Get default email configuration
            EmailConfiguration emailConfig = emailConfigService.getDefaultConfiguration();
            if (emailConfig == null) {
                log.warn("No default email configuration found, cannot send notifications");
                return;
            }
            
            // Send emails to all relevant mailing lists
            for (MailingList mailingList : relevantLists) {
                sendEmailToMailingList(alert, mailingList, emailConfig);
            }
            
            log.info("Sent email notifications for alert [{}] to {} mailing lists", 
                alert.getType(), relevantLists.size());
                
        } catch (Exception e) {
            log.error("Error sending email notification for alert: {}", alert.getMessage(), e);
        }
    }
    
    /**
     * Send email notification to a specific mailing list.
     * 
     * @param alert the alert to send
     * @param mailingList the mailing list to send to
     * @param emailConfig the email configuration to use
     */
    private void sendEmailToMailingList(QueueAlert alert, MailingList mailingList, EmailConfiguration emailConfig) {
        if (mailingList.getEmailAddresses() == null || mailingList.getEmailAddresses().isEmpty()) {
            log.warn("Mailing list '{}' has no email addresses", mailingList.getListName());
            return;
        }
        
        try {
            JavaMailSender mailSender = createMailSender(emailConfig);
            MimeMessage message = createAlertMessage(alert, mailingList, emailConfig);
            
            // Send with retry logic
            sendWithRetry(mailSender, message, mailingList.getEmailAddresses());
            
            log.debug("Successfully sent alert notification to mailing list: {}", mailingList.getListName());
            
        } catch (Exception e) {
            log.error("Failed to send email notification to mailing list '{}': {}", 
                mailingList.getListName(), e.getMessage(), e);
        }
    }
    
    /**
     * Create and configure JavaMailSender from email configuration.
     * 
     * @param config the email configuration
     * @return configured JavaMailSender
     */
    private JavaMailSender createMailSender(EmailConfiguration config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        mailSender.setHost(config.getSmtpHost());
        mailSender.setPort(config.getSmtpPort());
        mailSender.setUsername(config.getUsername());
        mailSender.setPassword(config.getPassword());
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", config.getUseTls());
        props.put("mail.smtp.ssl.enable", config.getUseSsl());
        props.put("mail.smtp.connectiontimeout", config.getConnectionTimeout());
        props.put("mail.smtp.timeout", config.getReadTimeout());
        props.put("mail.smtp.writetimeout", config.getReadTimeout());
        
        return mailSender;
    }
    
    /**
     * Create MimeMessage for the alert notification.
     * 
     * @param alert the alert
     * @param mailingList the mailing list
     * @param emailConfig the email configuration
     * @return configured MimeMessage
     * @throws MessagingException if message creation fails
     */
    private MimeMessage createAlertMessage(QueueAlert alert, MailingList mailingList, 
                                         EmailConfiguration emailConfig) throws MessagingException {
        JavaMailSender mailSender = createMailSender(emailConfig);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        // Set recipients
        helper.setTo(mailingList.getEmailAddresses().toArray(new String[0]));
        try {
            helper.setFrom(emailConfig.getFromEmail(), emailConfig.getFromName());
        } catch (java.io.UnsupportedEncodingException e) {
            log.warn("Failed to set from name, using email only: {}", e.getMessage());
            helper.setFrom(emailConfig.getFromEmail());
        }
        
        // Set subject
        String subject = String.format("[MSMQ ALERT] %s - %s", 
            alert.getSeverity(), alert.getType());
        helper.setSubject(subject);
        
        // Set content
        String htmlContent = createAlertEmailContent(alert, mailingList);
        helper.setText(htmlContent, true);
        
        return message;
    }
    
    /**
     * Create HTML content for the alert email.
     * 
     * @param alert the alert
     * @param mailingList the mailing list
     * @return HTML content
     */
    private String createAlertEmailContent(QueueAlert alert, MailingList mailingList) {
        String severityColor = getSeverityColor(alert.getSeverity());
        String severityIcon = getSeverityIcon(alert.getSeverity());
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>MSMQ Queue Alert</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    .alert { border-left: 4px solid %s; padding: 15px; margin: 20px 0; }
                    .severity { font-weight: bold; color: %s; }
                    .timestamp { color: #666; font-size: 12px; }
                    .message { margin: 15px 0; }
                    .details { background: #f5f5f5; padding: 10px; border-radius: 4px; }
                </style>
            </head>
            <body>
                <h2>%s MSMQ Queue Alert</h2>
                <div class="alert">
                    <div class="severity">%s %s</div>
                    <div class="timestamp">%s</div>
                    <div class="message">%s</div>
                    <div class="details">
                        <strong>Alert Type:</strong> %s<br>
                        <strong>Severity:</strong> %s<br>
                        <strong>Queue:</strong> %s<br>
                        <strong>Mailing List:</strong> %s
                    </div>
                </div>
                <p>This is an automated notification from the MSMQ Queue Monitoring System.</p>
            </body>
            </html>
            """,
            severityColor, severityColor, severityIcon, severityIcon, alert.getSeverity(),
            alert.getTimestamp(), alert.getMessage(), alert.getType(), alert.getSeverity(),
            alert.getQueueName() != null ? alert.getQueueName() : "N/A",
            mailingList.getListName()
        );
    }
    
    /**
     * Get color for alert severity.
     * 
     * @param severity the alert severity
     * @return color hex code
     */
    private String getSeverityColor(AlertSeverity severity) {
        return switch (severity) {
            case ERROR -> "#dc3545";    // Red
            case WARNING -> "#ffc107";   // Yellow
            case INFO -> "#17a2b8";      // Blue
        };
    }
    
    /**
     * Get icon for alert severity.
     * 
     * @param severity the alert severity
     * @return icon symbol
     */
    private String getSeverityIcon(AlertSeverity severity) {
        return switch (severity) {
            case ERROR -> "🚨";
            case WARNING -> "⚠️";
            case INFO -> "ℹ️";
        };
    }
    
    /**
     * Send email with retry logic.
     * 
     * @param mailSender the mail sender
     * @param message the message to send
     * @param recipients the recipients
     */
    private void sendWithRetry(JavaMailSender mailSender, MimeMessage message, List<String> recipients) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < maxRetries) {
            try {
                mailSender.send(message);
                return; // Success, exit retry loop
            } catch (Exception e) {
                lastException = e;
                attempts++;
                
                if (attempts < maxRetries) {
                    log.warn("Email send attempt {} failed, retrying in {}ms: {}", 
                        attempts, retryDelayMs, e.getMessage());
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // All retries failed
        log.error("Failed to send email after {} attempts to recipients: {}", 
            maxRetries, recipients);
        throw new RuntimeException("Email send failed after " + maxRetries + " attempts", lastException);
    }
    
    /**
     * Send test email to verify configuration.
     * 
     * @param emailConfig the email configuration to test
     * @param testRecipient the test recipient email
     * @return true if test email sent successfully
     */
    public boolean sendTestEmail(EmailConfiguration emailConfig, String testRecipient) {
        try {
            JavaMailSender mailSender = createMailSender(emailConfig);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(testRecipient);
            try {
                helper.setFrom(emailConfig.getFromEmail(), emailConfig.getFromName());
            } catch (java.io.UnsupportedEncodingException e) {
                log.warn("Failed to set from name, using email only: {}", e.getMessage());
                helper.setFrom(emailConfig.getFromEmail());
            }
            helper.setSubject("[MSMQ] Test Email Configuration");
            helper.setText("This is a test email to verify your MSMQ email configuration is working correctly.", false);
            
            mailSender.send(message);
            log.info("Test email sent successfully to: {}", testRecipient);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send test email to {}: {}", testRecipient, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check if email notifications are enabled.
     * 
     * @return true if enabled
     */
    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }
    
    /**
     * Get email notification statistics.
     * 
     * @return statistics map
     */
    public java.util.Map<String, Object> getEmailNotificationStats() {
        return java.util.Map.of(
            "enabled", emailNotificationsEnabled,
            "maxRetries", maxRetries,
            "retryDelayMs", retryDelayMs,
            "activeConfigurations", emailConfigService.countActiveConfigurations(),
            "activeMailingLists", mailingListService.countActiveMailingLists(),
            "lastUpdate", java.time.LocalDateTime.now()
        );
    }
}
