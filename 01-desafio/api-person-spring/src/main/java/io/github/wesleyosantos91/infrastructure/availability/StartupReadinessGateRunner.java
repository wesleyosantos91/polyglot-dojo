package io.github.wesleyosantos91.infrastructure.availability;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "app.availability.gates", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StartupReadinessGateRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupReadinessGateRunner.class);

    private final ObjectProvider<DataSource> writerDataSource;
    private final ObjectProvider<DataSource> readerDataSource;
    private final StartupGateState startupGateState;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${app.availability.gates.db-validation-query:SELECT 1}")
    private String dbValidationQuery;

    public StartupReadinessGateRunner(
            @Qualifier("writerDataSource") ObjectProvider<DataSource> writerDataSource,
            @Qualifier("readerDataSource") ObjectProvider<DataSource> readerDataSource,
            StartupGateState startupGateState,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.writerDataSource = writerDataSource;
        this.readerDataSource = readerDataSource;
        this.startupGateState = startupGateState;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void run(ApplicationArguments args) {
        AvailabilityChangeEvent.publish(applicationEventPublisher, this, ReadinessState.REFUSING_TRAFFIC);
        long start = System.nanoTime();

        try {
            DataSource writer = writerDataSource.getIfAvailable();
            DataSource reader = readerDataSource.getIfAvailable();
            if (writer == null || reader == null) {
                long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
                startupGateState.markPassed(durationMs);
                log.info("event_type=startup_gate status=skipped_missing_datasource duration_ms={}", durationMs);
                return;
            }

            assertDataSourceIsReady("writer", writer);
            assertDataSourceIsReady("reader", reader);

            long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
            startupGateState.markPassed(durationMs);
            log.info("event_type=startup_gate status=passed duration_ms={}", durationMs);
        } catch (Exception ex) {
            long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
            String reason = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            startupGateState.markFailed(reason, durationMs);
            log.error("event_type=startup_gate status=failed duration_ms={} reason=\"{}\"", durationMs, reason, ex);
            throw new IllegalStateException("Startup readiness gate failed", ex);
        }
    }

    private void assertDataSourceIsReady(String gateName, DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(dbValidationQuery)) {
            statement.execute();
            log.info("event_type=startup_gate_check gate={} status=ok", gateName);
        }
    }
}
