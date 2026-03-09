package io.github.wesleyosantos91.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

public record PersonPatchRequest(
        @Schema(description = "Nome completo da pessoa", example = "Wesley Santos")
        @Size(min = 1, max = 150, message = "name must have between 1 and 150 characters")
        String name,

        @Schema(description = "E-mail unico da pessoa", example = "wesley@example.com")
        @Email(message = "email must be valid")
        @Size(max = 255, message = "email must have at most 255 characters")
        String email,

        @Schema(description = "Data de nascimento (YYYY-MM-DD)", example = "1991-01-15")
        @Past(message = "birthDate must be in the past")
        LocalDate birthDate
) implements Serializable {
}
