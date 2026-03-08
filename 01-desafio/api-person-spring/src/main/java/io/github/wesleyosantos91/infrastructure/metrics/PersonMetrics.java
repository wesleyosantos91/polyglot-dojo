package io.github.wesleyosantos91.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PersonMetrics {

    private final MeterRegistry registry;

    public PersonMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Increments the counter for a business-level error.
     * Used for dynamic error codes that cannot be expressed with @Counted static extraTags.
     *
     * @param errorCode the domain error code (e.g. PERSON_EMAIL_ALREADY_EXISTS, RESOURCE_BUSY)
     */
    public void recordError(String errorCode) {
        Counter.builder("person.errors")
                .description("Person business errors classified by error code")
                .tag("error_code", errorCode)
                .register(registry)
                .increment();
    }
}
