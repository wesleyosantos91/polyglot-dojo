package io.github.wesleyosantos91.infrastructure.logging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class AdaptiveHttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveHttpLoggingFilter.class);

    private static final String K_EVENT_TYPE  = "event_type";
    private static final String K_HTTP_METHOD = "http_method";
    private static final String K_HTTP_PATH   = "http_path";
    private static final String K_HTTP_STATUS = "http_status";
    private static final String K_LATENCY_MS  = "latency_ms";
    private static final String K_OUTCOME     = "outcome";
    private static final String K_LOG_POLICY  = "log_policy";
    private static final String K_REQUEST_ID = "request_id";
    private static final String K_CORRELATION_ID = "correlation_id";
    private static final String K_TRACE_ID = "trace_id";
    private static final String K_SPAN_ID = "span_id";
    private static final String K_ERROR_MESSAGE = "error.message";
    private static final String K_ERROR_TYPE = "error.type";
    private static final String V_HTTP_ACCESS = "http_access";

    private final LoggingProperties props;
    private final MeterRegistry meterRegistry;
    private final Set<String> maskedHeaderNames;
    private final Set<String> maskedFieldNames;

    public AdaptiveHttpLoggingFilter(LoggingProperties props, MeterRegistry meterRegistry) {
        this.props = props;
        this.meterRegistry = meterRegistry;
        this.maskedHeaderNames = normalizeToLowercaseSet(props.getMaskHeaders());
        this.maskedFieldNames = normalizeToLowercaseSet(props.getMaskJsonFields());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startNanos = System.nanoTime();
        Exception caught = null;

        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            caught = e;
            throw e;
        } finally {
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
            logEvent(request, response, latencyMs, caught);
        }
    }

    private void logEvent(HttpServletRequest request, HttpServletResponse response,
                          long latencyMs, Exception error) {
        int status     = response.getStatus();
        String method  = request.getMethod();
        String path    = request.getRequestURI();
        String outcome = classifyOutcome(status, latencyMs, error != null);
        String policy  = classifyPolicy(status, latencyMs, error != null);
        recordAccessEventMetric(status, outcome);

        if (error != null || status >= 500) {
            logError(method, path, status, latencyMs, outcome, policy, error, request);
        } else if (status >= 400 || latencyMs >= props.getSlowRequestThresholdMs()) {
            logWarn(method, path, status, latencyMs, outcome, policy, request);
        } else {
            logInfo(method, path, status, latencyMs, outcome, policy);
        }
    }

    private void logError(String method, String path, int status, long latencyMs,
                          String outcome, String policy, Exception error,
                          HttpServletRequest request) {
        var event = log.atError()
                .setMessage("http_request_failed")
                .addKeyValue(K_EVENT_TYPE, V_HTTP_ACCESS)
                .addKeyValue(K_HTTP_METHOD, method)
                .addKeyValue(K_HTTP_PATH, path)
                .addKeyValue(K_HTTP_STATUS, status)
                .addKeyValue(K_LATENCY_MS, latencyMs)
                .addKeyValue(K_OUTCOME, outcome)
                .addKeyValue(K_LOG_POLICY, policy);
        event = addTraceContext(event);

        if (error != null) {
            event = event.setCause(error)
                    .addKeyValue(K_ERROR_MESSAGE, sanitize(error.getMessage()))
                    .addKeyValue(K_ERROR_TYPE, error.getClass().getName());
        }
        if (props.isIncludeHeadersOnError()) {
            event = event.addKeyValue("request_headers", extractHeaders(request));
        }
        event.log();
    }

    private void logWarn(String method, String path, int status, long latencyMs,
                         String outcome, String policy, HttpServletRequest request) {
        var event = log.atWarn()
                .setMessage("http_request_elevated")
                .addKeyValue(K_EVENT_TYPE, V_HTTP_ACCESS)
                .addKeyValue(K_HTTP_METHOD, method)
                .addKeyValue(K_HTTP_PATH, path)
                .addKeyValue(K_HTTP_STATUS, status)
                .addKeyValue(K_LATENCY_MS, latencyMs)
                .addKeyValue(K_OUTCOME, outcome)
                .addKeyValue(K_LOG_POLICY, policy);
        event = addTraceContext(event);

        if (props.isIncludeQuerystringOnWarn()) {
            String queryString = maskQueryString(request.getQueryString());
            if (queryString != null && !queryString.isBlank()) {
                event = event.addKeyValue("query_string", queryString);
            }
        }
        event.log();
    }

    private void logInfo(String method, String path, int status, long latencyMs,
                         String outcome, String policy) {
        log.atInfo()
                .setMessage("http_request_completed")
                .addKeyValue(K_EVENT_TYPE, V_HTTP_ACCESS)
                .addKeyValue(K_HTTP_METHOD, method)
                .addKeyValue(K_HTTP_PATH, path)
                .addKeyValue(K_HTTP_STATUS, status)
                .addKeyValue(K_LATENCY_MS, latencyMs)
                .addKeyValue(K_OUTCOME, outcome)
                .addKeyValue(K_LOG_POLICY, policy)
                .addKeyValue(K_CORRELATION_ID, safeMdcValue(K_CORRELATION_ID))
                .addKeyValue(K_REQUEST_ID, safeMdcValue(K_REQUEST_ID))
                .addKeyValue(K_TRACE_ID, safeMdcValue(K_TRACE_ID))
                .addKeyValue(K_SPAN_ID, safeMdcValue(K_SPAN_ID))
                .log();
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : Collections.list(request.getHeaderNames())) {
            String lower = name.toLowerCase(Locale.ROOT);
            String value = maskedHeaderNames.contains(lower) ? "***" : request.getHeader(name);
            headers.put(name, sanitize(value));
        }
        return headers;
    }

    private String sanitize(String message) {
        if (message == null) {
            return "no message";
        }
        int maxLength = Math.max(64, props.getMaxPayloadLogBytes());
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...(truncated)";
    }

    private LoggingEventBuilder addTraceContext(LoggingEventBuilder event) {
        return event
                .addKeyValue(K_CORRELATION_ID, safeMdcValue(K_CORRELATION_ID))
                .addKeyValue(K_REQUEST_ID, safeMdcValue(K_REQUEST_ID))
                .addKeyValue(K_TRACE_ID, safeMdcValue(K_TRACE_ID))
                .addKeyValue(K_SPAN_ID, safeMdcValue(K_SPAN_ID));
    }

    private String safeMdcValue(String key) {
        if (K_TRACE_ID.equals(key)) {
            return firstNonBlank(MDC.get("trace_id"), MDC.get("traceId"));
        }
        if (K_SPAN_ID.equals(key)) {
            return firstNonBlank(MDC.get("span_id"), MDC.get("spanId"));
        }
        return MDC.get(key);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private Set<String> normalizeToLowercaseSet(Iterable<String> values) {
        Set<String> normalized = new HashSet<>();
        if (values == null) {
            return normalized;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private String maskQueryString(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return queryString;
        }

        String[] pairs = queryString.split("&");
        StringBuilder masked = new StringBuilder(queryString.length());
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            int separator = pair.indexOf('=');
            String key = separator >= 0 ? pair.substring(0, separator) : pair;
            String value = separator >= 0 ? pair.substring(separator + 1) : "";
            String normalizedKey = key.toLowerCase(Locale.ROOT);

            if (i > 0) {
                masked.append('&');
            }
            masked.append(key);
            if (separator >= 0) {
                masked.append('=');
                if (maskedFieldNames.contains(normalizedKey)) {
                    masked.append("***");
                } else {
                    masked.append(sanitize(value));
                }
            }
        }
        return masked.toString();
    }

    private void recordAccessEventMetric(int status, String outcome) {
        Counter.builder("http.access.events")
                .description("HTTP access events classified by adaptive outcome")
                .tag("event_type", V_HTTP_ACCESS)
                .tag("outcome", outcome)
                .tag("status_family", statusFamily(status))
                .register(meterRegistry)
                .increment();
    }

    private String statusFamily(int status) {
        if (status >= 500) {
            return "5xx";
        }
        if (status >= 400) {
            return "4xx";
        }
        if (status >= 300) {
            return "3xx";
        }
        if (status >= 200) {
            return "2xx";
        }
        return "1xx";
    }

    private String classifyOutcome(int status, long latencyMs, boolean error) {
        if (error || status >= 500) return "SERVER_ERROR";
        if (status >= 400) return "CLIENT_ERROR";
        if (latencyMs >= props.getSlowRequestThresholdMs()) return "SLOW";
        return "SUCCESS";
    }

    private String classifyPolicy(int status, long latencyMs, boolean error) {
        if (error || status >= 500) return "ERROR_VERBOSE";
        if (status >= 400 || latencyMs >= props.getSlowRequestThresholdMs()) return "ELEVATED";
        return "BASELINE";
    }
}
