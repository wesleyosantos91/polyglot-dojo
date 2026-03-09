package io.github.wesleyosantos91.infrastructure.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("StartupReadinessGateRunner - Unit Tests")
class StartupReadinessGateRunnerTest {

    @Test
    @DisplayName("marks gate as passed when writer and reader data sources are healthy")
    void marksPassedWhenDataSourcesAreHealthy() throws Exception {
        DataSource writer = mock(DataSource.class);
        DataSource reader = mock(DataSource.class);
        Connection writerConnection = mock(Connection.class);
        Connection readerConnection = mock(Connection.class);
        PreparedStatement writerStatement = mock(PreparedStatement.class);
        PreparedStatement readerStatement = mock(PreparedStatement.class);

        when(writer.getConnection()).thenReturn(writerConnection);
        when(reader.getConnection()).thenReturn(readerConnection);
        when(writerConnection.prepareStatement("SELECT 1")).thenReturn(writerStatement);
        when(readerConnection.prepareStatement("SELECT 1")).thenReturn(readerStatement);

        StartupGateState state = new StartupGateState();
        StartupReadinessGateRunner runner = new StartupReadinessGateRunner(
                providerOf(writer, DataSource.class),
                providerOf(reader, DataSource.class),
                state,
                noOpPublisher()
        );
        ReflectionTestUtils.setField(runner, "dbValidationQuery", "SELECT 1");

        runner.run(mock(ApplicationArguments.class));

        assertThat(state.isCompleted()).isTrue();
        assertThat(state.isPassed()).isTrue();
        assertThat(state.getDurationMs()).isGreaterThanOrEqualTo(0L);
        verify(writerStatement).execute();
        verify(readerStatement).execute();
    }

    @Test
    @DisplayName("marks gate as passed when writer or reader datasource bean is missing")
    void marksPassedWhenDatasourceBeanIsMissing() throws Exception {
        StartupGateState state = new StartupGateState();
        StartupReadinessGateRunner runner = new StartupReadinessGateRunner(
                emptyProvider(DataSource.class),
                emptyProvider(DataSource.class),
                state,
                noOpPublisher()
        );
        ReflectionTestUtils.setField(runner, "dbValidationQuery", "SELECT 1");

        runner.run(mock(ApplicationArguments.class));

        assertThat(state.isCompleted()).isTrue();
        assertThat(state.isPassed()).isTrue();
        assertThat(state.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("marks gate as failed and throws when datasource health check fails")
    void marksFailedWhenDatasourceCheckFails() throws Exception {
        DataSource brokenWriter = mock(DataSource.class);
        DataSource reader = mock(DataSource.class);
        when(brokenWriter.getConnection()).thenThrow(new SQLException("writer unavailable"));
        when(reader.getConnection()).thenReturn(mock(Connection.class));

        StartupGateState state = new StartupGateState();
        StartupReadinessGateRunner runner = new StartupReadinessGateRunner(
                providerOf(brokenWriter, DataSource.class),
                providerOf(reader, DataSource.class),
                state,
                noOpPublisher()
        );
        ReflectionTestUtils.setField(runner, "dbValidationQuery", "SELECT 1");

        assertThatThrownBy(() -> runner.run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Startup readiness gate failed");

        assertThat(state.isCompleted()).isTrue();
        assertThat(state.isPassed()).isFalse();
        assertThat(state.getFailureReason()).contains("writer unavailable");
    }

    private static <T> ObjectProvider<T> emptyProvider(Class<T> type) {
        return new StaticListableBeanFactory().getBeanProvider(type);
    }

    private static <T> ObjectProvider<T> providerOf(T bean, Class<T> type) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("bean", bean);
        return beanFactory.getBeanProvider(type);
    }

    private static ApplicationEventPublisher noOpPublisher() {
        return event -> {
        };
    }
}
