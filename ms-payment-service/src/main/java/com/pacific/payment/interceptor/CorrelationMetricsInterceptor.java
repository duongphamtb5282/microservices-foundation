package com.pacific.payment.interceptor;

import com.pacific.payment.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class CorrelationMetricsInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) throws Exception {
        
        // Ensure correlation ID is available for all metrics
        String correlationId = (String) request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTR);
        if (correlationId != null) {
            // Already handled by the filter, just ensuring it's there
            request.setAttribute("correlationId", correlationId);
        }
        
        return true;
    }
}
