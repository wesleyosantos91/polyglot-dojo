package io.github.wesleyosantos91.infrastructure.availability;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

@Component("startupGate")
public class StartupGateHealthIndicator implements HealthIndicator {

    private final StartupGateState startupGateState;
    private final ObjectProvider<StartupReadinessGateRunner> startupReadinessGateRunner;

    public StartupGateHealthIndicator(
            StartupGateState startupGateState,
            ObjectProvider<StartupReadinessGateRunner> startupReadinessGateRunner
    ) {
        this.startupGateState = startupGateState;
        this.startupReadinessGateRunner = startupReadinessGateRunner;
    }

    @Override
    public Health health() {
        if (startupReadinessGateRunner.getIfAvailable() == null) {
            return Health.up()
                    .withDetail("startup_gate", "disabled")
                    .build();
        }

        if (!startupGateState.isCompleted()) {
            return Health.status(Status.OUT_OF_SERVICE)
                    .withDetail("startup_gate", "running")
                    .build();
        }

        if (startupGateState.isPassed()) {
            return Health.up()
                    .withDetail("startup_gate", "passed")
                    .withDetail("duration_ms", startupGateState.getDurationMs())
                    .build();
        }

        return Health.down()
                .withDetail("startup_gate", "failed")
                .withDetail("duration_ms", startupGateState.getDurationMs())
                .withDetail("reason", startupGateState.getFailureReason())
                .build();
    }
}
