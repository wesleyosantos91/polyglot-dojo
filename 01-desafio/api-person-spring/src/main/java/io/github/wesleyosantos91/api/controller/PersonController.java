package io.github.wesleyosantos91.api.controller;

import io.github.wesleyosantos91.api.request.PersonPatchRequest;
import io.github.wesleyosantos91.api.request.PersonRequest;
import io.github.wesleyosantos91.api.response.PersonResponse;
import io.github.wesleyosantos91.core.mapper.PersonMapper;
import io.github.wesleyosantos91.domain.entity.PersonEntity;
import io.github.wesleyosantos91.domain.service.PersonService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/persons")
public class PersonController {

    private final PersonService personService;
    private final PersonMapper personMapper;

    public PersonController(PersonService personService, PersonMapper personMapper) {
        this.personService = personService;
        this.personMapper = personMapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonResponse> findById(@PathVariable UUID id) {
        PersonEntity person = personService.findByIdOrThrow(id);
        return ResponseEntity.ok(personMapper.toResponse(person));
    }

    @GetMapping
    public ResponseEntity<Page<PersonResponse>> findAllPaged(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PersonResponse> responsePage = personService.findAllPaged(name, email, pageable)
                .map(personMapper::toResponse);

        return ResponseEntity.ok(responsePage);
    }

    @PostMapping
    public ResponseEntity<PersonResponse> create(
            @Valid @RequestBody PersonRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        PersonEntity entity = personMapper.toEntity(request);
        PersonEntity created = personService.create(entity);

        URI location = uriBuilder
                .path("/api/persons/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(personMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PersonRequest request
    ) {
        PersonEntity payload = personMapper.toEntity(request);
        PersonEntity updated = personService.update(id, payload);
        return ResponseEntity.ok(personMapper.toResponse(updated));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PersonResponse> patch(
            @PathVariable UUID id,
            @Valid @RequestBody PersonPatchRequest request
    ) {
        PersonEntity updated = personService.patch(id, request);
        return ResponseEntity.ok(personMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        personService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}