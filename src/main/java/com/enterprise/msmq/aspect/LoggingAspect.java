package com.enterprise.msmq.aspect;

import com.enterprise.msmq.dto.ApiResponse;
import com.enterprise.msmq.util.RequestIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * AOP aspect for comprehensive request/response logging.
 * Logs all incoming requests and outgoing responses for monitoring and debugging.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Pointcut for all MSMQ controller methods.
     */
    @Pointcut("execution(* com.enterprise.msmq.controller.MsmqController.*(..))")
    public void msmqControllerMethods() {}

    /**
     * Pointcut for all service methods.
     */
    @Pointcut("execution(* com.enterprise.msmq.service.MsmqService.*(..))")
    public void msmqServiceMethods() {}

    /**
     * Around advice for logging controller requests and responses.
     * 
     * @param joinPoint the join point
     * @return the method result
     * @throws Throwable if the method execution fails
     */
    @Around("msmqControllerMethods()")
    public Object logControllerRequestResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = RequestIdGenerator.generateRequestId();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // Log request details
        logRequest(requestId, className, methodName, joinPoint.getArgs());
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;
        
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            logResponse(requestId, className, methodName, result, exception, executionTime);
        }
    }

    /**
     * Around advice for logging service method executions.
     * 
     * @param joinPoint the join point
     * @return the method result
     * @throws Throwable if the method execution fails
     */
    @Around("msmqServiceMethods()")
    public Object logServiceMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        logger.debug("Executing service method: {}.{}", className, methodName);
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;
        
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            logger.error("Service method {}.{} failed: {}", className, methodName, e.getMessage(), e);
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.debug("Service method {}.{} completed in {}ms", className, methodName, executionTime);
        }
    }

    /**
     * Logs the incoming request details.
     * 
     * @param requestId the unique request identifier
     * @param className the class name
     * @param methodName the method name
     * @param args the method arguments
     */
    private void logRequest(String requestId, String className, String methodName, Object[] args) {
        try {
            HttpServletRequest request = getCurrentRequest();
            String clientIp = getClientIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : "Unknown";
            
            logger.info("REQUEST [{}] - {}:{} - IP: {} - User-Agent: {} - Args: {}", 
                requestId, className, methodName, clientIp, userAgent, 
                args != null ? objectMapper.writeValueAsString(args) : "null");
        } catch (Exception e) {
            logger.warn("Failed to log request details: {}", e.getMessage());
        }
    }

    /**
     * Logs the outgoing response details.
     * 
     * @param requestId the unique request identifier
     * @param className the class name
     * @param methodName the method name
     * @param result the method result
     * @param exception the exception if any
     * @param executionTime the execution time in milliseconds
     */
    private void logResponse(String requestId, String className, String methodName, 
                           Object result, Exception exception, long executionTime) {
        try {
            if (exception != null) {
                logger.error("RESPONSE [{}] - {}:{} - FAILED in {}ms - Error: {}", 
                    requestId, className, methodName, executionTime, exception.getMessage());
            } else {
                String resultSummary = result instanceof ApiResponse ? 
                    "ApiResponse with status: " + ((ApiResponse<?>) result).getStatusCode() :
                    "Result: " + (result != null ? result.getClass().getSimpleName() : "null");
                
                logger.info("RESPONSE [{}] - {}:{} - SUCCESS in {}ms - {}", 
                    requestId, className, methodName, executionTime, resultSummary);
            }
        } catch (Exception e) {
            logger.warn("Failed to log response details: {}", e.getMessage());
        }
    }

    /**
     * Gets the current HTTP request from the request context.
     * 
     * @return the current HTTP request or null if not available
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the client IP address from the request.
     * 
     * @param request the HTTP request
     * @return the client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
