package io.github.wesleyosantos91.infrastructure.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.time.Duration;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.resilience.retry.MethodRetryEvent;

@DisplayName("DbRetryObservabilityListener — Unit Tests")
class DbRetryObservabilityListenerTest {

    @Test
    @DisplayName("records retry attempt metrics with estimated next delay")
    void recordsRetryAttemptMetrics() throws Exception {
        DbRetryProperties properties = retryProperties();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DbRetryObservabilityListener listener = new DbRetryObservabilityListener(properties, meterRegistry);

        Method method = DbRetryObservabilityListenerTest.class.getDeclaredMethod("sampleRetryableMethod");
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method);

        listener.onMethodRetry(new MethodRetryEvent(
                invocation,
                new QueryTimeoutException("timeout"),
                false
        ));

        String methodTag = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        assertThat(meterRegistry.get("db.retry.attempts")
                .tag("method", methodTag)
                .tag("exception", "QueryTimeoutException")
                .tag("will_retry", "true")
                .counter()
                .count()).isEqualTo(1.0d);

        assertThat(meterRegistry.get("db.retry.next_delay")
                .tag("method", methodTag)
                .summary()
                .count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("records exhausted metric when retry is aborted")
    void recordsRetryExhaustedMetric() throws Exception {
        DbRetryProperties properties = retryProperties();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DbRetryObservabilityListener listener = new DbRetryObservabilityListener(properties, meterRegistry);

        Method method = DbRetryObservabilityListenerTest.class.getDeclaredMethod("sampleRetryableMethod");
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method);

        listener.onMethodRetry(new MethodRetryEvent(
                invocation,
                new QueryTimeoutException("timeout"),
                false
        ));
        listener.onMethodRetry(new MethodRetryEvent(
                invocation,
                new QueryTimeoutException("timeout"),
                true
        ));

        String methodTag = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        assertThat(meterRegistry.get("db.retry.exhausted")
                .tag("method", methodTag)
                .tag("exception", "QueryTimeoutException")
                .counter()
                .count()).isEqualTo(1.0d);
    }

    private static DbRetryProperties retryProperties() {
        DbRetryProperties properties = new DbRetryProperties();
        properties.setMaxRetries(3);
        properties.setDelay(Duration.ofMillis(100));
        properties.setMultiplier(2.0d);
        properties.setJitter(Duration.ofMillis(25));
        properties.setMaxDelay(Duration.ofSeconds(2));
        return properties;
    }

    @SuppressWarnings("unused")
    private void sampleRetryableMethod() {
    }
}
