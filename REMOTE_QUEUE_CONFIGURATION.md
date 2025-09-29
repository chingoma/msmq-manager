# Remote Queue Configuration

This document explains how to configure and use the remote MSMQ queue fetching functionality.

## Overview

The MSMQ Manager now supports fetching queues from remote MSMQ servers in addition to the local machine. This is controlled by configuration properties that determine whether to fetch from local or remote machines.

## Configuration Properties

### Basic Configuration

Add the following properties to your `application.properties` or profile-specific properties files:

```properties
# MSMQ Remote Queue Configuration
msmq.remote.enabled=false                    # Enable/disable remote queue fetching
msmq.remote.host=localhost                   # Remote MSMQ server hostname or IP
msmq.remote.port=1801                        # Remote MSMQ server port
msmq.remote.timeout=30000                    # Connection timeout in milliseconds
msmq.remote.retry-attempts=3                 # Number of retry attempts
msmq.remote.use-tcp=true                     # Use TCP protocol (true) or OS protocol (false)
msmq.remote.queue-names=queue1,queue2,queue3 # Comma-separated list of queue names to check
```

### Environment-Specific Examples

#### Development Environment
```properties
# application-dev.properties
msmq.remote.enabled=false
msmq.remote.host=localhost
msmq.remote.port=1801
msmq.remote.timeout=30000
msmq.remote.retry-attempts=3
```

#### Staging Environment
```properties
# application-staging.properties
msmq.remote.enabled=true
msmq.remote.host=staging-msmq-server
msmq.remote.port=1801
msmq.remote.timeout=30000
msmq.remote.retry-attempts=3
```

#### Production Environment
```properties
# application-prod.properties
msmq.remote.enabled=${MSMQ_REMOTE_ENABLED:false}
msmq.remote.host=${MSMQ_REMOTE_HOST:localhost}
msmq.remote.port=${MSMQ_REMOTE_PORT:1801}
msmq.remote.timeout=${MSMQ_REMOTE_TIMEOUT:60000}
msmq.remote.retry-attempts=${MSMQ_REMOTE_RETRY_ATTEMPTS:5}
```

## How It Works

### Local Queue Fetching (Default)
When `msmq.remote.enabled=false` (default), the `getAllQueues()` method fetches queues from the local machine using PowerShell MSMQ cmdlets.

### Remote Queue Fetching
When `msmq.remote.enabled=true`, the `getAllQueues()` method fetches queues from the specified remote MSMQ server using .NET System.Messaging.

## API Usage

### Automatic Mode (Recommended)
```java
// This will automatically use local or remote based on configuration
List<MsmqQueue> queues = msmqQueueManager.getAllQueues();
```

### Manual Remote Mode
```java
// This explicitly fetches from a specific remote host
List<MsmqQueue> queues = msmqQueueManager.getAllRemoteQueues("192.168.1.100");
```

## Protocol Options

### TCP Protocol (Recommended for Remote)
- Uses `TCP:` prefix for queue paths
- Better for cross-network communication
- More reliable for remote connections

### OS Protocol (Native MSMQ)
- Uses `OS:` prefix for queue paths
- Native MSMQ protocol
- May have limitations with remote connections

## Troubleshooting

### Common Issues

1. **Connection Timeout**
   - Increase `msmq.remote.timeout` value
   - Check network connectivity to remote server
   - Verify MSMQ service is running on remote server

2. **Authentication Issues**
   - Ensure proper Windows authentication is configured
   - Check firewall settings for MSMQ ports (1801)
   - Verify remote server allows MSMQ connections

3. **PowerShell Execution Issues**
   - Ensure PowerShell execution policy allows script execution
   - Check that .NET System.Messaging assembly is available
   - Verify MSMQ is installed on both local and remote machines

### Debugging

Enable debug logging to see detailed information about remote queue operations:

```properties
logging.level.com.enterprise.msmq=DEBUG
```

This will show:
- Whether remote or local fetching is being used
- Remote server connection details
- PowerShell command execution results
- Queue parsing and error information

## Security Considerations

1. **Network Security**
   - Use VPN or secure network connections for remote MSMQ access
   - Configure appropriate firewall rules
   - Consider using encrypted connections where possible

2. **Authentication**
   - Ensure proper Windows domain authentication
   - Use service accounts with minimal required permissions
   - Regularly rotate credentials

3. **Monitoring**
   - Monitor remote connection health
   - Set up alerts for connection failures
   - Log all remote queue operations for audit purposes

## Performance Considerations

1. **Timeout Settings**
   - Adjust timeout values based on network latency
   - Consider retry attempts for unreliable networks
   - Monitor performance metrics

2. **Caching**
   - Consider implementing caching for frequently accessed queue information
   - Balance between data freshness and performance

3. **Load Balancing**
   - For high-availability scenarios, consider multiple remote MSMQ servers
   - Implement failover mechanisms

## Migration Guide

### From Local-Only to Remote-Enabled

1. **Update Configuration**
   ```properties
   # Change from local-only to remote-enabled
   msmq.remote.enabled=true
   msmq.remote.host=your-remote-server
   ```

2. **Test Connectivity**
   ```bash
   # Test PowerShell connectivity to remote server
   powershell -Command "Get-MsmqQueue -ComputerName your-remote-server"
   ```

3. **Verify Application**
   - Restart the application
   - Check logs for remote connection status
   - Test queue operations

### Rollback Plan

If issues occur, quickly rollback by setting:
```properties
msmq.remote.enabled=false
```

This will immediately switch back to local queue fetching.
