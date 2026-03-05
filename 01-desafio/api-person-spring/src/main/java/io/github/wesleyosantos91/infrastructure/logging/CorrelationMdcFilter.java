package io.github.wesleyosantos91.infrastructure.logging;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CorrelationMdcFilter extends OncePerRequestFilter {

    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    private final ObjectProvider<Tracer> tracerProvider;

    public CorrelationMdcFilter(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String incomingCorrelationId = request.getHeader(HEADER_CORRELATION_ID);
        String correlationId = (incomingCorrelationId != null && !incomingCorrelationId.isBlank())
                ? incomingCorrelationId
                : UUID.randomUUID().toString();

        MDC.put("correlation_id", correlationId);
        MDC.put("http_method", request.getMethod());
        MDC.put("http_path", request.getRequestURI());
        MDC.put("client_ip", extractClientIp(request));
        Optional.ofNullable(request.getHeader("User-Agent")).ifPresent(v -> MDC.put("user_agent", v));

        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null && currentSpan.context() != null) {
                String traceId = currentSpan.context().traceId();
                String spanId = currentSpan.context().spanId();

                if (traceId != null) {
                    MDC.put("trace_id", traceId);
                    MDC.put("traceId", traceId);
                    if (incomingCorrelationId == null || incomingCorrelationId.isBlank()) {
                        MDC.put("correlation_id", traceId);
                    }
                }
                if (spanId != null) {
                    MDC.put("span_id", spanId);
                    MDC.put("spanId", spanId);
                }
            }
        }

        response.setHeader(HEADER_CORRELATION_ID, MDC.get("correlation_id"));

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return (realIp != null && !realIp.isBlank()) ? realIp : request.getRemoteAddr();
    }
}