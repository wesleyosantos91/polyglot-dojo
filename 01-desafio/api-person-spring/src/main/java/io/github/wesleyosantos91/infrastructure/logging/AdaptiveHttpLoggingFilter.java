package io.github.wesleyosantos91.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
        Throwable error = null;

        try {
            filterChain.doFilter(request, response);
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
            int status = response.getStatus();

            String outcome = classifyOutcome(status, latencyMs, error != null);
            String policy = classifyPolicy(status, latencyMs, error != null);

            var event = log.atInfo()
                    .setMessage("http_request_completed")
                    .addKeyValue("event_type", "http_access")
                    .addKeyValue("http_status", status)
                    .addKeyValue("latency_ms", latencyMs)
                    .addKeyValue("outcome", outcome)
                    .addKeyValue("log_policy", policy);

            if (error != null || status >= 500) {
                event = log.atError()
                        .setMessage("http_request_failed")
                        .addKeyValue("event_type", "http_access")
                        .addKeyValue("http_status", status)
                        .addKeyValue("latency_ms", latencyMs)
                        .addKeyValue("outcome", outcome)
                        .addKeyValue("log_policy", policy);

                if (error != null) {
                    event = event.setCause(error)
                            .addKeyValue("exception_class", error.getClass().getName());
                }
            } else if (status >= 400 || latencyMs >= props.getSlowRequestThresholdMs()) {
                event = log.atWarn()
                        .setMessage("http_request_elevated")
                        .addKeyValue("event_type", "http_access")
                        .addKeyValue("http_status", status)
                        .addKeyValue("latency_ms", latencyMs)
                        .addKeyValue("outcome", outcome)
                        .addKeyValue("log_policy", policy);
            }

            if ((status >= 400 || latencyMs >= props.getSlowRequestThresholdMs()) && props.isIncludeQuerystringOnWarn()) {
                String qs = request.getQueryString();
                if (qs != null && !qs.isBlank()) {
                    event = event.addKeyValue("query_string", qs);
                }
            }

            event.log();
        }
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