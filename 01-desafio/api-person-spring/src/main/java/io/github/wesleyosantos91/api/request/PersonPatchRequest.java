package io.github.wesleyosantos91.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

public record PersonPatchRequest(
        @Size(min = 1, max = 150, message = "name must have between 1 and 150 characters")
        String name,

        @Email(message = "email must be valid")
        @Size(max = 255, message = "email must have at most 255 characters")
        String email,

        @Past(message = "birthDate must be in the past")
        LocalDate birthDate
) implements Serializable {
}