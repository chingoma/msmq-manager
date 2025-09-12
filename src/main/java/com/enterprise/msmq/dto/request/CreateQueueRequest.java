package com.enterprise.msmq.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Request DTO for creating MSMQ queues.
 * Contains only the fields that should be provided by the client.
 * 
 * @author Enterprise Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for creating a new MSMQ queue")
public class CreateQueueRequest {

    @Schema(
        description = "Queue name identifier",
        example = "payment-processing-queue",
        required = true
    )
    @NotBlank(message = "Queue name is required")
    private String name;

    @Schema(
        description = "Queue path in MSMQ format",
        example = "private$\\payment-processing-queue",
        required = true
    )
    @NotBlank(message = "Queue path is required")
    private String path;

    @Schema(
        description = "Queue type (PRIVATE, PUBLIC, SYSTEM)",
        example = "PRIVATE",
        allowableValues = {"PRIVATE", "PUBLIC", "SYSTEM"}
    )
    private String type;

    @Schema(
        description = "Queue description",
        example = "Queue for processing payment transactions"
    )
    private String description;

    @Schema(
        description = "Maximum number of messages the queue can hold",
        example = "10000",
        minimum = "1"
    )
    @Positive(message = "Max message count must be positive")
    private Long maxMessageCount;

    @Schema(
        description = "Maximum queue size in bytes",
        example = "104857600",
        minimum = "1"
    )
    @Positive(message = "Max size must be positive")
    private Long maxSize;

    @Schema(
        description = "Whether the queue is transactional",
        example = "false"
    )
    private Boolean transactional;

    @Schema(
        description = "Whether the queue is journaled",
        example = "true"
    )
    private Boolean journaled;

    @Schema(
        description = "Whether the queue requires authentication",
        example = "false"
    )
    private Boolean authenticated;

    @Schema(
        description = "Whether the queue messages are encrypted",
        example = "false"
    )
    private Boolean encrypted;

    @Schema(
        description = "Queue owner information",
        example = "payment-service"
    )
    private String owner;

    @Schema(
        description = "Queue permissions",
        example = "Everyone:Full Control"
    )
    private String permissions;
}
