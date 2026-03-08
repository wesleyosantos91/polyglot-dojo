package io.github.wesleyosantos91.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.wesleyosantos91.AbstractIT;
import io.github.wesleyosantos91.domain.entity.PersonEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PersonController - Integration Tests")
class PersonControllerIT extends AbstractIT {

    private PersonEntity savePerson(String name, String email, LocalDate birthDate) {
        var entity = new PersonEntity();
        entity.setId(UuidCreator.getTimeOrderedEpoch());
        entity.setName(name);
        entity.setEmail(email.toLowerCase());
        entity.setBirthDate(birthDate);
        entity.setCreatedAt(OffsetDateTime.now());
        return personRepository.save(entity);
    }

    private PersonEntity defaultPerson() {
        return savePerson("Wesley Santos", "wesley@example.com", LocalDate.of(1991, 1, 15));
    }

    @Nested
    @DisplayName("GET /api/persons")
    class FindAll {

        @Test
        void shouldReturnEmptyList() {
            assertThat(mvc.get().uri("/api/persons"))
                    .hasStatusOk()
                    .matches(jsonPath("$.page.totalElements").value(0));
        }

        @Test
        void shouldReturnPagedPersons() {
            savePerson("Ana Lima", "ana@example.com", LocalDate.of(1990, 5, 10));
            savePerson("Bruno Costa", "bruno@example.com", LocalDate.of(1988, 3, 22));

            assertThat(mvc.get().uri("/api/persons"))
                    .hasStatusOk()
                    .matches(jsonPath("$.page.totalElements").value(2));
        }

        @Test
        void shouldFilterByName() {
            savePerson("Wesley Santos", "wesley@example.com", LocalDate.of(1991, 1, 15));
            savePerson("Carlos Maia", "carlos@example.com", LocalDate.of(1985, 7, 4));

            assertThat(mvc.get().uri("/api/persons").param("name", "wes"))
                    .hasStatusOk()
                    .matches(jsonPath("$.content[0].name").value("Wesley Santos"));
        }

        @Test
        void shouldFilterByEmail() {
            savePerson("Wesley Santos", "wesley@example.com", LocalDate.of(1991, 1, 15));
            savePerson("Carlos Maia", "carlos@example.com", LocalDate.of(1985, 7, 4));

            assertThat(mvc.get().uri("/api/persons").param("email", "CARLOS@EXAMPLE.COM"))
                    .hasStatusOk()
                    .matches(jsonPath("$.content[0].email").value("carlos@example.com"));
        }

        @Test
        void shouldRespectPagination() {
            savePerson("Pessoa A", "a@example.com", LocalDate.of(1990, 1, 1));
            savePerson("Pessoa B", "b@example.com", LocalDate.of(1991, 2, 2));
            savePerson("Pessoa C", "c@example.com", LocalDate.of(1992, 3, 3));

            assertThat(mvc.get().uri("/api/persons").param("size", "2").param("page", "0"))
                    .hasStatusOk()
                    .matches(jsonPath("$.page.totalElements").value(3))
                    .matches(jsonPath("$.page.totalPages").value(2));
        }
    }

    @Nested
    @DisplayName("GET /api/persons/{id}")
    class FindById {

        @Test
        void shouldReturnPersonWhenFound() {
            PersonEntity saved = defaultPerson();

            assertThat(mvc.get().uri("/api/persons/{id}", saved.getId()))
                    .hasStatusOk()
                    .matches(jsonPath("$.id").value(saved.getId().toString()))
                    .matches(jsonPath("$.name").value("Wesley Santos"))
                    .matches(jsonPath("$.email").value("wesley@example.com"))
                    .matches(jsonPath("$.birthDate").value("1991-01-15"))
                    .matches(jsonPath("$.createdAt").isNotEmpty())
                    .matches(jsonPath("$.updatedAt").value(nullValue()));
        }

        @Test
        void shouldReturn404WhenNotFound() {
            UUID unknownId = UUID.randomUUID();

            assertThat(mvc.get().uri("/api/persons/{id}", unknownId))
                    .hasStatus(404)
                    .matches(jsonPath("$.status").value(404))
                    .matches(jsonPath("$.error_code").value("RESOURCE_NOT_FOUND"))
                    .matches(jsonPath("$.detail").value(containsString(unknownId.toString())));
        }

        @Test
        void shouldReturn400WhenIdIsInvalid() {
            assertThat(mvc.get().uri("/api/persons/nao-e-um-uuid"))
                    .hasStatus(400)
                    .matches(jsonPath("$.error_code").value("ARGUMENT_TYPE_MISMATCH"));
        }
    }

    @Nested
    @DisplayName("POST /api/persons")
    class Create {

        @Test
        void shouldCreatePersonAndReturn201() {
            var result = assertThat(mvc.post().uri("/api/persons")
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "name": "Wesley Santos",
                                "email": "wesley@example.com",
                                "birthDate": "1991-01-15"
                            }
                            """))
                    .hasStatus(201);

            result.headers().containsHeader("Location");
            result.matches(jsonPath("$.id").isNotEmpty())
                    .matches(jsonPath("$.name").value("Wesley Santos"))
                    .matches(jsonPath("$.email").value("wesley@example.com"))
                    .matches(jsonPath("$.birthDate").value("1991-01-15"))
                    .matches(jsonPath("$.createdAt").isNotEmpty())
                    .matches(jsonPath("$.updatedAt").value(nullValue()));
        }

        @Test
        void shouldNormalizeEmailToLowercase() {
            assertThat(mvc.post().uri("/api/persons")
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "name": "Wesley Santos",
                                "email": "WESLEY@EXAMPLE.COM",
                                "birthDate": "1991-01-15"
                            }
                            """))
                    .hasStatus(201)
                    .matches(jsonPath("$.email").value("wesley@example.com"));
        }

        @Test
        void shouldReturn422WhenEmailAlreadyExists() {
            defaultPerson();

            assertThat(mvc.post().uri("/api/persons")
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "name": "Outro Nome",
                                "email": "wesley@example.com",
                                "birthDate": "1990-06-20"
                            }
                            """))
                    .hasStatus(422)
                    .matches(jsonPath("$.error_code").value("PERSON_EMAIL_ALREADY_EXISTS"));
        }

        @Test
        void shouldReturn400WithViolationWhenNameIsBlank() {
            assertThat(mvc.post().uri("/api/persons")
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "name": "",
                                "email": "valid@example.com",
                                "birthDate": "1990-01-01"
                            }
                            """))
                    .hasStatus(400)
                    .matches(jsonPath("$.error_code").value("VALIDATION_ERROR"))
                    .matches(jsonPath("$.violations[0].field").value("name"));
        }

        @Test
        void shouldReturn400WhenEmailIsInvalid() {
            assertThat(mvc.post().uri("/api/persons")
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "name": "Wesley Santos",
                                "email": "nao-e-email",
                                "birthDate": "1991-01-15"
                            }
                            """))
                    .hasStatus(400)
                    .matches(jsonPath("$.error_code").value("VALIDATION_ERROR"))
                    .matches(jsonPath("$.violations[0].field").value("email"));
        }

        @Test
        void shouldReturn400WhenBirthDateIsInFuture() {
            assertThat(mvc.post().uri("/api/persons")
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "name": "Wesley Santos",
                                "email": "wesley@example.com",
                                "birthDate": "2099-12-31"
                            }
                            """))
                    .hasStatus(400)
                    .matches(jsonPath("$.error_code").value("VALIDATION_ERROR"))
                    .matches(jsonPath("$.violations[0].field").value("birthDate"));
        }

        @Test
        void shouldReturn400WhenBodyIsMissing() {
            assertThat(mvc.post().uri("/api/persons").contentType(APPLICATION_JSON))
                    .hasStatus(400)
                    .matches(jsonPath("$.error_code").value("MALFORMED_JSON"));
        }
    }

    @Nested
    @DisplayName("PUT /api/persons/{id}")
    class Update {

        @Test
        void shouldUpdatePersonAndReturn200() {
            PersonEntity saved = defaultPerson();

            assertThat(mvc.put().uri("/api/persons/{id}", saved.getId())
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "name": "Wesley Atualizado",
                                "email": "novo@example.com",
                                "birthDate": "1992-06-20"
                            }
                            """))
                    .hasStatusOk()
                    .matches(jsonPath("$.id").value(saved.getId().toString()))
                    .matches(jsonPath("$.name").value("Wesley Atualizado"))
                    .matches(jsonPath("$.email").value("novo@example.com"))
                    .matches(jsonPath("$.birthDate").value("1992-06-20"))
                    .matches(jsonPath("$.updatedAt").isNotEmpty());
        }

        @Test
        void shouldReturn404WhenNotFound() {
            assertThat(mvc.put().uri("/api/persons/{id}", UUID.randomUUID())
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "name": "Qualquer",
                                "email": "qualquer@example.com",
                                "birthDate": "1990-01-01"
                            }
                            """))
                    .hasStatus(404)
                    .matches(jsonPath("$.error_code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        void shouldReturn422WhenEmailBelongsToAnotherPerson() {
            savePerson("Ana Lima", "ana@example.com", LocalDate.of(1990, 5, 10));
            PersonEntity wesley = defaultPerson();

            assertThat(mvc.put().uri("/api/persons/{id}", wesley.getId())
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "name": "Wesley Santos",
                                "email": "ana@example.com",
                                "birthDate": "1991-01-15"
                            }
                            """))
                    .hasStatus(422)
                    .matches(jsonPath("$.error_code").value("PERSON_EMAIL_ALREADY_EXISTS"));
        }

        @Test
        void shouldAllowUpdateWithSameEmail() {
            PersonEntity saved = defaultPerson();

            assertThat(mvc.put().uri("/api/persons/{id}", saved.getId())
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "name": "Wesley Atualizado",
                                "email": "wesley@example.com",
                                "birthDate": "1991-01-15"
                            }
                            """))
                    .hasStatusOk()
                    .matches(jsonPath("$.name").value("Wesley Atualizado"))
                    .matches(jsonPath("$.email").value("wesley@example.com"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/persons/{id}")
    class PartialUpdate {

        @Test
        void shouldPatchNameOnly() {
            PersonEntity saved = defaultPerson();

            assertThat(mvc.patch().uri("/api/persons/{id}", saved.getId())
                    .contentType(APPLICATION_JSON)
                    .content("{\"name\":\"Wesley Patched\"}"))
                    .hasStatusOk()
                    .matches(jsonPath("$.name").value("Wesley Patched"))
                    .matches(jsonPath("$.email").value("wesley@example.com"))
                    .matches(jsonPath("$.birthDate").value("1991-01-15"))
                    .matches(jsonPath("$.updatedAt").isNotEmpty());
        }

        @Test
        void shouldPatchEmailOnly() {
            PersonEntity saved = defaultPerson();

            assertThat(mvc.patch().uri("/api/persons/{id}", saved.getId())
                    .contentType(APPLICATION_JSON)
                    .content("{\"email\":\"patched@example.com\"}"))
                    .hasStatusOk()
                    .matches(jsonPath("$.name").value("Wesley Santos"))
                    .matches(jsonPath("$.email").value("patched@example.com"));
        }

        @Test
        void shouldPatchBirthDateOnly() {
            PersonEntity saved = defaultPerson();

            assertThat(mvc.patch().uri("/api/persons/{id}", saved.getId())
                    .contentType(APPLICATION_JSON)
                    .content("{\"birthDate\":\"1995-08-30\"}"))
                    .hasStatusOk()
                    .matches(jsonPath("$.name").value("Wesley Santos"))
                    .matches(jsonPath("$.birthDate").value("1995-08-30"));
        }

        @Test
        void shouldReturn422WhenEmailConflictOnPatch() {
            savePerson("Ana Lima", "ana@example.com", LocalDate.of(1990, 5, 10));
            PersonEntity wesley = defaultPerson();

            assertThat(mvc.patch().uri("/api/persons/{id}", wesley.getId())
                    .contentType(APPLICATION_JSON)
                    .content("{\"email\":\"ana@example.com\"}"))
                    .hasStatus(422)
                    .matches(jsonPath("$.error_code").value("PERSON_EMAIL_ALREADY_EXISTS"));
        }

        @Test
        void shouldReturn400WhenPatchEmailIsInvalid() {
            PersonEntity saved = defaultPerson();

            assertThat(mvc.patch().uri("/api/persons/{id}", saved.getId())
                    .contentType(APPLICATION_JSON)
                    .content("{\"email\":\"invalido\"}"))
                    .hasStatus(400)
                    .matches(jsonPath("$.error_code").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/persons/{id}")
    class Delete {

        @Test
        void shouldDeletePersonAndReturn204() {
            PersonEntity saved = defaultPerson();

            assertThat(mvc.delete().uri("/api/persons/{id}", saved.getId()))
                    .hasStatus(204);

            assertThat(mvc.get().uri("/api/persons/{id}", saved.getId()))
                    .hasStatus(404);
        }

        @Test
        void shouldReturn404WhenDeletingNonExistentPerson() {
            assertThat(mvc.delete().uri("/api/persons/{id}", UUID.randomUUID()))
                    .hasStatus(404)
                    .matches(jsonPath("$.error_code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        void shouldReturn404OnDoubleDeletion() {
            PersonEntity saved = defaultPerson();
            UUID id = saved.getId();

            assertThat(mvc.delete().uri("/api/persons/{id}", id)).hasStatus(204);
            assertThat(mvc.delete().uri("/api/persons/{id}", id)).hasStatus(404);
        }
    }
}
