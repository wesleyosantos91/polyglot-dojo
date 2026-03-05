package io.github.wesleyosantos91.infrastructure.datastore.config;

import com.zaxxer.hikari.HikariDataSource;
import io.github.wesleyosantos91.infrastructure.datastore.routing.DataSourceType;
import io.github.wesleyosantos91.infrastructure.datastore.routing.ReadWriteRoutingDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("app.datasource.writer")
    public DataSourceProperties writerDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "writerDataSource")
    @ConfigurationProperties("app.datasource.writer.hikari")
    public HikariDataSource writerDataSource(
            @Qualifier("writerDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @ConfigurationProperties("app.datasource.reader")
    public DataSourceProperties readerDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "readerDataSource")
    @ConfigurationProperties("app.datasource.reader.hikari")
    public HikariDataSource readerDataSource(
            @Qualifier("readerDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "routingDataSource")
    public DataSource routingDataSource(
            @Qualifier("writerDataSource") DataSource writer,
            @Qualifier("readerDataSource") DataSource reader) {

        ReadWriteRoutingDataSource routing = new ReadWriteRoutingDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.WRITER, writer);
        targetDataSources.put(DataSourceType.READER, reader);

        routing.setTargetDataSources(targetDataSources);
        routing.setDefaultTargetDataSource(writer); // fail-safe: write por padrão
        routing.afterPropertiesSet();

        return routing;
    }

    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("routingDataSource") DataSource routingDataSource) {
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }
}