package io.github.wesleyosantos91.infrastructure.availability;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

@Component
public class StartupGateState {

    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicBoolean passed = new AtomicBoolean(false);
    private volatile long durationMs = -1;
    private volatile String failureReason;

    public void markPassed(long durationMs) {
        this.durationMs = durationMs;
        this.failureReason = null;
        this.passed.set(true);
        this.completed.set(true);
    }

    public void markFailed(String failureReason, long durationMs) {
        this.durationMs = durationMs;
        this.failureReason = failureReason;
        this.passed.set(false);
        this.completed.set(true);
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public boolean isPassed() {
        return passed.get();
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
