package io.github.wesleyosantos91.infrastructure.logging;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/**
 * Opcional.
 * Mantido como no-op para futuras customizações de structured logging JSON.
 * Se quiser usar, referencie via logging.structured.json.customizer no application.yml
 * ou registre em META-INF/spring.factories.
 */
public class JsonLogMembersCustomizer implements StructuredLoggingJsonMembersCustomizer<Object> {

    @Override
    public void customize(JsonWriter.Members<Object> members) {
        // no-op por enquanto
        // Evoluções futuras:
        // - rename dinâmico de chaves
        // - masking de campos
        // - remoção de membros em ambiente prod/dev
        // Exemplo de customização: adicionar um campo "application" com o nome da aplicação
        // members.add("application", "my-api-person-spring");
    }
}