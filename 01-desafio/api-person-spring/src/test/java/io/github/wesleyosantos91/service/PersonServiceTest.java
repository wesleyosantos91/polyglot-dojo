package io.github.wesleyosantos91.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.wesleyosantos91.api.request.PersonPatchRequest;
import io.github.wesleyosantos91.api.request.PersonRequest;
import io.github.wesleyosantos91.core.mapper.PersonMapper;
import io.github.wesleyosantos91.domain.entity.PersonEntity;
import io.github.wesleyosantos91.domain.exception.ConflictException;
import io.github.wesleyosantos91.domain.exception.ResourceNotFoundException;
import io.github.wesleyosantos91.domain.repository.PersonRepository;
import io.github.wesleyosantos91.domain.service.PersonService;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonService — Unit Tests")
class PersonServiceTest {

    @Mock
    PersonRepository personRepository;

    @Mock
    PersonMapper personMapper;

    @InjectMocks
    PersonService personService;

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private PersonEntity personWith(UUID id, String name, String email) {
        PersonEntity e = new PersonEntity();
        e.setId(id);
        e.setName(name);
        e.setEmail(email);
        e.setBirthDate(LocalDate.of(1991, 1, 15));
        return e;
    }

    // ─── findAllPaged ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAllPaged")
    class FindAllPaged {

        @Test
        @DisplayName("should return paged results when no filter is provided")
        void shouldReturnPagedResults_whenNoFilter() {
            // Arrange
            var pageable = PageRequest.of(0, 10);
            var entity = personWith(UUID.randomUUID(), "Wesley Santos", "wesley@example.com");
            Page<PersonEntity> page = new PageImpl<>(java.util.List.of(entity));
            when(personRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            // Act
            var result = personService.findAllPaged(null, null, pageable);

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(personRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("should apply name filter when name is provided")
        void shouldApplyNameFilter_whenNameIsProvided() {
            // Arrange
            var pageable = PageRequest.of(0, 10);
            when(personRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(Page.empty());

            // Act
            personService.findAllPaged("wesley", null, pageable);

            // Assert — verifica que o repositório foi chamado (spec montada internamente)
            verify(personRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("should normalize email filter to lowercase")
        void shouldNormalizeEmailFilter_whenEmailIsProvided() {
            // Arrange
            var pageable = PageRequest.of(0, 10);
            when(personRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(Page.empty());

            // Act
            personService.findAllPaged(null, "WESLEY@EXAMPLE.COM", pageable);

            // Assert
            verify(personRepository).findAll(any(Specification.class), eq(pageable));
        }
    }

    // ─── findByIdOrThrow ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByIdOrThrow")
    class FindByIdOrThrow {

        @Test
        @DisplayName("should return entity when found")
        void shouldReturnEntity_whenFound() {
            // Arrange
            var id = UUID.randomUUID();
            var entity = personWith(id, "Wesley", "w@example.com");
            when(personRepository.findById(id)).thenReturn(Optional.of(entity));

            // Act
            var result = personService.findByIdOrThrow(id);

            // Assert
            assertThat(result.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrow_whenNotFound() {
            // Arrange
            var id = UUID.randomUUID();
            when(personRepository.findById(id)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> personService.findByIdOrThrow(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should persist and return entity when email is unique")
        void shouldCreatePerson_whenEmailIsUnique() {
            // Arrange
            var entity = personWith(null, "Wesley Santos", "WESLEY@EXAMPLE.COM");
            when(personRepository.existsByEmail("wesley@example.com")).thenReturn(false);
            when(personRepository.save(any(PersonEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            var result = personService.create(entity);

            // Assert
            assertThat(result.getEmail()).isEqualTo("wesley@example.com");
            assertThat(result.getName()).isEqualTo("Wesley Santos");
            assertThat(result.getId()).isNotNull();
            verify(personRepository).save(entity);
        }

        @Test
        @DisplayName("should normalize email to lowercase before persist")
        void shouldNormalizeEmail_beforePersist() {
            // Arrange
            var entity = personWith(null, "  Wesley  ", "UPPER@CASE.COM");
            when(personRepository.existsByEmail("upper@case.com")).thenReturn(false);
            when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            var result = personService.create(entity);

            // Assert
            assertThat(result.getEmail()).isEqualTo("upper@case.com");
            assertThat(result.getName()).isEqualTo("Wesley");
        }

        @Test
        @DisplayName("should throw ConflictException when email already exists")
        void shouldThrowConflict_whenEmailAlreadyExists() {
            // Arrange
            var entity = personWith(null, "Wesley", "dup@example.com");
            when(personRepository.existsByEmail("dup@example.com")).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> personService.create(entity))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("dup@example.com");

            verify(personRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when payload is null")
        void shouldThrow_whenPayloadIsNull() {
            assertThatThrownBy(() -> personService.create(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when name is null")
        void shouldThrow_whenNameIsNull() {
            var entity = personWith(null, null, "valid@example.com");
            when(personRepository.existsByEmail("valid@example.com")).thenReturn(false);

            assertThatThrownBy(() -> personService.create(entity))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");

            verify(personRepository, never()).save(any());
        }

        @Test
        @DisplayName("should preserve existing id when entity already has one set")
        void shouldPreserveExistingId_whenIdAlreadySet() {
            var existingId = UUID.randomUUID();
            var entity = personWith(existingId, "Wesley", "w@example.com");
            when(personRepository.existsByEmail("w@example.com")).thenReturn(false);
            when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = personService.create(entity);

            assertThat(result.getId()).isEqualTo(existingId);
        }

        @Test
        @DisplayName("should use WRITER datasource — @Transactional without readOnly")
        void shouldUseWriterDataSource_forCreate() throws NoSuchMethodException {
            // Verifica que o método create NÃO tem readOnly=true
            var method = PersonService.class.getMethod("create", PersonEntity.class);
            var tx = method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);

            assertThat(tx).isNotNull();
            assertThat(tx.readOnly()).isFalse();
        }
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update all fields and return updated entity")
        void shouldUpdateAllFields_whenPersonExists() {
            // Arrange
            var id = UUID.randomUUID();
            var current = personWith(id, "Old Name", "old@example.com");
            var payload = personWith(null, "New Name", "new@example.com");

            when(personRepository.findById(id)).thenReturn(Optional.of(current));
            when(personRepository.existsByEmailAndIdNot("new@example.com", id)).thenReturn(false);
            when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            var result = personService.update(id, payload);

            // Assert
            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(result.getEmail()).isEqualTo("new@example.com");
        }

        @Test
        @DisplayName("should allow update with same email as owner")
        void shouldAllowUpdate_whenEmailBelongsToSamePerson() {
            // Arrange
            var id = UUID.randomUUID();
            var current = personWith(id, "Wesley", "same@example.com");
            var payload = personWith(null, "Wesley Updated", "same@example.com");

            when(personRepository.findById(id)).thenReturn(Optional.of(current));
            when(personRepository.existsByEmailAndIdNot("same@example.com", id)).thenReturn(false);
            when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            var result = personService.update(id, payload);

            // Assert
            assertThat(result.getName()).isEqualTo("Wesley Updated");
            assertThat(result.getEmail()).isEqualTo("same@example.com");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when person does not exist")
        void shouldThrow_whenPersonNotFound() {
            // Arrange
            var id = UUID.randomUUID();
            when(personRepository.findById(id)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> personService.update(id, personWith(null, "X", "x@x.com")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ConflictException when new email belongs to another person")
        void shouldThrowConflict_whenEmailBelongsToAnotherPerson() {
            // Arrange
            var id = UUID.randomUUID();
            var current = personWith(id, "Wesley", "mine@example.com");
            var payload = personWith(null, "Wesley", "taken@example.com");

            when(personRepository.findById(id)).thenReturn(Optional.of(current));
            when(personRepository.existsByEmailAndIdNot("taken@example.com", id)).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> personService.update(id, payload))
                    .isInstanceOf(ConflictException.class);

            verify(personRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when payload is null")
        void shouldThrow_whenUpdatePayloadIsNull() {
            assertThatThrownBy(() -> personService.update(UUID.randomUUID(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should use WRITER datasource — @Transactional without readOnly")
        void shouldUseWriterDataSource_forUpdate() throws NoSuchMethodException {
            var method = PersonService.class.getMethod("update", UUID.class, PersonEntity.class);
            var tx = method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);

            assertThat(tx).isNotNull();
            assertThat(tx.readOnly()).isFalse();
        }
    }

    // ─── patch ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("patch")
    class Patch {

        @Test
        @DisplayName("should patch only provided fields and leave others unchanged")
        void shouldPatchOnlyProvidedFields() {
            // Arrange
            var id = UUID.randomUUID();
            var current = personWith(id, "Wesley", "w@example.com");
            current.setBirthDate(LocalDate.of(1991, 1, 15));
            var patchRequest = new PersonPatchRequest("Wesley Patched", null, null);

            when(personRepository.findById(id)).thenReturn(Optional.of(current));
            // mapper.patchEntity aplica apenas os campos não-nulos
            org.mockito.Mockito.doAnswer(inv -> {
                PersonEntity target = inv.getArgument(1);
                target.setName("Wesley Patched"); // simula NullValuePropertyMappingStrategy.IGNORE
                return null;
            }).when(personMapper).patchEntity(patchRequest, current);
            when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            var result = personService.patch(id, patchRequest);

            // Assert
            assertThat(result.getName()).isEqualTo("Wesley Patched");
            assertThat(result.getEmail()).isEqualTo("w@example.com"); // inalterado
        }

        @Test
        @DisplayName("should throw ConflictException when patched email belongs to another person")
        void shouldThrowConflict_whenPatchedEmailIsTaken() {
            // Arrange
            var id = UUID.randomUUID();
            var current = personWith(id, "Wesley", "mine@example.com");
            var patchRequest = new PersonPatchRequest(null, "taken@example.com", null);

            when(personRepository.findById(id)).thenReturn(Optional.of(current));
            when(personRepository.existsByEmailAndIdNot("taken@example.com", id)).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> personService.patch(id, patchRequest))
                    .isInstanceOf(ConflictException.class);

            verify(personRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when person does not exist")
        void shouldThrow_whenPersonNotFoundOnPatch() {
            // Arrange
            var id = UUID.randomUUID();
            when(personRepository.findById(id)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> personService.patch(id, new PersonPatchRequest("X", null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when patch payload is null")
        void shouldThrow_whenPatchPayloadIsNull() {
            assertThatThrownBy(() -> personService.patch(UUID.randomUUID(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should skip email conflict check when email is blank")
        void shouldSkipEmailCheck_whenEmailIsBlank() {
            var id = UUID.randomUUID();
            var current = personWith(id, "Wesley", "w@example.com");
            var patchRequest = new PersonPatchRequest(null, "   ", null);

            when(personRepository.findById(id)).thenReturn(Optional.of(current));
            org.mockito.Mockito.doAnswer(inv -> null).when(personMapper).patchEntity(patchRequest, current);
            when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            personService.patch(id, patchRequest);

            verify(personRepository, never()).existsByEmailAndIdNot(any(), any());
        }

        @Test
        @DisplayName("should not normalize name when name becomes null after mapping")
        void shouldNotNormalizeName_whenNameIsNullAfterMapping() {
            var id = UUID.randomUUID();
            var current = personWith(id, "Wesley", "w@example.com");
            var patchRequest = new PersonPatchRequest(null, null, null);

            when(personRepository.findById(id)).thenReturn(Optional.of(current));
            org.mockito.Mockito.doAnswer(inv -> {
                PersonEntity target = inv.getArgument(1);
                target.setName(null);
                return null;
            }).when(personMapper).patchEntity(patchRequest, current);
            when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = personService.patch(id, patchRequest);

            assertThat(result.getName()).isNull();
        }

        @Test
        @DisplayName("should not normalize email when email becomes null after mapping")
        void shouldNotNormalizeEmail_whenEmailIsNullAfterMapping() {
            var id = UUID.randomUUID();
            var current = personWith(id, "Wesley", "w@example.com");
            var patchRequest = new PersonPatchRequest(null, null, null);

            when(personRepository.findById(id)).thenReturn(Optional.of(current));
            org.mockito.Mockito.doAnswer(inv -> {
                PersonEntity target = inv.getArgument(1);
                target.setEmail(null);
                return null;
            }).when(personMapper).patchEntity(patchRequest, current);
            when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = personService.patch(id, patchRequest);

            assertThat(result.getEmail()).isNull();
        }
    }

    // ─── deleteById ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("should delete entity when person exists")
        void shouldDelete_whenPersonExists() {
            // Arrange
            var id = UUID.randomUUID();
            when(personRepository.existsById(id)).thenReturn(true);

            // Act
            personService.deleteById(id);

            // Assert
            verify(personRepository).deleteById(id);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when person does not exist")
        void shouldThrow_whenPersonNotFoundOnDelete() {
            // Arrange
            var id = UUID.randomUUID();
            when(personRepository.existsById(id)).thenReturn(false);

            // Act + Assert
            assertThatThrownBy(() -> personService.deleteById(id))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(personRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("should use WRITER datasource — @Transactional without readOnly")
        void shouldUseWriterDataSource_forDelete() throws NoSuchMethodException {
            var method = PersonService.class.getMethod("deleteById", UUID.class);
            var tx = method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);

            assertThat(tx).isNotNull();
            assertThat(tx.readOnly()).isFalse();
        }
    }

    // ─── findByEmail ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("should normalize email and delegate to repository")
        void shouldNormalizeAndDelegate_whenEmailFound() {
            var entity = personWith(UUID.randomUUID(), "Wesley", "w@example.com");
            when(personRepository.findByEmail("w@example.com")).thenReturn(Optional.of(entity));

            var result = personService.findByEmail("W@EXAMPLE.COM");

            assertThat(result).contains(entity);
        }

        @Test
        @DisplayName("should return empty when email not found")
        void shouldReturnEmpty_whenNotFound() {
            when(personRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            var result = personService.findByEmail("missing@example.com");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when email is null")
        void shouldThrow_whenEmailIsNull() {
            assertThatThrownBy(() -> personService.findByEmail(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("email");
        }
    }

    // ─── Routing — leituras usam readOnly=true ────────────────────────────────

    @Nested
    @DisplayName("Transactional routing — reader datasource")
    class TransactionalRouting {

        @Test
        @DisplayName("findAllPaged should use READER — @Transactional(readOnly = true)")
        void findAllPaged_shouldUseReaderDataSource() throws NoSuchMethodException {
            var method = PersonService.class.getMethod(
                    "findAllPaged", String.class, String.class,
                    org.springframework.data.domain.Pageable.class);
            var tx = method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);

            assertThat(tx).isNotNull();
            assertThat(tx.readOnly()).isTrue();
        }

        @Test
        @DisplayName("findByIdOrThrow should use READER — @Transactional(readOnly = true)")
        void findByIdOrThrow_shouldUseReaderDataSource() throws NoSuchMethodException {
            var method = PersonService.class.getMethod("findByIdOrThrow", UUID.class);
            var tx = method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);

            assertThat(tx).isNotNull();
            assertThat(tx.readOnly()).isTrue();
        }
    }
}