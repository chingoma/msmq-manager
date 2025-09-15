package com.enterprise.msmq.platform.windows;

/**
 * MSMQ constants and access modes for native operations on Windows.
 * Defines the standard MSMQ API constants used in queue operations.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
public final class MsmqConstants {
    
    // Queue Access Modes
    public static final int MQ_RECEIVE_ACCESS = 0x00000001;
    public static final int MQ_SEND_ACCESS = 0x00000002;
    public static final int MQ_PEEK_ACCESS = 0x00000020;
    public static final int MQ_ADMIN_ACCESS = 0x00000080;
    
    // Queue Share Modes
    public static final int MQ_DENY_NONE = 0x00000000;
    public static final int MQ_DENY_RECEIVE_SHARE = 0x00000001;
    
    // Message Actions
    public static final int MQ_ACTION_RECEIVE = 0x00000000;
    public static final int MQ_ACTION_PEEK_CURRENT = 0x00000020;
    public static final int MQ_ACTION_PEEK_NEXT = 0x00000040;
    
    // Queue Types
    public static final int MQ_QUEUE_TYPE_NORMAL = 0x00000000;
    public static final int MQ_QUEUE_TYPE_DEADLETTER = 0x00000001;
    public static final int MQ_QUEUE_TYPE_JOURNAL = 0x00000002;
    
    // Transaction Types
    public static final int MQ_NO_TRANSACTION = 0x00000000;
    public static final int MQ_MTS_TRANSACTION = 0x00000001;
    public static final int MQ_XA_TRANSACTION = 0x00000002;
    public static final int MQ_SINGLE_MESSAGE = 0x00000003;
    
    // Timeout Values
    public static final int MQ_INFINITE = 0xFFFFFFFF;
    public static final int MQ_DEFAULT_TIMEOUT = 0x00000000;
    
    // Message Properties
    public static final int MQMSG_CALG_MD2 = 0x8001;
    public static final int MQMSG_CALG_MD4 = 0x8002;
    public static final int MQMSG_CALG_MD5 = 0x8003;
    public static final int MQMSG_CALG_SHA = 0x8004;
    public static final int MQMSG_CALG_SHA1 = 0x8004;
    
    // Queue Properties
    public static final int MQ_QUEUE_PROPERTY_ID = 1;
    public static final int MQ_QUEUE_PROPERTY_TYPE = 2;
    public static final int MQ_QUEUE_PROPERTY_PATHNAME = 3;
    public static final int MQ_QUEUE_PROPERTY_JOURNAL = 4;
    public static final int MQ_QUEUE_PROPERTY_QUOTA = 5;
    public static final int MQ_QUEUE_PROPERTY_BASEPRIORITY = 6;
    public static final int MQ_QUEUE_PROPERTY_PRIVLEVEL = 7;
    public static final int MQ_QUEUE_PROPERTY_AUTHENTICATE = 8;
    public static final int MQ_QUEUE_PROPERTY_TRANSACTION = 9;
    
    // MSMQ Error Constants
    public static final int MQ_OK = 0;
    public static final int MQ_ERROR_INVALID_HANDLE = 0xC00E0001;
    public static final int MQ_ERROR_INVALID_PARAMETER = 0xC00E0006;
    public static final int MQ_ERROR_QUEUE_NOT_FOUND = 0xC00E0003;
    public static final int MQ_ERROR_ACCESS_DENIED = 0xC00E0025;
    public static final int MQ_ERROR_QUEUE_EXISTS = 0xC00E0005;
    public static final int MQ_ERROR_SHARING_VIOLATION = 0xC00E0007;
    public static final int MQ_ERROR_SERVICE_NOT_AVAILABLE = 0xC00E0008;
    public static final int MQ_ERROR_COMPUTER_DOES_NOT_EXIST = 0xC00E000D;
    public static final int MQ_ERROR_NO_DS = 0xC00E0013;
    public static final int MQ_ERROR_ILLEGAL_QUEUE_PATHNAME = 0xC00E001E;
    public static final int MQ_ERROR_ILLEGAL_PROPERTY_VALUE = 0xC00E001F;
    public static final int MQ_ERROR_ILLEGAL_PROPERTY_VT = 0xC00E0020;
    public static final int MQ_ERROR_BUFFER_OVERFLOW = 0xC00E0021;
    public static final int MQ_ERROR_IO_TIMEOUT = 0xC00E0022;
    public static final int MQ_ERROR_ILLEGAL_CURSOR_ACTION = 0xC00E0023;
    public static final int MQ_ERROR_MESSAGE_ALREADY_RECEIVED = 0xC00E0024;
    public static final int MQ_ERROR_ILLEGAL_FORMATNAME = 0xC00E0026;
    public static final int MQ_ERROR_FORMATNAME_BUFFER_TOO_SMALL = 0xC00E0027;
    public static final int MQ_ERROR_UNSUPPORTED_FORMATNAME_OPERATION = 0xC00E0028;
    public static final int MQ_ERROR_ILLEGAL_SECURITY_DESCRIPTOR = 0xC00E0029;
    public static final int MQ_ERROR_SENDERID_BUFFER_TOO_SMALL = 0xC00E002A;
    public static final int MQ_ERROR_SECURITY_DESCRIPTOR_TOO_SMALL = 0xC00E002B;
    public static final int MQ_ERROR_CANNOT_IMPERSONATE_CLIENT = 0xC00E002C;
    public static final int MQ_ERROR_FORMATNAME_BUFFER_TOO_SMALL_2 = 0xC00E002F;
    public static final int MQ_ERROR_UNSUPPORTED_FORMATNAME_OPERATION_2 = 0xC00E0030;
    public static final int MQ_ERROR_REMOTE_MACHINE_NOT_AVAILABLE = 0xC00E0040;
    
    private MsmqConstants() {
        // Utility class - prevent instantiation
    }
}
