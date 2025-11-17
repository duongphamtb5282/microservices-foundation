package com.pacific.core.client;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import com.pacific.core.filter.CorrelationIdFilter;

@Component
public class RestTemplateInterceptor implements ClientHttpRequestInterceptor {

  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  private static final String SERVICE_CALLING_HEADER = "X-Service-Calling";
  private static final String CORRELATION_ID_MDC_KEY = "correlationId";

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    // Propagate correlation ID to downstream services
    String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
    if (correlationId != null) {
      request.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
    }

    // Add calling service identification for better tracing
    request.getHeaders().add(SERVICE_CALLING_HEADER, "backend-core");

    // Add correlation ID to request attributes for client metrics
    request.getHeaders().set(CorrelationIdFilter.CORRELATION_ID_ATTR, correlationId);

    return execution.execute(request, body);
  }
}
