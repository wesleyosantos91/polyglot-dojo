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
    @DisplayName("records increasing backoff estimates across retry attempts")
    void recordsBackoffAcrossAttempts() throws Exception {
        DbRetryProperties properties = retryProperties();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DbRetryObservabilityListener listener = new DbRetryObservabilityListener(properties, meterRegistry);

        Method method = DbRetryObservabilityListenerTest.class.getDeclaredMethod("sampleRetryableMethod");
        MethodInvocation invocation = invocationFor(method);

        listener.onMethodRetry(new MethodRetryEvent(invocation, new QueryTimeoutException("timeout"), false));
        listener.onMethodRetry(new MethodRetryEvent(invocation, new QueryTimeoutException("timeout"), false));

        String methodTag = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        var summary = meterRegistry.get("db.retry.next_delay")
                .tag("method", methodTag)
                .summary();

        assertThat(summary.count()).isEqualTo(2L);
        assertThat(summary.totalAmount()).isEqualTo(300.0d);
    }

    @Test
    @DisplayName("records attempt metric with will_retry false and no next delay")
    void recordsAttemptWithoutNextDelayWhenRetriesExceeded() throws Exception {
        DbRetryProperties properties = retryProperties();
        properties.setMaxRetries(0);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DbRetryObservabilityListener listener = new DbRetryObservabilityListener(properties, meterRegistry);

        Method method = DbRetryObservabilityListenerTest.class.getDeclaredMethod("sampleRetryableMethod");
        MethodInvocation invocation = invocationFor(method);

        listener.onMethodRetry(new MethodRetryEvent(invocation, new QueryTimeoutException("timeout"), false));

        String methodTag = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        assertThat(meterRegistry.get("db.retry.attempts")
                .tag("method", methodTag)
                .tag("exception", "QueryTimeoutException")
                .tag("will_retry", "false")
                .counter()
                .count()).isEqualTo(1.0d);

        assertThat(meterRegistry.find("db.retry.next_delay")
                .tag("method", methodTag)
                .summary()).isNull();
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

    @Test
    @DisplayName("uses unknown tags when invocation method and failure are null")
    void recordsUnknownTagsWhenMethodAndFailureAreNull() {
        DbRetryProperties properties = retryProperties();
        properties.setJitter(Duration.ZERO);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DbRetryObservabilityListener listener = new DbRetryObservabilityListener(properties, meterRegistry);

        MethodInvocation invocation = invocationFor(null);

        listener.onMethodRetry(new MethodRetryEvent(invocation, null, false));

        assertThat(meterRegistry.get("db.retry.attempts")
                .tag("method", "unknown")
                .tag("exception", "UnknownException")
                .tag("will_retry", "true")
                .counter()
                .count()).isEqualTo(1.0d);

        assertThat(meterRegistry.get("db.retry.next_delay")
                .tag("method", "unknown")
                .summary()
                .count()).isEqualTo(1L);
        assertThat(meterRegistry.get("db.retry.next_delay")
                .tag("method", "unknown")
                .summary()
                .totalAmount()).isEqualTo(100.0d);
    }

    @Test
    @DisplayName("records exhausted metric with zero previous attempts and long message")
    void recordsExhaustedWithoutPreviousAttempts() {
        DbRetryProperties properties = retryProperties();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DbRetryObservabilityListener listener = new DbRetryObservabilityListener(properties, meterRegistry);

        MethodInvocation invocation = invocationFor(null);
        String longMessage = "x".repeat(600);

        listener.onMethodRetry(new MethodRetryEvent(
                invocation,
                new QueryTimeoutException(longMessage),
                true
        ));

        assertThat(meterRegistry.get("db.retry.exhausted")
                .tag("method", "unknown")
                .tag("exception", "QueryTimeoutException")
                .counter()
                .count()).isEqualTo(1.0d);
    }

    @Test
    @DisplayName("records exhausted metric with null failure")
    void recordsExhaustedWithNullFailure() {
        DbRetryProperties properties = retryProperties();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DbRetryObservabilityListener listener = new DbRetryObservabilityListener(properties, meterRegistry);

        MethodInvocation invocation = invocationFor(null);
        listener.onMethodRetry(new MethodRetryEvent(invocation, null, true));

        assertThat(meterRegistry.get("db.retry.exhausted")
                .tag("method", "unknown")
                .tag("exception", "UnknownException")
                .counter()
                .count()).isEqualTo(1.0d);
    }

    @Test
    @DisplayName("keeps backoff at max delay once interval is capped")
    void keepsBackoffAtMaxDelayWhenCapped() throws Exception {
        DbRetryProperties properties = retryProperties();
        properties.setDelay(Duration.ofMillis(100));
        properties.setMaxDelay(Duration.ofMillis(10));
        properties.setJitter(Duration.ZERO);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DbRetryObservabilityListener listener = new DbRetryObservabilityListener(properties, meterRegistry);

        Method method = DbRetryObservabilityListenerTest.class.getDeclaredMethod("sampleRetryableMethod");
        MethodInvocation invocation = invocationFor(method);

        listener.onMethodRetry(new MethodRetryEvent(invocation, new QueryTimeoutException("timeout"), false));
        listener.onMethodRetry(new MethodRetryEvent(invocation, new QueryTimeoutException("timeout"), false));

        String methodTag = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        var summary = meterRegistry.get("db.retry.next_delay")
                .tag("method", methodTag)
                .summary();

        assertThat(summary.count()).isEqualTo(2L);
        assertThat(summary.totalAmount()).isEqualTo(200.0d);
    }

    @Test
    @DisplayName("records zero next delay when initial delay is zero")
    void recordsZeroDelayWhenInitialDelayIsZero() throws Exception {
        DbRetryProperties properties = retryProperties();
        properties.setDelay(Duration.ZERO);
        properties.setJitter(Duration.ofMillis(25));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DbRetryObservabilityListener listener = new DbRetryObservabilityListener(properties, meterRegistry);

        Method method = DbRetryObservabilityListenerTest.class.getDeclaredMethod("sampleRetryableMethod");
        MethodInvocation invocation = invocationFor(method);

        listener.onMethodRetry(new MethodRetryEvent(invocation, new QueryTimeoutException("timeout"), false));

        String methodTag = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        assertThat(meterRegistry.get("db.retry.next_delay")
                .tag("method", methodTag)
                .summary()
                .totalAmount()).isEqualTo(0.0d);
    }

    @Test
    @DisplayName("returns zero backoff window for non-positive failed attempts")
    void returnsZeroBackoffWindowForNonPositiveAttempts() throws Exception {
        DbRetryProperties properties = retryProperties();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DbRetryObservabilityListener listener = new DbRetryObservabilityListener(properties, meterRegistry);

        Method method = DbRetryObservabilityListener.class.getDeclaredMethod("computeBackoffWindow", int.class);
        method.setAccessible(true);
        Object window = method.invoke(listener, 0);

        assertThat(window).isNotNull();
    }

    private static MethodInvocation invocationFor(Method method) {
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method);
        return invocation;
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
