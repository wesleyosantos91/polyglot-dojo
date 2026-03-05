package io.github.wesleyosantos91.api.response;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PersonResponse(
        UUID id,
        String name,
        String email,
        LocalDate birthDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) implements Serializable {
}