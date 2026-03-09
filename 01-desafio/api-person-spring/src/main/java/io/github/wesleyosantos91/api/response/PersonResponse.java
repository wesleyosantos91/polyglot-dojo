package io.github.wesleyosantos91.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PersonResponse(
        @Schema(description = "ID da pessoa", example = "018f1f35-9056-7f1b-89f5-8a6f41c0ab00")
        UUID id,
        @Schema(description = "Nome completo da pessoa", example = "Wesley Santos")
        String name,
        @Schema(description = "E-mail unico da pessoa", example = "wesley@example.com")
        String email,
        @Schema(description = "Data de nascimento (YYYY-MM-DD)", example = "1991-01-15")
        LocalDate birthDate,
        @Schema(description = "Data/hora de criacao do registro", example = "2026-03-08T11:55:12.320571-03:00")
        OffsetDateTime createdAt,
        @Schema(description = "Data/hora da ultima atualizacao", example = "2026-03-08T12:10:42.121993-03:00")
        OffsetDateTime updatedAt
) implements Serializable {
}
