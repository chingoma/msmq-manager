package com.enterprise.msmq.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity for MSMQ Message Templates.
 * Stores reusable message templates with parameter placeholders.
 */
@Entity
@Table(name = "msmq_message_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MSMQ Message Template entity for storing reusable message templates with parameter placeholders")
public class MsmqMessageTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier for the template", example = "1")
    private Long id;

    @Column(name = "template_name", nullable = false, unique = true)
    @Schema(description = "Unique name for the template", example = "SWIFT_SHARE_TRANSFER_DETAILED")
    private String templateName;

    @Column(name = "template_type", nullable = false)
    @Schema(description = "Type/category of the template", example = "SWIFT", allowableValues = {"SWIFT", "JSON", "XML", "TEXT"})
    private String templateType;

    @Column(name = "template_content", nullable = false, columnDefinition = "TEXT")
    @Schema(description = "Template content with parameter placeholders using {{PARAM_NAME}} syntax", example = "<RequestPayload>{{FROM_BIC}}</RequestPayload>")
    private String templateContent;

    @Column(name = "description")
    @Schema(description = "Human-readable description of the template", example = "Detailed SWIFT Share Transfer Instruction Template")
    private String description;

    @Column(name = "parameters", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Schema(description = "Parameter definitions with types (string, number, date, datetime)", example = "{\"FROM_BIC\": \"string\", \"QUANTITY\": \"number\"}")
    private Map<String, String> parameters;

    @Column(name = "is_active")
    @Schema(description = "Whether the template is active and available for use", example = "true")
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp when the template was created", example = "2025-08-27T12:36:33")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp when the template was last updated", example = "2025-08-27T12:36:50")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    @Schema(description = "User who created the template", example = "admin")
    private String createdBy;

    @Column(name = "updated_by")
    @Schema(description = "User who last updated the template", example = "admin")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
