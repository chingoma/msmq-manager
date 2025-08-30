package com.enterprise.msmq.aspect;

import com.enterprise.msmq.util.RequestIdGenerator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * AOP aspect for monitoring API performance and collecting metrics.
 * Tracks execution times, success/failure rates, and provides Prometheus metrics.
 * 
 * @author Enterprise MSMQ Team
 * @version 1.0
 */
@Aspect
@Component
public class MonitoringAspect {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringAspect.class);
    
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Timer> timerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> successCounterCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> failureCounterCache = new ConcurrentHashMap<>();

    public MonitoringAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Pointcut for all MSMQ controller methods.
     */
    @Pointcut("execution(* com.enterprise.msmq.controller.MsmqController.*(..))")
    public void msmqControllerMethods() {}

    /**
     * Pointcut for all service methods.
     */
    @Pointcut("execution(* com.enterprise.msmq.service.contracts.IMsmqService.*(..))")
    public void msmqServiceMethods() {}

    /**
     * Around advice for monitoring controller method performance.
     * 
     * @param joinPoint the join point
     * @return the method result
     * @throws Throwable if the method execution fails
     */
    @Around("msmqControllerMethods()")
    public Object monitorControllerPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String metricName = String.format("%s.%s", className, methodName);
        
        Timer.Sample sample = Timer.start(meterRegistry);
        String requestId = RequestIdGenerator.generateRequestId();
        
        try {
            Object result = joinPoint.proceed();
            recordSuccess(metricName, sample);
            logger.debug("Request [{}] completed successfully: {}.{}", requestId, className, methodName);
            return result;
        } catch (Exception e) {
            recordFailure(metricName, sample, e);
            logger.error("Request [{}] failed: {}.{} - Error: {}", requestId, className, methodName, e.getMessage());
            throw e;
        }
    }

    /**
     * Around advice for monitoring service method performance.
     * 
     * @param joinPoint the join point
     * @return the method result
     * @throws Throwable if the method execution fails
     */
    @Around("msmqServiceMethods()")
    public Object monitorServicePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String metricName = String.format("%s.%s", className, methodName);
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            Object result = joinPoint.proceed();
            recordSuccess(metricName, sample);
            return result;
        } catch (Exception e) {
            recordFailure(metricName, sample, e);
            throw e;
        }
    }

    /**
     * Records a successful operation completion.
     * 
     * @param metricName the metric name
     * @param sample the timer sample
     */
    private void recordSuccess(String metricName, Timer.Sample sample) {
        Timer timer = getOrCreateTimer(metricName);
        sample.stop(timer);
        
        Counter successCounter = getOrCreateSuccessCounter(metricName);
        successCounter.increment();
    }

    /**
     * Records a failed operation.
     * 
     * @param metricName the metric name
     * @param sample the timer sample
     * @param exception the exception that occurred
     */
    private void recordFailure(String metricName, Timer.Sample sample, Exception exception) {
        Timer timer = getOrCreateTimer(metricName);
        sample.stop(timer);
        
        Counter failureCounter = getOrCreateFailureCounter(metricName);
        failureCounter.increment();
        
        // Record exception-specific metrics
        String exceptionType = exception.getClass().getSimpleName();
        Counter exceptionCounter = meterRegistry.counter("msmq.exceptions", 
            "method", metricName, "exception", exceptionType);
        exceptionCounter.increment();
    }

    /**
     * Gets or creates a timer for the specified metric name.
     * 
     * @param metricName the metric name
     * @return the timer
     */
    private Timer getOrCreateTimer(String metricName) {
        return timerCache.computeIfAbsent(metricName, name -> 
            Timer.builder("msmq.method.execution.time")
                .tag("method", name)
                .description("Execution time for MSMQ methods")
                .register(meterRegistry));
    }

    /**
     * Gets or creates a success counter for the specified metric name.
     * 
     * @param metricName the metric name
     * @return the success counter
     */
    private Counter getOrCreateSuccessCounter(String metricName) {
        return successCounterCache.computeIfAbsent(metricName, name -> 
            Counter.builder("msmq.method.success.count")
                .tag("method", name)
                .description("Success count for MSMQ methods")
                .register(meterRegistry));
    }

    /**
     * Gets or creates a failure counter for the specified metric name.
     * 
     * @param metricName the metric name
     * @return the failure counter
     */
    private Counter getOrCreateFailureCounter(String metricName) {
        return failureCounterCache.computeIfAbsent(metricName, name -> 
            Counter.builder("msmq.method.failure.count")
                .tag("method", name)
                .description("Failure count for MSMQ methods")
                .register(meterRegistry));
    }

    /**
     * Gets the current performance metrics for all monitored methods.
     * 
     * @return a summary of current metrics
     */
    public String getMetricsSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("MSMQ Performance Metrics Summary:\n");
        
        timerCache.forEach((methodName, timer) -> {
            summary.append(String.format("Method: %s\n", methodName));
            summary.append(String.format("  - Total Count: %d\n", timer.count()));
            summary.append(String.format("  - Mean Time: %.2f ms\n", timer.mean(TimeUnit.MILLISECONDS)));
            summary.append(String.format("  - Max Time: %.2f ms\n", timer.max(TimeUnit.MILLISECONDS)));
            summary.append(String.format("  - 95th Percentile: %.2f ms\n", 
                timer.percentile(0.95, TimeUnit.MILLISECONDS)));
            
            Counter successCounter = successCounterCache.get(methodName);
            Counter failureCounter = failureCounterCache.get(methodName);
            
            if (successCounter != null) {
                summary.append(String.format("  - Success Count: %d\n", (long) successCounter.count()));
            }
            if (failureCounter != null) {
                summary.append(String.format("  - Failure Count: %d\n", (long) failureCounter.count()));
            }
            summary.append("\n");
        });
        
        return summary.toString();
    }
}
