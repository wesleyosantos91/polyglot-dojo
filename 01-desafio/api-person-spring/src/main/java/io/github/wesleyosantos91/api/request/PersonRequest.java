package io.github.wesleyosantos91.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

public record PersonRequest(
        @Schema(description = "Nome completo da pessoa", example = "Wesley Santos")
        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must have at most 150 characters")
        String name,

        @Schema(description = "E-mail unico da pessoa", example = "wesley@example.com")
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 255, message = "email must have at most 255 characters")
        String email,

        @Schema(description = "Data de nascimento (YYYY-MM-DD)", example = "1991-01-15")
        @NotNull(message = "birthDate is required")
        @Past(message = "birthDate must be in the past")
        LocalDate birthDate
) implements Serializable {
}
