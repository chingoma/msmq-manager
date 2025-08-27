package com.enterprise.msmq.msmq.win32;


import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

/**
 * Native MSMQ interface using JNA to connect to real MSMQ services.
 * Maps Windows MSMQ API functions for queue operations.
 */
public interface MsmqNativeInterface extends Library {
    
    MsmqNativeInterface INSTANCE = Native.load("mqrt.dll", MsmqNativeInterface.class);
    
    // MSMQ Queue Management Functions
    int MQOpenQueue(String lpszFormatName, int dwAccess, int dwShareMode, PointerByReference phQueue);
    int MQCloseQueue(Pointer hQueue);
    int MQCreateQueue(Pointer pSecurityDescriptor, String lpszFormatName, PointerByReference phQueue);
    int MQDeleteQueue(String lpszFormatName);
    
    // Message Operations
    int MQSendMessage(Pointer hDestinationQueue, Pointer pMsgProps, Pointer pTransaction);
    int MQReceiveMessage(Pointer hSourceQueue, int dwTimeout, int dwAction, Pointer pMsgProps, Pointer pOverlapped, Pointer pTransaction);
    int MQReceiveMessage2(Pointer hSourceQueue, int dwTimeout, int dwAction, Pointer pMsgProps, Pointer pOverlapped, Pointer pTransaction);
    
    // Queue Properties
    int MQGetQueueProperties(String lpszFormatName, Pointer pQueueProps);
    int MQSetQueueProperties(String lpszFormatName, Pointer pQueueProps);
    
    // Error Constants
    int MQ_OK = 0;
    int MQ_ERROR_INVALID_HANDLE = 0xC00E0001;
    int MQ_ERROR_INVALID_PARAMETER = 0xC00E0006;
    int MQ_ERROR_QUEUE_NOT_FOUND = 0xC00E0003;
    int MQ_ERROR_ACCESS_DENIED = 0xC00E0025;
}
