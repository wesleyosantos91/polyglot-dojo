package io.github.wesleyosantos91.infrastructure.resilience;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

@Configuration
@EnableResilientMethods
@EnableConfigurationProperties(DbRetryProperties.class)
public class ResilienceMethodsConfig {
}
