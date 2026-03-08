package io.github.wesleyosantos91.infrastructure.resilience;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.resilience.retry")
public class DbRetryProperties {

    private long maxRetries = 3;
    private Duration delay = Duration.ofMillis(100);
    private double multiplier = 2.0;
    private Duration jitter = Duration.ofMillis(25);
    private Duration maxDelay = Duration.ofSeconds(2);

    public long getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(long maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getDelay() {
        return delay;
    }

    public void setDelay(Duration delay) {
        this.delay = delay;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public Duration getJitter() {
        return jitter;
    }

    public void setJitter(Duration jitter) {
        this.jitter = jitter;
    }

    public Duration getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(Duration maxDelay) {
        this.maxDelay = maxDelay;
    }
}
