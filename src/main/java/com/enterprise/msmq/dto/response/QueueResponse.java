package com.enterprise.msmq.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Response DTO for MSMQ queue operations.
 * Contains all queue information including auto-generated fields.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing MSMQ queue information")
public class QueueResponse {

    @Schema(
        description = "Unique queue identifier",
        example = "12345"
    )
    @JsonProperty("id")
    private Long id;

    @Schema(
        description = "Queue name identifier",
        example = "payment-processing-queue"
    )
    @JsonProperty("name")
    private String name;

    @Schema(
        description = "Queue path in MSMQ format",
        example = "private$\\payment-processing-queue"
    )
    @JsonProperty("path")
    private String path;

    @Schema(
        description = "Queue type (PRIVATE, PUBLIC, SYSTEM)",
        example = "PRIVATE"
    )
    @JsonProperty("type")
    private String type;

    @Schema(
        description = "Queue status (ACTIVE, INACTIVE, ERROR)",
        example = "ACTIVE"
    )
    @JsonProperty("status")
    private String status;

    @Schema(
        description = "Current number of messages in the queue",
        example = "25"
    )
    @JsonProperty("messageCount")
    private Long messageCount;

    @Schema(
        description = "Maximum number of messages the queue can hold",
        example = "10000"
    )
    @JsonProperty("maxMessageCount")
    private Long maxMessageCount;

    @Schema(
        description = "Current queue size in bytes",
        example = "1048576"
    )
    @JsonProperty("size")
    private Long size;

    @Schema(
        description = "Maximum queue size in bytes",
        example = "104857600"
    )
    @JsonProperty("maxSize")
    private Long maxSize;

    @Schema(
        description = "Queue creation timestamp",
        example = "2024-01-15T10:30:00.000Z"
    )
    @JsonProperty("createdTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdTime;

    @Schema(
        description = "Last modification timestamp",
        example = "2024-01-15T14:45:00.000Z"
    )
    @JsonProperty("modifiedTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime modifiedTime;

    @Schema(
        description = "Last access timestamp",
        example = "2024-01-15T16:20:00.000Z"
    )
    @JsonProperty("lastAccessTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime lastAccessTime;

    @Schema(
        description = "Queue description",
        example = "Queue for processing payment transactions"
    )
    @JsonProperty("description")
    private String description;

    @Schema(
        description = "Whether the queue is transactional",
        example = "false"
    )
    @JsonProperty("transactional")
    private Boolean transactional;

    @Schema(
        description = "Whether the queue is journaled",
        example = "true"
    )
    @JsonProperty("journaled")
    private Boolean journaled;

    @Schema(
        description = "Whether the queue requires authentication",
        example = "false"
    )
    @JsonProperty("authenticated")
    private Boolean authenticated;

    @Schema(
        description = "Whether the queue messages are encrypted",
        example = "false"
    )
    @JsonProperty("encrypted")
    private Boolean encrypted;

    @Schema(
        description = "Queue owner information",
        example = "payment-service"
    )
    @JsonProperty("owner")
    private String owner;

    @Schema(
        description = "Queue permissions",
        example = "Everyone:Full Control"
    )
    @JsonProperty("permissions")
    private String permissions;

    @Schema(
        description = "Error message if queue is in error state",
        example = "Access denied"
    )
    @JsonProperty("errorMessage")
    private String errorMessage;

    @Schema(
        description = "Queue direction (INCOMING_ONLY, OUTGOING_ONLY, BIDIRECTIONAL)",
        example = "BIDIRECTIONAL"
    )
    @JsonProperty("queueDirection")
    private String queueDirection;

    @Schema(
        description = "Queue purpose classification",
        example = "SWIFT_MESSAGES"
    )
    @JsonProperty("queuePurpose")
    private String queuePurpose;

    @Schema(
        description = "Whether the queue is active",
        example = "true"
    )
    @JsonProperty("isActive")
    private Boolean isActive;
}
