package io.github.wesleyosantos91.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final String V_HTTP_ACCESS = "http_access";

    private final LoggingProperties props;

    public AdaptiveHttpLoggingFilter(LoggingProperties props) {
        this.props = props;
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

        if (error != null) {
            event = event.setCause(error)
                    .addKeyValue("exception_message", sanitize(error.getMessage()))
                    .addKeyValue("exception_class", error.getClass().getName());
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

        if (props.isIncludeQuerystringOnWarn()) {
            String qs = request.getQueryString();
            if (qs != null && !qs.isBlank()) {
                event = event.addKeyValue("query_string", qs);
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
                .log();
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : Collections.list(request.getHeaderNames())) {
            String lower = name.toLowerCase();
            headers.put(name, props.getMaskHeaders().contains(lower) ? "***" : request.getHeader(name));
        }
        return headers;
    }

    private String sanitize(String message) {
        return message != null ? message : "no message";
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