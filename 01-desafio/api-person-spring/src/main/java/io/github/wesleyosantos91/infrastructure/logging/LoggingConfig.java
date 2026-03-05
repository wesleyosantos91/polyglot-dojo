package io.github.wesleyosantos91.infrastructure.logging;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingConfig {
}
