package com.enterprise.msmq.msmq.win32;

/**
 * MSMQ constants and access modes for native operations.
 * Defines the standard MSMQ API constants used in queue operations.
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
    
    private MsmqConstants() {
        // Utility class - prevent instantiation
    }
}
