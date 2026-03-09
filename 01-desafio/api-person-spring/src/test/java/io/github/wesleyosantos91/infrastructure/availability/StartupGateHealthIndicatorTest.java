package io.github.wesleyosantos91.infrastructure.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.health.contributor.Health;

@DisplayName("StartupGateHealthIndicator - Unit Tests")
class StartupGateHealthIndicatorTest {

    @Test
    @DisplayName("returns UP with disabled detail when startup gate runner is not present")
    void returnsUpWhenGateIsDisabled() {
        StartupGateState state = new StartupGateState();
        StartupGateHealthIndicator indicator = new StartupGateHealthIndicator(
                state,
                emptyProvider(StartupReadinessGateRunner.class)
        );

        Health health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsEntry("startup_gate", "disabled");
    }

    @Test
    @DisplayName("returns OUT_OF_SERVICE while startup gate is running")
    void returnsOutOfServiceWhileRunning() {
        StartupGateState state = new StartupGateState();
        StartupGateHealthIndicator indicator = new StartupGateHealthIndicator(
                state,
                providerOf(mock(StartupReadinessGateRunner.class), StartupReadinessGateRunner.class)
        );

        Health health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("OUT_OF_SERVICE");
        assertThat(health.getDetails()).containsEntry("startup_gate", "running");
    }

    @Test
    @DisplayName("returns UP with duration when startup gate has passed")
    void returnsUpWhenGatePassed() {
        StartupGateState state = new StartupGateState();
        state.markPassed(123L);
        StartupGateHealthIndicator indicator = new StartupGateHealthIndicator(
                state,
                providerOf(mock(StartupReadinessGateRunner.class), StartupReadinessGateRunner.class)
        );

        Health health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsEntry("startup_gate", "passed");
        assertThat(health.getDetails()).containsEntry("duration_ms", 123L);
    }

    @Test
    @DisplayName("returns DOWN with reason when startup gate has failed")
    void returnsDownWhenGateFailed() {
        StartupGateState state = new StartupGateState();
        state.markFailed("db timeout", 321L);
        StartupGateHealthIndicator indicator = new StartupGateHealthIndicator(
                state,
                providerOf(mock(StartupReadinessGateRunner.class), StartupReadinessGateRunner.class)
        );

        Health health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
        assertThat(health.getDetails()).containsEntry("startup_gate", "failed");
        assertThat(health.getDetails()).containsEntry("duration_ms", 321L);
        assertThat(health.getDetails()).containsEntry("reason", "db timeout");
    }

    private static <T> ObjectProvider<T> emptyProvider(Class<T> type) {
        return new StaticListableBeanFactory().getBeanProvider(type);
    }

    private static <T> ObjectProvider<T> providerOf(T bean, Class<T> type) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("bean", bean);
        return beanFactory.getBeanProvider(type);
    }
}
