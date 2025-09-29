package com.enterprise.msmq.service;

import com.enterprise.msmq.entity.MsmqMessage;
import com.enterprise.msmq.repository.MsmqMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing message status tracking and storage.
 * Handles storing messages and tracking their status using common reference IDs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageStatusService {

    private final MsmqMessageRepository messageRepository;

    /**
     * Store a message in the database with status tracking information.
     */
    @Transactional
    public MsmqMessage storeMessage(MsmqMessage message) {
        try {
            message.setSentAt(LocalDateTime.now());
            message.setProcessingStatus("SENT");
            MsmqMessage savedMessage = messageRepository.save(message);
            log.info("Message stored successfully: ID={}, TransactionID={}, CommonRef={}", 
                    savedMessage.getId(), savedMessage.getTransactionId(), savedMessage.getCommonReferenceId());
            return savedMessage;
        } catch (Exception e) {
            log.error("Failed to store message: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update message status after processing.
     */
    @Transactional
    public void updateMessageStatus(String transactionId, String status, String errorMessage) {
        Optional<MsmqMessage> messageOpt = messageRepository.findByTransactionId(transactionId);
        if (messageOpt.isPresent()) {
            MsmqMessage message = messageOpt.get();
            message.setProcessingStatus(status);
            message.setProcessedAt(LocalDateTime.now());
            if (errorMessage != null) {
                message.setErrorMessage(errorMessage);
            }
            messageRepository.save(message);
            log.info("Message status updated: TransactionID={}, Status={}", transactionId, status);
        } else {
            log.warn("Message not found for status update: TransactionID={}", transactionId);
        }
    }

    /**
     * Get all messages by common reference ID (for paired RECE/DELI messages).
     */
    public List<MsmqMessage> getMessagesByCommonReferenceId(String commonReferenceId) {
        return messageRepository.findByCommonReferenceIdOrderByCreatedAtAsc(commonReferenceId);
    }

    /**
     * Get paired messages (RECE and DELI) by common reference ID.
     */
    public List<MsmqMessage> getPairedMessages(String commonReferenceId) {
        return messageRepository.findPairedMessagesByCommonReferenceId(commonReferenceId);
    }

    /**
     * Get message by transaction ID.
     */
    public Optional<MsmqMessage> getMessageByTransactionId(String transactionId) {
        return messageRepository.findByTransactionId(transactionId);
    }

    /**
     * Get messages by status.
     */
    public List<MsmqMessage> getMessagesByStatus(String status) {
        return messageRepository.findByProcessingStatus(status);
    }

    /**
     * Get messages that need status updates.
     */
    public List<MsmqMessage> getMessagesNeedingStatusUpdate() {
        return messageRepository.findMessagesNeedingStatusUpdate();
    }

    /**
     * Update paired message status by common reference ID.
     */
    @Transactional
    public void updatePairedMessageStatus(String commonReferenceId, String status, String errorMessage) {
        List<MsmqMessage> messages = getPairedMessages(commonReferenceId);
        for (MsmqMessage message : messages) {
            message.setProcessingStatus(status);
            message.setProcessedAt(LocalDateTime.now());
            if (errorMessage != null) {
                message.setErrorMessage(errorMessage);
            }
            messageRepository.save(message);
        }
        log.info("Updated {} paired messages with common reference ID: {}, Status: {}", 
                messages.size(), commonReferenceId, status);
    }
}
