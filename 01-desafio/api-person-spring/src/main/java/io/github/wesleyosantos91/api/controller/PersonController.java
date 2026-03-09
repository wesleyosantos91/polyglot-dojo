package io.github.wesleyosantos91.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/persons")
@Tag(name = "Persons", description = "Operacoes de CRUD de pessoas")
public class PersonController {

    private final PersonService personService;
    private final PersonMapper personMapper;

    public PersonController(PersonService personService, PersonMapper personMapper) {
        this.personService = personService;
        this.personMapper = personMapper;
    }

    @Operation(summary = "Busca pessoa por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pessoa encontrada"),
            @ApiResponse(responseCode = "400", description = "ID invalido"),
            @ApiResponse(responseCode = "404", description = "Pessoa nao encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PersonResponse> findById(
            @Parameter(description = "ID da pessoa (UUID)") @PathVariable UUID id
    ) {
        PersonEntity person = personService.findByIdOrThrow(id);
        return ResponseEntity.ok(personMapper.toResponse(person));
    }

    @Operation(summary = "Lista pessoas com filtros e paginacao")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada retornada com sucesso")
    })
    @GetMapping
    public ResponseEntity<Page<PersonResponse>> findAllPaged(
            @Parameter(description = "Filtro por nome (contains, case-insensitive)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filtro por email (contains, case-insensitive)")
            @RequestParam(required = false) String email,
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PersonResponse> responsePage = personService.findAllPaged(name, email, pageable)
                .map(personMapper::toResponse);

        return ResponseEntity.ok(responsePage);
    }

    @Operation(summary = "Cria uma nova pessoa")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pessoa criada"),
            @ApiResponse(responseCode = "400", description = "Payload invalido"),
            @ApiResponse(responseCode = "422", description = "Regra de negocio violada")
    })
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

    @Operation(summary = "Atualiza pessoa por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pessoa atualizada"),
            @ApiResponse(responseCode = "400", description = "Payload ou ID invalido"),
            @ApiResponse(responseCode = "404", description = "Pessoa nao encontrada"),
            @ApiResponse(responseCode = "422", description = "Regra de negocio violada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PersonResponse> update(
            @Parameter(description = "ID da pessoa (UUID)") @PathVariable UUID id,
            @Valid @RequestBody PersonRequest request
    ) {
        PersonEntity payload = personMapper.toEntity(request);
        PersonEntity updated = personService.update(id, payload);
        return ResponseEntity.ok(personMapper.toResponse(updated));
    }

    @Operation(summary = "Atualiza parcialmente pessoa por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pessoa atualizada parcialmente"),
            @ApiResponse(responseCode = "400", description = "Payload ou ID invalido"),
            @ApiResponse(responseCode = "404", description = "Pessoa nao encontrada"),
            @ApiResponse(responseCode = "422", description = "Regra de negocio violada")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<PersonResponse> patch(
            @Parameter(description = "ID da pessoa (UUID)") @PathVariable UUID id,
            @Valid @RequestBody PersonPatchRequest request
    ) {
        PersonEntity updated = personService.patch(id, request);
        return ResponseEntity.ok(personMapper.toResponse(updated));
    }

    @Operation(summary = "Remove pessoa por id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pessoa removida"),
            @ApiResponse(responseCode = "404", description = "Pessoa nao encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da pessoa (UUID)") @PathVariable UUID id
    ) {
        personService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
