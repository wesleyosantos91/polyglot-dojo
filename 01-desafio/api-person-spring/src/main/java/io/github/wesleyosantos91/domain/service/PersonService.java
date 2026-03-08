package io.github.wesleyosantos91.domain.service;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.wesleyosantos91.api.request.PersonPatchRequest;
import io.github.wesleyosantos91.api.request.PersonRequest;
import io.github.wesleyosantos91.core.mapper.PersonMapper;
import io.github.wesleyosantos91.domain.entity.PersonEntity;
import io.github.wesleyosantos91.domain.exception.ConflictException;
import io.github.wesleyosantos91.domain.exception.ResourceNotFoundException;
import io.github.wesleyosantos91.domain.repository.PersonRepository;
import io.micrometer.core.annotation.Timed;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;

    public PersonService(PersonRepository personRepository, PersonMapper personMapper) {
        this.personRepository = personRepository;
        this.personMapper = personMapper;
    }

    @Timed(value = "person.service.operation", extraTags = {"operation", "find_all"},
            description = "Duration of PersonService operations")
    @Transactional(readOnly = true)
    @ConcurrencyLimit(
            limitString = "${app.db.concurrency.read-limit:3}",
            policy = ConcurrencyLimit.ThrottlePolicy.REJECT
    )
    public Page<PersonEntity> findAllPaged(String name, String email, Pageable pageable) {
        Specification<PersonEntity> spec = Specification.unrestricted();

        if (name != null && !name.isBlank()) {
            String normalizedName = "%" + name.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), normalizedName));
        }

        if (email != null && !email.isBlank()) {
            String normalizedEmail = normalizeEmail(email);
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("email")), normalizedEmail));
        }

        return personRepository.findAll(spec, pageable);
    }

    @Timed(value = "person.service.operation", extraTags = {"operation", "find_by_id"},
            description = "Duration of PersonService operations")
    @Transactional(readOnly = true)
    @ConcurrencyLimit(
            limitString = "${app.db.concurrency.read-limit:3}",
            policy = ConcurrencyLimit.ThrottlePolicy.REJECT
    )
    public Optional<PersonEntity> findById(UUID id) {
        return personRepository.findById(id);
    }

    @Timed(value = "person.service.operation", extraTags = {"operation", "find_by_id"},
            description = "Duration of PersonService operations")
    @Transactional(readOnly = true)
    @ConcurrencyLimit(
            limitString = "${app.db.concurrency.read-limit:3}",
            policy = ConcurrencyLimit.ThrottlePolicy.REJECT
    )
    public PersonEntity findByIdOrThrow(UUID id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person", id));
    }

    @Timed(value = "person.service.operation", extraTags = {"operation", "find_by_email"},
            description = "Duration of PersonService operations")
    @Transactional(readOnly = true)
    @ConcurrencyLimit(
            limitString = "${app.db.concurrency.read-limit:3}",
            policy = ConcurrencyLimit.ThrottlePolicy.REJECT
    )
    public Optional<PersonEntity> findByEmail(String email) {
        return personRepository.findByEmail(normalizeEmail(email));
    }

    @Timed(value = "person.service.operation", extraTags = {"operation", "create"},
            description = "Duration of PersonService operations")
    @Transactional
    @ConcurrencyLimit(
            limitString = "${app.db.concurrency.write-limit:2}",
            policy = ConcurrencyLimit.ThrottlePolicy.REJECT
    )
    public PersonEntity create(PersonEntity person) {
        if (person == null) {
            throw new IllegalArgumentException("Person payload must not be null");
        }

        String normalizedEmail = normalizeEmail(person.getEmail());

        if (personRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("PERSON_EMAIL_ALREADY_EXISTS", "Email already exists: " + normalizedEmail);
        }

        if (person.getId() == null) {
            person.setId(UuidCreator.getTimeOrderedEpoch());
        }

        person.setName(normalizeName(person.getName()));
        person.setEmail(normalizedEmail);

        return personRepository.save(person);
    }

    @Timed(value = "person.service.operation", extraTags = {"operation", "update"},
            description = "Duration of PersonService operations")
    @Transactional
    @ConcurrencyLimit(
            limitString = "${app.db.concurrency.write-limit:2}",
            policy = ConcurrencyLimit.ThrottlePolicy.REJECT
    )
    public PersonEntity update(UUID id, PersonEntity payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Person payload must not be null");
        }

        PersonEntity current = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person", id));

        String normalizedEmail = normalizeEmail(payload.getEmail());

        if (personRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new ConflictException("PERSON_EMAIL_ALREADY_EXISTS", "Email already exists: " + normalizedEmail);
        }

        current.setName(normalizeName(payload.getName()));
        current.setEmail(normalizedEmail);
        current.setBirthDate(payload.getBirthDate());

        return personRepository.save(current);
    }

    @Timed(value = "person.service.operation", extraTags = {"operation", "patch"},
            description = "Duration of PersonService operations")
    @Transactional
    @ConcurrencyLimit(
            limitString = "${app.db.concurrency.write-limit:2}",
            policy = ConcurrencyLimit.ThrottlePolicy.REJECT
    )
    public PersonEntity patch(UUID id, PersonPatchRequest patchRequest) {
        if (patchRequest == null) {
            throw new IllegalArgumentException("Patch payload must not be null");
        }

        PersonEntity current = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person", id));

        if (patchRequest.email() != null && !patchRequest.email().isBlank()) {
            String normalizedEmail = normalizeEmail(patchRequest.email());
            if (personRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
                throw new ConflictException("PERSON_EMAIL_ALREADY_EXISTS", "Email already exists: " + normalizedEmail);
            }
        }

        personMapper.patchEntity(patchRequest, current);

        if (current.getName() != null) {
            current.setName(normalizeName(current.getName()));
        }
        if (current.getEmail() != null) {
            current.setEmail(normalizeEmail(current.getEmail()));
        }

        return personRepository.save(current);
    }

    @Timed(value = "person.service.operation", extraTags = {"operation", "delete"},
            description = "Duration of PersonService operations")
    @Transactional
    @ConcurrencyLimit(
            limitString = "${app.db.concurrency.write-limit:2}",
            policy = ConcurrencyLimit.ThrottlePolicy.REJECT
    )
    public void deleteById(UUID id) {
        if (!personRepository.existsById(id)) {
            throw new ResourceNotFoundException("Person", id);
        }

        personRepository.deleteById(id);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("email must not be null");
        }
        return email.trim().toLowerCase();
    }

    private String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        return name.trim();
    }
}