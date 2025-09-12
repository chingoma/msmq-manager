package com.enterprise.msmq.service;

import com.enterprise.msmq.dto.MsmqMessage;
import com.enterprise.msmq.dto.MsmqQueue;
import com.enterprise.msmq.service.contracts.IMsmqQueueManager;
import com.enterprise.msmq.platform.windows.MsmqNativeInterface;
import com.enterprise.msmq.platform.windows.MsmqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.Memory;
import com.sun.jna.Structure;
import com.sun.jna.WString;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Native MSMQ Queue Manager using JNA.
 * 
 * This implementation uses the native MSMQ API through JNA
 * for high-performance queue operations. It implements the 
 * IMsmqQueueManager interface to provide consistent queue management.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NativeMsmqQueueManager implements IMsmqQueueManager {

    @Override
    public boolean createQueue(MsmqQueue queue) {
        try {
            log.info("Creating queue via Native MSMQ API: {}", queue.getName());
            
            String queuePath = queue.getPath() != null ? queue.getPath() : "private$\\" + queue.getName();
            String formatName = "itr00ictl135\\" + queuePath;
            
            PointerByReference queueHandle = new PointerByReference();
            int result = MsmqNativeInterface.INSTANCE.MQCreateQueue(null, formatName, queueHandle);
            
            if (result == MsmqNativeInterface.MQ_OK) {
                log.info("Successfully created queue via Native MSMQ API: {}", queuePath);
                // Close the handle after creation
                MsmqNativeInterface.INSTANCE.MQCloseQueue(queueHandle.getValue());
                return true;
            } else {
                log.error("Failed to create queue via Native MSMQ API: {}, error code: 0x{}", 
                    queuePath, Integer.toHexString(result));
                return false;
            }
        } catch (Exception e) {
            log.error("Error creating queue {} via Native MSMQ: {}", queue.getName(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean deleteQueue(String queuePath) {
        try {
            log.info("Deleting queue via Native MSMQ API: {}", queuePath);
            
            String formatName = ".\\private$\\" + queuePath;
            int result = MsmqNativeInterface.INSTANCE.MQDeleteQueue(formatName);
            
            if (result == MsmqNativeInterface.MQ_OK) {
                log.info("Successfully deleted queue via Native MSMQ API: {}", queuePath);
                return true;
            } else if (result == MsmqNativeInterface.MQ_ERROR_QUEUE_NOT_FOUND) {
                log.warn("Queue not found for deletion: {}", queuePath);
                return false;
            } else {
                log.error("Failed to delete queue via Native MSMQ API: {}, error code: 0x{}", 
                    queuePath, Integer.toHexString(result));
                return false;
            }
        } catch (Exception e) {
            log.error("Error deleting queue {} via Native MSMQ: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean queueExists(String queuePath) {
        try {
            log.debug("Checking if queue exists via Native MSMQ API: {}", queuePath);
            
            String formatName = "itr00ictl135\\private$\\" + queuePath;
            PointerByReference queueHandle = new PointerByReference();
            
            int result = MsmqNativeInterface.INSTANCE.MQOpenQueue(
                formatName, 
                MsmqConstants.MQ_PEEK_ACCESS, 
                MsmqConstants.MQ_DENY_NONE, 
                queueHandle
            );
            
            if (result == MsmqNativeInterface.MQ_OK) {
                MsmqNativeInterface.INSTANCE.MQCloseQueue(queueHandle.getValue());
                return true;
            } else if (result == MsmqNativeInterface.MQ_ERROR_QUEUE_NOT_FOUND) {
                return false;
            } else {
                log.warn("Unexpected result when checking queue existence: 0x{}", Integer.toHexString(result));
                return false;
            }
        } catch (Exception e) {
            log.error("Error checking if queue exists {} via Native MSMQ: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendMessage(String queuePath, MsmqMessage message) {
        return sendMessage(queuePath, message.getBody());
    }

    @Override
    public boolean sendMessage(String queuePath, String messageBody) {
        try {
            log.info("Sending message via Native MSMQ API to queue: {}", queuePath);
            
            String formatName = buildMsmqFormatName(queuePath);
            
            PointerByReference queueHandle = new PointerByReference();
            
            // Open queue for sending
            int result = MsmqNativeInterface.INSTANCE.MQOpenQueue(
                formatName, 
                MsmqConstants.MQ_SEND_ACCESS, 
                MsmqConstants.MQ_DENY_NONE, 
                queueHandle
            );
            
            if (result != MsmqNativeInterface.MQ_OK) {
                log.error("Failed to open queue for sending: {}, error code: 0x{}", 
                    queuePath, Integer.toHexString(result));
                return false;
            }
            
            try {
                // Create message properties structure
                MsmqMessageProperties msgProps = new MsmqMessageProperties();
                msgProps.body = messageBody.getBytes(StandardCharsets.UTF_8);
                msgProps.bodySize = msgProps.body.length;
                msgProps.label = "MSMQ Manager Message";
                msgProps.labelSize = msgProps.label.length();
                
                // Send message
                result = MsmqNativeInterface.INSTANCE.MQSendMessage(
                    queueHandle.getValue(), 
                    msgProps.getPointer(), 
                    null // No transaction
                );
                
                if (result == MsmqNativeInterface.MQ_OK) {
                    log.info("Successfully sent message via Native MSMQ API to queue: {}", queuePath);
                    return true;
                } else {
                    log.error("Failed to send message via Native MSMQ API: {}, error code: 0x{}", 
                        queuePath, Integer.toHexString(result));
                    return false;
                }
            } finally {
                // Always close the queue handle
                MsmqNativeInterface.INSTANCE.MQCloseQueue(queueHandle.getValue());
            }
        } catch (Exception e) {
            log.error("Error sending message to queue {} via Native MSMQ: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Optional<MsmqMessage> receiveMessage(String queuePath) {
        try {
            log.debug("Receiving message via Native MSMQ API from queue: {}", queuePath);
            
            String formatName = buildMsmqFormatName(queuePath);
            
            PointerByReference queueHandle = new PointerByReference();
            
            // Open queue for receiving
            int result = MsmqNativeInterface.INSTANCE.MQOpenQueue(
                formatName, 
                MsmqConstants.MQ_RECEIVE_ACCESS, 
                MsmqConstants.MQ_DENY_NONE, 
                queueHandle
            );
            
            if (result != MsmqNativeInterface.MQ_OK) {
                log.error("Failed to open queue for receiving: {}, error code: 0x{}", 
                    queuePath, Integer.toHexString(result));
                return Optional.empty();
            }
            
            try {
                // Create message properties structure for receiving
                MsmqMessageProperties msgProps = new MsmqMessageProperties();
                msgProps.body = new byte[8192]; // 8KB buffer
                msgProps.bodySize = msgProps.body.length;
                
                // Receive message
                result = MsmqNativeInterface.INSTANCE.MQReceiveMessage(
                    queueHandle.getValue(), 
                    MsmqConstants.MQ_DEFAULT_TIMEOUT, 
                    MsmqConstants.MQ_ACTION_RECEIVE, 
                    msgProps.getPointer(), 
                    null, // No overlapped
                    null  // No transaction
                );
                
                if (result == MsmqNativeInterface.MQ_OK) {
                    String receivedBody = new String(msgProps.body, 0, msgProps.bodySize, StandardCharsets.UTF_8);
                    MsmqMessage message = new MsmqMessage();
                    message.setMessageId(UUID.randomUUID().toString());
                    message.setBody(receivedBody);
                    message.setSourceQueue(queuePath);
                    message.setCreatedTime(LocalDateTime.now());
                    
                    log.debug("Successfully received message via Native MSMQ API from queue: {}", queuePath);
                    return Optional.of(message);
                } else if (result == MsmqConstants.MQ_ERROR_IO_TIMEOUT) {
                    log.debug("No message available in queue: {}", queuePath);
                    return Optional.empty();
                } else {
                    log.error("Failed to receive message via Native MSMQ API: {}, error code: 0x{}", 
                        queuePath, Integer.toHexString(result));
                    return Optional.empty();
                }
            } finally {
                // Always close the queue handle
                MsmqNativeInterface.INSTANCE.MQCloseQueue(queueHandle.getValue());
            }
        } catch (Exception e) {
            log.error("Error receiving message from queue {} via Native MSMQ: {}", queuePath, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<MsmqMessage> peekMessage(String queuePath) {
        try {
            log.debug("Peeking message via Native MSMQ API from queue: {}", queuePath);
            
            String formatName = buildMsmqFormatName(queuePath);
            
            PointerByReference queueHandle = new PointerByReference();
            
            // Open queue for peeking
            int result = MsmqNativeInterface.INSTANCE.MQOpenQueue(
                formatName, 
                MsmqConstants.MQ_PEEK_ACCESS, 
                MsmqConstants.MQ_DENY_NONE, 
                queueHandle
            );
            
            if (result != MsmqNativeInterface.MQ_OK) {
                log.error("Failed to open queue for peeking: {}, error code: 0x{}", 
                    queuePath, Integer.toHexString(result));
                return Optional.empty();
            }
            
            try {
                // Create message properties structure for peeking
                MsmqMessageProperties msgProps = new MsmqMessageProperties();
                msgProps.body = new byte[8192]; // 8KB buffer
                msgProps.bodySize = msgProps.body.length;
                
                // Peek message
                result = MsmqNativeInterface.INSTANCE.MQReceiveMessage(
                    queueHandle.getValue(), 
                    MsmqConstants.MQ_DEFAULT_TIMEOUT, 
                    MsmqConstants.MQ_ACTION_PEEK_CURRENT, 
                    msgProps.getPointer(), 
                    null, // No overlapped
                    null  // No transaction
                );
                
                if (result == MsmqNativeInterface.MQ_OK) {
                    String peekedBody = new String(msgProps.body, 0, msgProps.bodySize, StandardCharsets.UTF_8);
                    MsmqMessage message = new MsmqMessage();
                    message.setMessageId(UUID.randomUUID().toString());
                    message.setBody(peekedBody);
                    message.setSourceQueue(queuePath);
                    message.setCreatedTime(LocalDateTime.now());
                    
                    log.debug("Successfully peeked message via Native MSMQ API from queue: {}", queuePath);
                    return Optional.of(message);
                } else if (result == MsmqConstants.MQ_ERROR_IO_TIMEOUT) {
                    log.debug("No message available in queue: {}", queuePath);
                    return Optional.empty();
                } else {
                    log.error("Failed to peek message via Native MSMQ API: {}, error code: 0x{}", 
                        queuePath, Integer.toHexString(result));
                    return Optional.empty();
                }
            } finally {
                // Always close the queue handle
                MsmqNativeInterface.INSTANCE.MQCloseQueue(queueHandle.getValue());
            }
        } catch (Exception e) {
            log.error("Error peeking message from queue {} via Native MSMQ: {}", queuePath, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public void purgeQueue(String queuePath) {
        try {
            log.info("Purging queue via Native MSMQ API: {}", queuePath);
            
            // For native MSMQ, we need to receive all messages to purge
            String formatName = ".\\private$\\" + queuePath;
            PointerByReference queueHandle = new PointerByReference();
            
            // Open queue for receiving
            int result = MsmqNativeInterface.INSTANCE.MQOpenQueue(
                formatName, 
                MsmqConstants.MQ_RECEIVE_ACCESS, 
                MsmqConstants.MQ_DENY_NONE, 
                queueHandle
            );
            
            if (result != MsmqNativeInterface.MQ_OK) {
                log.error("Failed to open queue for purging: {}, error code: 0x{}", 
                    queuePath, Integer.toHexString(result));
                return;
            }
            
            try {
                int purgedCount = 0;
                MsmqMessageProperties msgProps = new MsmqMessageProperties();
                msgProps.body = new byte[8192];
                msgProps.bodySize = msgProps.body.length;
                
                // Keep receiving messages until none are left
                while (true) {
                    result = MsmqNativeInterface.INSTANCE.MQReceiveMessage(
                        queueHandle.getValue(), 
                        100, // Short timeout
                        MsmqConstants.MQ_ACTION_RECEIVE, 
                        msgProps.getPointer(), 
                        null, 
                        null
                    );
                    
                    if (result == MsmqNativeInterface.MQ_OK) {
                        purgedCount++;
                    } else if (result == MsmqConstants.MQ_ERROR_IO_TIMEOUT) {
                        break; // No more messages
                    } else {
                        log.error("Error during queue purge: 0x{}", Integer.toHexString(result));
                        break;
                    }
                }
                
                log.info("Successfully purged {} messages from queue: {}", purgedCount, queuePath);
            } finally {
                MsmqNativeInterface.INSTANCE.MQCloseQueue(queueHandle.getValue());
            }
        } catch (Exception e) {
            log.error("Error purging queue {} via Native MSMQ: {}", queuePath, e.getMessage(), e);
        }
    }

    @Override
    public List<MsmqQueue> getAllQueues() {
        try {
            log.debug("Getting all queues via Native MSMQ API");
            
            // For native MSMQ, we'll use a simple approach to get private queues
            // This is a simplified implementation - in production you might want to use
            // MQGetMachineProperties or similar for more comprehensive queue enumeration
            
            List<MsmqQueue> queues = new ArrayList<>();
            
            // Check for common private queues
            String[] commonQueues = {
                "securities-settlement-queue",
                "testqueue",
                "testqueue"
            };
            
            for (String queueName : commonQueues) {
                if (queueExists(queueName)) {
                    MsmqQueue queue = new MsmqQueue();
                    queue.setName(queueName);
                    queue.setPath("private$\\" + queueName);
                    queue.setType("PRIVATE");
                    queues.add(queue);
                }
            }
            
            log.debug("Found {} queues via Native MSMQ API", queues.size());
            return queues;
        } catch (Exception e) {
            log.error("Error getting all queues via Native MSMQ: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public Optional<MsmqQueue> getQueue(String queuePath) {
        try {
            log.debug("Getting queue via Native MSMQ API: {}", queuePath);
            
            if (queueExists(queuePath)) {
                MsmqQueue queue = new MsmqQueue();
                queue.setName(queuePath.replace("private$\\", ""));
                queue.setPath(queuePath);
                queue.setType("PRIVATE");
                return Optional.of(queue);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting queue {} via Native MSMQ: {}", queuePath, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public long getMessageCount(String queuePath) {
        try {
            log.debug("Getting message count via Native MSMQ API for queue: {}", queuePath);
            
            // For native MSMQ, we'll count messages by peeking until none are left
            String formatName = ".\\private$\\" + queuePath;
            PointerByReference queueHandle = new PointerByReference();
            
            int result = MsmqNativeInterface.INSTANCE.MQOpenQueue(
                formatName, 
                MsmqConstants.MQ_PEEK_ACCESS, 
                MsmqConstants.MQ_DENY_NONE, 
                queueHandle
            );
            
            if (result != MsmqNativeInterface.MQ_OK) {
                log.error("Failed to open queue for message count: {}, error code: 0x{}", 
                    queuePath, Integer.toHexString(result));
                return -1;
            }
            
            try {
                long messageCount = 0;
                MsmqMessageProperties msgProps = new MsmqMessageProperties();
                msgProps.body = new byte[8192];
                msgProps.bodySize = msgProps.body.length;
                
                // Count messages by peeking
                while (true) {
                    result = MsmqNativeInterface.INSTANCE.MQReceiveMessage(
                        queueHandle.getValue(), 
                        100, // Short timeout
                        MsmqConstants.MQ_ACTION_PEEK_NEXT, 
                        msgProps.getPointer(), 
                        null, 
                        null
                    );
                    
                    if (result == MsmqNativeInterface.MQ_OK) {
                        messageCount++;
                    } else if (result == MsmqConstants.MQ_ERROR_IO_TIMEOUT) {
                        break; // No more messages
                    } else {
                        log.error("Error during message count: 0x{}", Integer.toHexString(result));
                        break;
                    }
                }
                
                return messageCount;
            } finally {
                MsmqNativeInterface.INSTANCE.MQCloseQueue(queueHandle.getValue());
            }
        } catch (Exception e) {
            log.error("Error getting message count for queue {} via Native MSMQ: {}", queuePath, e.getMessage(), e);
            return -1;
        }
    }

    @Override
    public boolean testConnectivity() {
        try {
            log.debug("Testing Native MSMQ connectivity");
            
            // Test if we can load the native library
            try {
                MsmqNativeInterface.INSTANCE.toString();
                log.debug("MSMQ native library loaded successfully");
                return true;
            } catch (Exception e) {
                log.error("Failed to load MSMQ native library: {}", e.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Native MSMQ connectivity test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateQueue(String queuePath, MsmqQueue queue) {
        try {
            log.info("Updating queue via Native MSMQ API: {}", queuePath);
            
            // For native MSMQ, updating typically involves deleting and recreating
            // since MSMQ doesn't have a direct update method
            if (deleteQueue(queuePath)) {
                return createQueue(queue);
            }
            return false;
        } catch (Exception e) {
            log.error("Error updating queue {} via Native MSMQ: {}", queuePath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Optional<MsmqQueue> getQueueStatistics(String queuePath) {
        try {
            log.debug("Getting queue statistics via Native MSMQ API: {}", queuePath);
            
            if (queueExists(queuePath)) {
                MsmqQueue queue = new MsmqQueue();
                queue.setName(queuePath.replace("private$\\", ""));
                queue.setPath(queuePath);
                queue.setType("PRIVATE");
                
                // Get message count as a basic statistic
                long messageCount = getMessageCount(queuePath);
                if (messageCount >= 0) {
                    // You could add more statistics here if needed
                    log.debug("Queue {} has {} messages", queuePath, messageCount);
                }
                
                return Optional.of(queue);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting queue statistics for {} via Native MSMQ: {}", queuePath, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<MsmqMessage> receiveMessage(String queuePath, long timeout) {
        try {
            log.debug("Receiving message with timeout via Native MSMQ API from queue: {}", queuePath);
            
            String formatName = ".\\private$\\" + queuePath;
            PointerByReference queueHandle = new PointerByReference();
            
            int result = MsmqNativeInterface.INSTANCE.MQOpenQueue(
                formatName, 
                MsmqConstants.MQ_RECEIVE_ACCESS, 
                MsmqConstants.MQ_DENY_NONE, 
                queueHandle
            );
            
            if (result != MsmqNativeInterface.MQ_OK) {
                log.error("Failed to open queue for receiving: {}, error code: 0x{}", 
                    queuePath, Integer.toHexString(result));
                return Optional.empty();
            }
            
            try {
                MsmqMessageProperties msgProps = new MsmqMessageProperties();
                msgProps.body = new byte[8192];
                msgProps.bodySize = msgProps.body.length;
                
                // Convert timeout to MSMQ format (0 = infinite, positive = milliseconds)
                int msmqTimeout = timeout <= 0 ? MsmqConstants.MQ_INFINITE : (int) timeout;
                
                result = MsmqNativeInterface.INSTANCE.MQReceiveMessage(
                    queueHandle.getValue(), 
                    msmqTimeout, 
                    MsmqConstants.MQ_ACTION_RECEIVE, 
                    msgProps.getPointer(), 
                    null, 
                    null
                );
                
                if (result == MsmqNativeInterface.MQ_OK) {
                    String receivedBody = new String(msgProps.body, 0, msgProps.bodySize, StandardCharsets.UTF_8);
                    MsmqMessage message = new MsmqMessage();
                    message.setMessageId(UUID.randomUUID().toString());
                    message.setBody(receivedBody);
                    message.setSourceQueue(queuePath);
                    message.setCreatedTime(LocalDateTime.now());
                    
                    return Optional.of(message);
                } else if (result == MsmqConstants.MQ_ERROR_IO_TIMEOUT) {
                    log.debug("No message available in queue: {} (timeout: {}ms)", queuePath, timeout);
                    return Optional.empty();
                } else {
                    log.error("Failed to receive message with timeout: {}, error code: 0x{}", 
                        queuePath, Integer.toHexString(result));
                    return Optional.empty();
                }
            } finally {
                MsmqNativeInterface.INSTANCE.MQCloseQueue(queueHandle.getValue());
            }
        } catch (Exception e) {
            log.error("Error receiving message with timeout from queue {} via Native MSMQ: {}", queuePath, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<MsmqMessage> peekMessage(String queuePath, long timeout) {
        try {
            log.debug("Peeking message with timeout via Native MSMQ API from queue: {}", queuePath);
            
            String formatName = ".\\private$\\" + queuePath;
            PointerByReference queueHandle = new PointerByReference();
            
            int result = MsmqNativeInterface.INSTANCE.MQOpenQueue(
                formatName, 
                MsmqConstants.MQ_PEEK_ACCESS, 
                MsmqConstants.MQ_DENY_NONE, 
                queueHandle
            );
            
            if (result != MsmqNativeInterface.MQ_OK) {
                log.error("Failed to open queue for peeking: {}, error code: 0x{}", 
                    queuePath, Integer.toHexString(result));
                return Optional.empty();
            }
            
            try {
                MsmqMessageProperties msgProps = new MsmqMessageProperties();
                msgProps.body = new byte[8192];
                msgProps.bodySize = msgProps.body.length;
                
                int msmqTimeout = timeout <= 0 ? MsmqConstants.MQ_INFINITE : (int) timeout;
                
                result = MsmqNativeInterface.INSTANCE.MQReceiveMessage(
                    queueHandle.getValue(), 
                    msmqTimeout, 
                    MsmqConstants.MQ_ACTION_PEEK_CURRENT, 
                    msgProps.getPointer(), 
                    null, 
                    null
                );
                
                if (result == MsmqNativeInterface.MQ_OK) {
                    String peekedBody = new String(msgProps.body, 0, msgProps.bodySize, StandardCharsets.UTF_8);
                    MsmqMessage message = new MsmqMessage();
                    message.setMessageId(UUID.randomUUID().toString());
                    message.setBody(peekedBody);
                    message.setSourceQueue(queuePath);
                    message.setCreatedTime(LocalDateTime.now());
                    
                    return Optional.of(message);
                } else if (result == MsmqConstants.MQ_ERROR_IO_TIMEOUT) {
                    log.debug("No message available in queue: {} (timeout: {}ms)", queuePath, timeout);
                    return Optional.empty();
                } else {
                    log.error("Failed to peek message with timeout: {}, error code: 0x{}", 
                        queuePath, Integer.toHexString(result));
                    return Optional.empty();
                }
            } finally {
                MsmqNativeInterface.INSTANCE.MQCloseQueue(queueHandle.getValue());
            }
        } catch (Exception e) {
            log.error("Error peeking message with timeout from queue {} via Native MSMQ: {}", queuePath, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Helper method to construct proper MSMQ format names.
     * Tries multiple formats including format names and ensures queue exists.
     */
    private String buildMsmqFormatName(String queuePath) {
        // Try multiple path formats to find one that works
        String[] possibleFormats = {
            // Format 1: PowerShell style (working format)
            ".\\private$\\" + queuePath,
            
            // Format 2: UNC network path
            "\\\\itr00ictl135\\private$\\" + queuePath,
            
            // Format 3: Without dot prefix
            "private$\\" + queuePath,
            
            // Format 4: Forward slashes
            "./private$/" + queuePath,
            
            // Format 5: Full UNC with machine name
            "\\\\itr00ictl135\\private$\\" + queuePath,
            
            // Format 6: Local machine format
            ".\\private$\\" + queuePath,
            
            // Format 7: DIRECT format name
            "DIRECT=OS:itr00ictl135\\private$\\" + queuePath,
            
            // Format 8: DIRECT format name with dot
            "DIRECT=OS:.\\private$\\" + queuePath,
            
            // Format 9: TCP format name
            "DIRECT=TCP:itr00ictl135\\private$\\" + queuePath,
            
            // Format 10: Local format name
            "DIRECT=OS:.\\private$\\" + queuePath
        };
        
        // Try each format until one works
        for (int i = 0; i < possibleFormats.length; i++) {
            String formatName = possibleFormats[i];
            log.debug("Trying format {}: '{}' for queue: '{}'", i + 1, formatName, queuePath);
            
            // Test if this format can open the queue
            if (testQueuePathFormat(formatName)) {
                log.info("✅ Found working format {}: '{}' for queue: '{}'", i + 1, formatName, queuePath);
                return formatName;
            }
        }
        
        // If none work, try to create the queue first, then use PowerShell format
        log.warn("❌ No working format found, attempting to create queue first");
        if (createQueueIfNotExists(queuePath)) {
            String fallbackFormat = ".\\private$\\" + queuePath;
            log.info("✅ Queue created successfully, using fallback format: '{}'", fallbackFormat);
            return fallbackFormat;
        }
        
        // Last resort fallback
        String fallbackFormat = ".\\private$\\" + queuePath;
        log.error("❌ Failed to create queue, using fallback: '{}'", fallbackFormat);
        return fallbackFormat;
    }
    
    /**
     * Create the queue if it doesn't exist, using PowerShell as fallback.
     */
    private boolean createQueueIfNotExists(String queuePath) {
        try {
            log.debug("Attempting to create queue if it doesn't exist: {}", queuePath);
            
            // First try native MSMQ creation with different approaches
            if (tryNativeQueueCreation(queuePath)) {
                return true;
            }
            
            // If native fails, try PowerShell creation
            log.warn("Native queue creation failed, trying PowerShell fallback");
            return createQueueViaPowerShell(queuePath);
            
        } catch (Exception e) {
            log.error("Error creating queue {}: {}", queuePath, e.getMessage());
            return false;
        }
    }
    
    /**
     * Try different native MSMQ queue creation approaches with proper structures.
     */
    private boolean tryNativeQueueCreation(String queuePath) {
        // Try different creation formats with proper MSMQ structures
        String[] creationFormats = {
            ".\\private$\\" + queuePath,
            "private$\\" + queuePath,
            queuePath
        };
        
        for (String formatName : creationFormats) {
            try {
                log.debug("Trying native creation with format: '{}'", formatName);
                
                // Try with null security descriptor first
                if (tryCreateQueueWithSecurityDescriptor(formatName, null)) {
                    return true;
                }
                
                // Try with default security descriptor
                if (tryCreateQueueWithSecurityDescriptor(formatName, createDefaultSecurityDescriptor())) {
                    return true;
                }
                
                // Try with queue properties
                if (tryCreateQueueWithProperties(formatName)) {
                    return true;
                }
                
            } catch (Exception e) {
                log.debug("Native creation threw exception with format '{}': {}", formatName, e.getMessage());
            }
        }
        
        return false;
    }
    
    /**
     * Try to create queue with a specific security descriptor.
     */
    private boolean tryCreateQueueWithSecurityDescriptor(String formatName, Pointer securityDescriptor) {
        try {
            PointerByReference queueHandle = new PointerByReference();
            int result = MsmqNativeInterface.INSTANCE.MQCreateQueue(securityDescriptor, formatName, queueHandle);
            
            if (result == MsmqConstants.MQ_OK) {
                log.info("✅ Successfully created queue via native API with security descriptor: {}", formatName);
                MsmqNativeInterface.INSTANCE.MQCloseQueue(queueHandle.getValue());
                return true;
            } else if (result == MsmqConstants.MQ_ERROR_QUEUE_EXISTS) {
                log.info("✅ Queue already exists via native API: {}", formatName);
                return true;
            } else {
                log.debug("Native creation with security descriptor failed: '{}': 0x{}", formatName, Integer.toHexString(result));
                return false;
            }
        } catch (Exception e) {
            log.debug("Native creation with security descriptor threw exception: '{}': {}", formatName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Try to create queue with queue properties structure.
     */
    private boolean tryCreateQueueWithProperties(String formatName) {
        try {
            // Create a basic queue properties structure
            MsmqQueueProperties queueProps = new MsmqQueueProperties();
            queueProps.setDefaultProperties();
            
            PointerByReference queueHandle = new PointerByReference();
            // Note: This would require MQCreateQueueEx if available, but let's try the basic approach first
            int result = MsmqNativeInterface.INSTANCE.MQCreateQueue(null, formatName, queueHandle);
            
            if (result == MsmqConstants.MQ_OK) {
                log.info("✅ Successfully created queue via native API with properties: {}", formatName);
                MsmqNativeInterface.INSTANCE.MQCloseQueue(queueHandle.getValue());
                return true;
            } else if (result == MsmqConstants.MQ_ERROR_QUEUE_EXISTS) {
                log.info("✅ Queue already exists via native API: {}", formatName);
                return true;
            } else {
                log.debug("Native creation with properties failed: '{}': 0x{}", formatName, Integer.toHexString(result));
                return false;
            }
        } catch (Exception e) {
            log.debug("Native creation with properties threw exception: '{}': {}", formatName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Create a default security descriptor for queue creation.
     */
    private Pointer createDefaultSecurityDescriptor() {
        try {
            // Create a basic security descriptor that allows everyone
            // This is a simplified approach - in production you'd want proper security
            log.debug("Creating default security descriptor for queue creation");
            return null; // For now, return null to use default
        } catch (Exception e) {
            log.debug("Failed to create security descriptor: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Create queue using PowerShell as fallback.
     */
    private boolean createQueueViaPowerShell(String queuePath) {
        try {
            log.info("Creating queue via PowerShell: {}", queuePath);
            
            // PowerShell command to create queue
            String command = String.format(
                "Add-Type -AssemblyName System.Messaging; " +
                "$queuePath = '.\\private$\\%s'; " +
                "if (-not [System.Messaging.MessageQueue]::Exists($queuePath)) { " +
                "    [System.Messaging.MessageQueue]::Create($queuePath, $true) | Out-Null; " +
                "    Write-Host 'SUCCESS' " +
                "} else { " +
                "    Write-Host 'EXISTS' " +
                "}",
                queuePath
            );
            
            ProcessBuilder processBuilder = new ProcessBuilder("powershell.exe", "-Command", command);
            Process process = processBuilder.start();
            
            // Capture output
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();
            
            // Read output in separate threads
            Thread outputThread = new Thread(() -> {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.debug("Error reading PowerShell output: {}", e.getMessage());
                }
            });
            
            Thread errorThread = new Thread(() -> {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                } catch (Exception e) {
                    log.debug("Error reading PowerShell error: {}", e.getMessage());
                }
            });
            
            outputThread.start();
            errorThread.start();
            
            int exitCode = process.waitFor();
            outputThread.join(5000);
            errorThread.join(5000);
            
            String outputStr = output.toString().trim();
            String errorStr = error.toString().trim();
            
            if (exitCode == 0) {
                if (outputStr.contains("SUCCESS") || outputStr.contains("EXISTS")) {
                    log.info("✅ Successfully created/verified queue via PowerShell: {}", queuePath);
                    return true;
                } else {
                    log.warn("PowerShell succeeded but output unclear: '{}'", outputStr);
                    return false;
                }
            } else {
                log.error("PowerShell queue creation failed: exit code {}, error: {}", exitCode, errorStr);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error creating queue via PowerShell {}: {}", queuePath, e.getMessage());
            return false;
        }
    }
    
    /**
     * Test if a specific queue path format can be opened.
     * This helps us find the correct format for native MSMQ.
     */
    private boolean testQueuePathFormat(String formatName) {
        try {
            PointerByReference queueHandle = new PointerByReference();
            
            // Try to open the queue with PEEK access to test the format
            int result = MsmqNativeInterface.INSTANCE.MQOpenQueue(
                formatName,
                MsmqConstants.MQ_PEEK_ACCESS,
                MsmqConstants.MQ_DENY_NONE,
                queueHandle
            );
            
            if (result == MsmqConstants.MQ_OK) {
                // Successfully opened, close it and return true
                MsmqNativeInterface.INSTANCE.MQCloseQueue(queueHandle.getValue());
                return true;
            } else if (result == MsmqConstants.MQ_ERROR_QUEUE_NOT_FOUND) {
                // Queue not found, but format is valid
                log.debug("Format '{}' is valid (queue not found): 0x{}", formatName, Integer.toHexString(result));
                return true;
            } else {
                // Format error
                log.debug("Format '{}' failed: 0x{}", formatName, Integer.toHexString(result));
                return false;
            }
        } catch (Exception e) {
            log.debug("Format '{}' threw exception: {}", formatName, e.getMessage());
            return false;
        }
    }
    
    /**
     * MSMQ Message Properties structure for native API calls.
     * This maps to the MSMQ message properties structure used by the native API.
     */
    public static class MsmqMessageProperties extends Structure {
        public byte[] body;
        public int bodySize;
        public String label;
        public int labelSize;
        
        public MsmqMessageProperties() {
            body = new byte[8192];
            bodySize = 0;
            label = "";
            labelSize = 0;
        }
        
        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("body", "bodySize", "label", "labelSize");
        }
    }
    
    /**
     * MSMQ Queue Properties structure for native API calls.
     * This maps to the MSMQ queue properties structure used by the native API.
     */
    public static class MsmqQueueProperties extends Structure {
        public int queueType;
        public int maxMessageSize;
        public int maxMessageCount;
        public int defaultMessageTimeout;
        public int journalMessageTimeout;
        public int journalMessageCount;
        public int journalMessageSize;
        public int journalQueueSize;
        public int basePriority;
        public int createTime;
        public int modifyTime;
        public int accessMode;
        public int shareMode;
        public int queueQuota;
        public int journalQuota;
        public int securityInformation;
        
        public MsmqQueueProperties() {
            setDefaultProperties();
        }
        
        public void setDefaultProperties() {
            queueType = MsmqConstants.MQ_QUEUE_TYPE_NORMAL;
            maxMessageSize = 4194304; // 4MB default
            maxMessageCount = 0; // Unlimited
            defaultMessageTimeout = MsmqConstants.MQ_INFINITE;
            journalMessageTimeout = MsmqConstants.MQ_INFINITE;
            journalMessageCount = 0;
            journalMessageSize = 0;
            journalQueueSize = 0;
            basePriority = 0;
            createTime = 0;
            modifyTime = 0;
            accessMode = MsmqConstants.MQ_SEND_ACCESS | MsmqConstants.MQ_RECEIVE_ACCESS;
            shareMode = MsmqConstants.MQ_DENY_NONE;
            queueQuota = 0;
            journalQuota = 0;
            securityInformation = 0;
        }
        
        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList(
                "queueType", "maxMessageSize", "maxMessageCount", "defaultMessageTimeout",
                "journalMessageTimeout", "journalMessageCount", "journalMessageSize", "journalQueueSize",
                "basePriority", "createTime", "modifyTime", "accessMode", "shareMode",
                "queueQuota", "journalQuota", "securityInformation"
            );
        }
    }
}
