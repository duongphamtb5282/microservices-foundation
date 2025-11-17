package com.pacific.core;

import com.pacific.core.monitoring.MonitoringProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * Aspect for logging repository method executions with performance metrics.
 * 
 * Features:
 * - Execution time measurement
 * - Correlation ID support via MDC
 * - Configurable via monitoring properties
 * - Exception handling and logging
 * - Performance threshold warnings
 * - Smart argument and return value logging
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(MonitoringProperties.class)
@ConditionalOnProperty(
    name = "monitoring.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RepositoryLoggingAspect {

  private static final String CORRELATION_ID_MDC_KEY = "correlationId";
  private static final long SLOW_QUERY_THRESHOLD_MS = 1000; // 1 second
  private static final int MAX_ARGUMENT_STRING_LENGTH = 200;
  private static final int MAX_COLLECTION_SIZE_TO_LOG = 10;

  private final MonitoringProperties monitoringProperties;

  /**
   * Pointcut to match all repository methods.
   * Matches any method within a class annotated with @Repository.
   */
  @Pointcut("within(@org.springframework.stereotype.Repository *)")
  public void repositoryMethods() {}

  /**
   * Around advice that logs repository method execution with timing information.
   * 
   * @param joinPoint The join point representing the method execution
   * @return The result of the method execution
   * @throws Throwable If the method execution throws an exception
   */
  @Around("repositoryMethods()")
  public Object logRepositoryMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
    // Early return if logging is disabled
    if (!monitoringProperties.isEnableLogging()) {
      return joinPoint.proceed();
    }

    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = joinPoint.getSignature().getName();
    String fullMethodName = className + "." + methodName;
    String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);

    // Log method entry with arguments
    if (log.isDebugEnabled()) {
      Object[] args = joinPoint.getArgs();
      String argsString = formatArguments(args);
      log.debug(
          "[Repository] Executing: {} | Args: {} | CorrelationId: {}",
          fullMethodName,
          argsString,
          correlationId != null ? correlationId : "N/A");
    }

    long startTime = System.currentTimeMillis();
    Object result = null;
    Throwable exception = null;

    try {
      // Execute the repository method
      result = joinPoint.proceed();
      return result;
    } catch (Throwable t) {
      exception = t;
      throw t;
    } finally {
      // Log execution completion
      long executionTime = System.currentTimeMillis() - startTime;
      logExecutionResult(
          fullMethodName, correlationId, executionTime, result, exception);
    }
  }

  /**
   * Logs the execution result with appropriate log level based on execution time and outcome.
   */
  private void logExecutionResult(
      String fullMethodName,
      String correlationId,
      long executionTime,
      Object result,
      Throwable exception) {
    
    String correlationIdStr = correlationId != null ? correlationId : "N/A";
    
    if (exception != null) {
      // Log exception with error level
      log.error(
          "[Repository] FAILED: {} | ExecutionTime: {}ms | Exception: {} | CorrelationId: {}",
          fullMethodName,
          executionTime,
          exception.getClass().getSimpleName() + ": " + exception.getMessage(),
          correlationIdStr,
          exception);
    } else if (executionTime > SLOW_QUERY_THRESHOLD_MS) {
      // Log slow queries with warn level
      String resultSummary = formatReturnValue(result);
      log.warn(
          "[Repository] SLOW QUERY: {} | ExecutionTime: {}ms | Result: {} | CorrelationId: {}",
          fullMethodName,
          executionTime,
          resultSummary,
          correlationIdStr);
    } else if (log.isDebugEnabled()) {
      // Log normal execution with debug level
      String resultSummary = formatReturnValue(result);
      log.debug(
          "[Repository] Completed: {} | ExecutionTime: {}ms | Result: {} | CorrelationId: {}",
          fullMethodName,
          executionTime,
          resultSummary,
          correlationIdStr);
    } else if (log.isTraceEnabled()) {
      // Log detailed information with trace level
      String resultSummary = formatReturnValue(result);
      log.trace(
          "[Repository] Completed: {} | ExecutionTime: {}ms | Result: {} | CorrelationId: {}",
          fullMethodName,
          executionTime,
          resultSummary,
          correlationIdStr);
    }
  }

  /**
   * Formats method arguments for logging, truncating long values.
   */
  private String formatArguments(Object[] args) {
    if (args == null || args.length == 0) {
      return "[]";
    }

    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < args.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(formatArgument(args[i]));
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * Formats a single argument for logging.
   */
  private String formatArgument(Object arg) {
    if (arg == null) {
      return "null";
    }

    String argString = arg.toString();
    
    // Truncate very long strings
    if (argString.length() > MAX_ARGUMENT_STRING_LENGTH) {
      return argString.substring(0, MAX_ARGUMENT_STRING_LENGTH) + "...(truncated)";
    }
    
    return argString;
  }

  /**
   * Formats return value for logging, handling collections and large objects.
   */
  private String formatReturnValue(Object result) {
    if (result == null) {
      return "null";
    }

    // Handle collections
    if (result instanceof Collection<?> collection) {
      int size = collection.size();
      if (size == 0) {
        return "empty " + result.getClass().getSimpleName();
      }
      if (size > MAX_COLLECTION_SIZE_TO_LOG) {
        return result.getClass().getSimpleName() + "[" + size + " items]";
      }
      return result.getClass().getSimpleName() + "[" + size + " items: " + 
          collection.stream()
              .limit(MAX_COLLECTION_SIZE_TO_LOG)
              .map(this::formatArgument)
              .reduce((a, b) -> a + ", " + b)
              .orElse("") + "]";
    }

    // Handle maps
    if (result instanceof Map<?, ?> map) {
      int size = map.size();
      if (size == 0) {
        return "empty " + result.getClass().getSimpleName();
      }
      if (size > MAX_COLLECTION_SIZE_TO_LOG) {
        return result.getClass().getSimpleName() + "[" + size + " entries]";
      }
      return result.getClass().getSimpleName() + "[" + size + " entries]";
    }

    // Handle Optional
    if (result instanceof java.util.Optional<?> optional) {
      return optional.isPresent() 
          ? "Optional[" + formatArgument(optional.get()) + "]"
          : "Optional.empty";
    }

    // Handle Mono/Flux (reactive types) - don't block, just indicate type
    String className = result.getClass().getSimpleName();
    if (className.contains("Mono") || className.contains("Flux")) {
      return className + "[reactive - not resolved]";
    }

    // For other objects, use toString but truncate if too long
    String resultString = result.toString();
    if (resultString.length() > MAX_ARGUMENT_STRING_LENGTH) {
      return resultString.substring(0, MAX_ARGUMENT_STRING_LENGTH) + "...(truncated)";
    }

    return resultString;
  }
}
