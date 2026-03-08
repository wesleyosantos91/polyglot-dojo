package io.github.wesleyosantos91.infrastructure.resilience;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.resilience.retry.MethodRetryEvent;
import org.springframework.stereotype.Component;

@Component
public class DbRetryObservabilityListener {

    private static final Logger log = LoggerFactory.getLogger(DbRetryObservabilityListener.class);

    private final DbRetryProperties retryProperties;
    private final MeterRegistry meterRegistry;
    private final Map<Object, RetryAttemptState> attemptStates = Collections.synchronizedMap(new WeakHashMap<>());

    public DbRetryObservabilityListener(DbRetryProperties retryProperties, MeterRegistry meterRegistry) {
        this.retryProperties = retryProperties;
        this.meterRegistry = meterRegistry;
    }

    @EventListener
    public void onMethodRetry(MethodRetryEvent event) {
        String methodTag = methodTag(event.getMethod());
        String exceptionTag = exceptionTag(event.getFailure());

        if (event.isRetryAborted()) {
            int failedAttempts = removeAttemptCount(event.getSource());
            log.atError()
                    .setMessage("db_retry_exhausted")
                    .setCause(event.getFailure())
                    .addKeyValue("event_type", "db_retry")
                    .addKeyValue("method", methodTag)
                    .addKeyValue("exception_class", exceptionTag)
                    .addKeyValue("retry_attempt", failedAttempts)
                    .addKeyValue("max_retries", retryProperties.getMaxRetries())
                    .addKeyValue("retry_aborted", true)
                    .addKeyValue("error.message", sanitize(event.getFailure() != null ? event.getFailure().getMessage() : null))
                    .log();

            meterRegistry.counter("db.retry.exhausted", "method", methodTag, "exception", exceptionTag)
                    .increment();
            return;
        }

        int failedAttempts = incrementAttemptCount(event.getSource());
        boolean willRetry = failedAttempts <= retryProperties.getMaxRetries();

        RetryBackoffWindow window = computeBackoffWindow(failedAttempts);
        var eventBuilder = log.atWarn()
                .setMessage("db_retry_scheduled")
                .setCause(event.getFailure())
                .addKeyValue("event_type", "db_retry")
                .addKeyValue("method", methodTag)
                .addKeyValue("exception_class", exceptionTag)
                .addKeyValue("retry_attempt", failedAttempts)
                .addKeyValue("max_retries", retryProperties.getMaxRetries())
                .addKeyValue("retry_will_retry", willRetry)
                .addKeyValue("error.message", sanitize(event.getFailure() != null ? event.getFailure().getMessage() : null));

        if (willRetry) {
            eventBuilder = eventBuilder
                    .addKeyValue("next_delay_ms", window.nextDelayMs())
                    .addKeyValue("next_delay_min_ms", window.nextDelayMinMs())
                    .addKeyValue("next_delay_max_ms", window.nextDelayMaxMs());

            DistributionSummary.builder("db.retry.next_delay")
                    .description("Estimated retry backoff delay in milliseconds for DB retries")
                    .baseUnit("milliseconds")
                    .tag("method", methodTag)
                    .register(meterRegistry)
                    .record(window.nextDelayMs());
        }

        eventBuilder.log();

        meterRegistry.counter(
                        "db.retry.attempts",
                        "method", methodTag,
                        "exception", exceptionTag,
                        "will_retry", Boolean.toString(willRetry))
                .increment();
    }

    private int incrementAttemptCount(Object invocationSource) {
        synchronized (attemptStates) {
            RetryAttemptState state = attemptStates.get(invocationSource);
            if (state == null) {
                state = new RetryAttemptState();
                attemptStates.put(invocationSource, state);
            }
            return state.increment();
        }
    }

    private int removeAttemptCount(Object invocationSource) {
        synchronized (attemptStates) {
            RetryAttemptState state = attemptStates.remove(invocationSource);
            return state != null ? state.failedAttempts() : 0;
        }
    }

    private RetryBackoffWindow computeBackoffWindow(int failedAttempts) {
        if (failedAttempts <= 0 || failedAttempts > retryProperties.getMaxRetries()) {
            return new RetryBackoffWindow(0, 0, 0);
        }

        long initialDelayMs = Math.max(0, retryProperties.getDelay().toMillis());
        long maxDelayMs = Math.max(initialDelayMs, retryProperties.getMaxDelay().toMillis());
        double multiplier = Math.max(1.0, retryProperties.getMultiplier());

        long currentIntervalMs = initialDelayMs;
        for (int i = 1; i < failedAttempts; i++) {
            if (currentIntervalMs >= maxDelayMs) {
                currentIntervalMs = maxDelayMs;
            } else {
                currentIntervalMs = Math.min((long) (currentIntervalMs * multiplier), maxDelayMs);
            }
        }

        long jitterMs = Math.max(0, retryProperties.getJitter().toMillis());
        if (jitterMs <= 0 || initialDelayMs <= 0) {
            long interval = Math.min(currentIntervalMs, maxDelayMs);
            return new RetryBackoffWindow(interval, interval, interval);
        }

        long jitterRangeMs = jitterMs * currentIntervalMs / initialDelayMs;
        long minMs = Math.max(currentIntervalMs - jitterRangeMs, initialDelayMs);
        long maxMs = Math.min(currentIntervalMs + jitterRangeMs, maxDelayMs);
        long estimateMs = Math.min(currentIntervalMs, maxDelayMs);
        return new RetryBackoffWindow(estimateMs, minMs, maxMs);
    }

    private String methodTag(Method method) {
        if (method == null) {
            return "unknown";
        }
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    private String exceptionTag(Throwable failure) {
        if (failure == null) {
            return "UnknownException";
        }
        return failure.getClass().getSimpleName();
    }

    private String sanitize(String message) {
        if (message == null) {
            return "no message";
        }
        int maxLength = 512;
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...(truncated)";
    }

    private static final class RetryAttemptState {
        private int failedAttempts;

        int increment() {
            failedAttempts++;
            return failedAttempts;
        }

        int failedAttempts() {
            return failedAttempts;
        }
    }

    private record RetryBackoffWindow(long nextDelayMs, long nextDelayMinMs, long nextDelayMaxMs) {
    }
}
