package io.github.wesleyosantos91.infrastructure.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI personApiOpenAPI(
            @Value("${spring.application.name}") String applicationName,
            @Value("${APP_VERSION:0.0.1-SNAPSHOT}") String applicationVersion,
            @Value("${APP_ENV:dev}") String environment
    ) {
        return new OpenAPI().info(new Info()
                .title("API Person Spring")
                .description("API REST para gerenciamento de pessoas. Ambiente: " + environment + ". Serviço: " + applicationName + ".")
                .version(applicationVersion)
                .contact(new Contact()
                        .name("API Person Team"))
                .license(new License()
                        .name("Proprietary")));
    }
}
