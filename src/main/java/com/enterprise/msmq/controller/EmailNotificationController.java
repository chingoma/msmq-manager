package com.enterprise.msmq.controller;

import com.enterprise.msmq.dto.ApiResponse;
import com.enterprise.msmq.entity.EmailConfiguration;
import com.enterprise.msmq.entity.MailingList;
import com.enterprise.msmq.enums.ResponseCode;
import com.enterprise.msmq.service.EmailConfigurationService;
import com.enterprise.msmq.service.EmailNotificationService;
import com.enterprise.msmq.service.MailingListService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for managing email notifications, configurations, and mailing lists.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@RestController
@RequestMapping("/email-notifications")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Email Notifications", description = "Endpoints for managing email notifications and configurations")
public class EmailNotificationController {

    private final EmailConfigurationService emailConfigService;
    private final MailingListService mailingListService;
    private final EmailNotificationService emailNotificationService;

    // ==================== EMAIL CONFIGURATIONS ====================

    /**
     * Get all email configurations.
     * 
     * @return list of email configurations
     */
    @GetMapping("/configurations")
    public ResponseEntity<ApiResponse<List<EmailConfiguration>>> getAllEmailConfigurations() {
        try {
            List<EmailConfiguration> configs = emailConfigService.getAllConfigurations();
            return ResponseEntity.ok(ApiResponse.success("Email configurations retrieved successfully", configs));
        } catch (Exception e) {
            log.error("Error retrieving email configurations", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve email configurations: " + e.getMessage()));
        }
    }

    /**
     * Get email configuration by ID.
     * 
     * @param id the configuration ID
     * @return email configuration
     */
    @GetMapping("/configurations/{id}")
    public ResponseEntity<ApiResponse<EmailConfiguration>> getEmailConfiguration(@PathVariable Long id) {
        try {
            return emailConfigService.getConfigurationById(id)
                .map(config -> ResponseEntity.ok(ApiResponse.success("Email configuration retrieved successfully", config)))
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving email configuration: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve email configuration: " + e.getMessage()));
        }
    }

    /**
     * Create or update email configuration.
     * 
     * @param config the email configuration
     * @return saved configuration
     */
    @PostMapping("/configurations")
    public ResponseEntity<ApiResponse<EmailConfiguration>> saveEmailConfiguration(@RequestBody EmailConfiguration config) {
        try {
            if (!emailConfigService.validateConfiguration(config)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.VALIDATION_ERROR, "Invalid email configuration"));
            }
            
            EmailConfiguration saved = emailConfigService.saveConfiguration(config);
            return ResponseEntity.ok(ApiResponse.success("Email configuration saved successfully", saved));
        } catch (Exception e) {
            log.error("Error saving email configuration", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to save email configuration: " + e.getMessage()));
        }
    }

    /**
     * Delete email configuration.
     * 
     * @param id the configuration ID
     * @return success message
     */
    @DeleteMapping("/configurations/{id}")
    public ResponseEntity<ApiResponse<String>> deleteEmailConfiguration(@PathVariable Long id) {
        try {
            emailConfigService.deleteConfiguration(id);
            return ResponseEntity.ok(ApiResponse.success("Email configuration deleted successfully", "Configuration removed"));
        } catch (Exception e) {
            log.error("Error deleting email configuration: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to delete email configuration: " + e.getMessage()));
        }
    }

    /**
     * Activate email configuration.
     * 
     * @param id the configuration ID
     * @return success message
     */
    @PostMapping("/configurations/{id}/activate")
    public ResponseEntity<ApiResponse<String>> activateEmailConfiguration(@PathVariable Long id) {
        try {
            emailConfigService.activateConfiguration(id);
            return ResponseEntity.ok(ApiResponse.success("Email configuration activated successfully", "Configuration activated"));
        } catch (Exception e) {
            log.error("Error activating email configuration: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to activate email configuration: " + e.getMessage()));
        }
    }

    /**
     * Deactivate email configuration.
     * 
     * @param id the configuration ID
     * @return success message
     */
    @PostMapping("/configurations/{id}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivateEmailConfiguration(@PathVariable Long id) {
        try {
            emailConfigService.deactivateConfiguration(id);
            return ResponseEntity.ok(ApiResponse.success("Email configuration deactivated successfully", "Configuration deactivated"));
        } catch (Exception e) {
            log.error("Error deactivating email configuration: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to deactivate email configuration: " + e.getMessage()));
        }
    }

    /**
     * Set email configuration as default.
     * 
     * @param id the configuration ID
     * @return success message
     */
    @PostMapping("/configurations/{id}/set-default")
    public ResponseEntity<ApiResponse<String>> setEmailConfigurationAsDefault(@PathVariable Long id) {
        try {
            emailConfigService.setAsDefault(id);
            return ResponseEntity.ok(ApiResponse.success("Email configuration set as default successfully", "Configuration set as default"));
        } catch (Exception e) {
            log.error("Error setting email configuration as default: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to set email configuration as default: " + e.getMessage()));
        }
    }

    // ==================== MAILING LISTS ====================

    /**
     * Get all mailing lists.
     * 
     * @return list of mailing lists
     */
    @GetMapping("/mailing-lists")
    public ResponseEntity<ApiResponse<List<MailingList>>> getAllMailingLists() {
        try {
            List<MailingList> lists = mailingListService.getAllMailingLists();
            return ResponseEntity.ok(ApiResponse.success("Mailing lists retrieved successfully", lists));
        } catch (Exception e) {
            log.error("Error retrieving mailing lists", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve mailing lists: " + e.getMessage()));
        }
    }

    /**
     * Get mailing list by ID.
     * 
     * @param id the mailing list ID
     * @return mailing list
     */
    @GetMapping("/mailing-lists/{id}")
    public ResponseEntity<ApiResponse<MailingList>> getMailingList(@PathVariable Long id) {
        try {
            return mailingListService.getMailingListById(id)
                .map(list -> ResponseEntity.ok(ApiResponse.success("Mailing list retrieved successfully", list)))
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving mailing list: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve mailing list: " + e.getMessage()));
        }
    }

    /**
     * Create or update mailing list.
     * 
     * @param mailingList the mailing list
     * @return saved mailing list
     */
    @PostMapping("/mailing-lists")
    public ResponseEntity<ApiResponse<MailingList>> saveMailingList(@RequestBody MailingList mailingList) {
        try {
            if (!mailingListService.validateMailingList(mailingList)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.VALIDATION_ERROR, "Invalid mailing list"));
            }
            
            MailingList saved = mailingListService.saveMailingList(mailingList);
            return ResponseEntity.ok(ApiResponse.success("Mailing list saved successfully", saved));
        } catch (Exception e) {
            log.error("Error saving mailing list", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to save mailing list: " + e.getMessage()));
        }
    }

    /**
     * Delete mailing list.
     * 
     * @param id the mailing list ID
     * @return success message
     */
    @DeleteMapping("/mailing-lists/{id}")
    public ResponseEntity<ApiResponse<String>> deleteMailingList(@PathVariable Long id) {
        try {
            mailingListService.deleteMailingList(id);
            return ResponseEntity.ok(ApiResponse.success("Mailing list deleted successfully", "Mailing list removed"));
        } catch (Exception e) {
            log.error("Error deleting mailing list: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to delete mailing list: " + e.getMessage()));
        }
    }

    /**
     * Activate mailing list.
     * 
     * @param id the mailing list ID
     * @return success message
     */
    @PostMapping("/mailing-lists/{id}/activate")
    public ResponseEntity<ApiResponse<String>> activateMailingList(@PathVariable Long id) {
        try {
            mailingListService.activateMailingList(id);
            return ResponseEntity.ok(ApiResponse.success("Mailing list activated successfully", "Mailing list activated"));
        } catch (Exception e) {
            log.error("Error activating mailing list: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to activate mailing list: " + e.getMessage()));
        }
    }

    /**
     * Deactivate mailing list.
     * 
     * @param id the mailing list ID
     * @return success message
     */
    @PostMapping("/mailing-lists/{id}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivateMailingList(@PathVariable Long id) {
        try {
            mailingListService.deactivateMailingList(id);
            return ResponseEntity.ok(ApiResponse.success("Mailing list deactivated successfully", "Mailing list deactivated"));
        } catch (Exception e) {
            log.error("Error deactivating mailing list: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to deactivate mailing list: " + e.getMessage()));
        }
    }

    // ==================== EMAIL NOTIFICATIONS ====================

    /**
     * Get email notification statistics.
     * 
     * @return notification statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEmailNotificationStats() {
        try {
            Map<String, Object> stats = emailNotificationService.getEmailNotificationStats();
            return ResponseEntity.ok(ApiResponse.success("Email notification statistics retrieved successfully", stats));
        } catch (Exception e) {
            log.error("Error retrieving email notification statistics", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to retrieve email notification statistics: " + e.getMessage()));
        }
    }

    /**
     * Test email configuration by sending a test email.
     * 
     * @param id the configuration ID
     * @param testRecipient the test recipient email
     * @return test result
     */
    @PostMapping("/configurations/{id}/test")
    public ResponseEntity<ApiResponse<String>> testEmailConfiguration(
            @PathVariable Long id,
            @RequestParam String testRecipient) {
        try {
            Optional<EmailConfiguration> configOpt = emailConfigService.getConfigurationById(id);
            if (configOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            EmailConfiguration config = configOpt.get();
            boolean success = emailNotificationService.sendTestEmail(config, testRecipient);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("Test email sent successfully", "Test email delivered"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.BUSINESS_ERROR, "Failed to send test email"));
            }
        } catch (Exception e) {
            log.error("Error testing email configuration: {}", id, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.SYSTEM_ERROR, "Failed to test email configuration: " + e.getMessage()));
        }
    }
}
