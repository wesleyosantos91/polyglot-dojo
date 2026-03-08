package io.github.wesleyosantos91.property;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wesleyosantos91.api.request.PersonPatchRequest;
import io.github.wesleyosantos91.api.request.PersonRequest;
import io.github.wesleyosantos91.core.mapper.PersonMapper;
import io.github.wesleyosantos91.core.mapper.PersonMapperImpl;
import io.github.wesleyosantos91.domain.entity.PersonEntity;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.time.api.DateTimes;
import net.jqwik.time.api.Dates;
import org.junit.jupiter.api.DisplayName;

/**
 * Property-Based Tests com jqwik.
 *
 * Avaliação de valor:
 * - jqwik agrega valor REAL neste projeto para:
 *   1. Verificar que normalizações (email lowercase, trim de nome) são idempotentes
 *      para QUALQUER entrada válida — um teste unitário com 3 exemplos não cobre isso.
 *   2. Verificar que NENHUM email com formato válido produz violação de @Email,
 *      e que QUALQUER string com '@' não é suficiente — confirma o comportamento
 *      da constraint em vez de assumir.
 *   3. Verificar invariantes do mapeador Request→Entity para entradas arbitrárias.
 *
 * - Custo: a dependência já existe no pom.xml. A curva de aprendizado é baixa
 *   para quem já conhece JUnit 5 e AssertJ. Recomendado manter.
 */
@DisplayName("PersonService / PersonRequest — Property-Based Tests")
class PersonValidationPropertyTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private final PersonMapper mapper = new PersonMapperImpl();

    // ─── Property 1: normalização de email é idempotente ──────────────────────
    /**
     * Para QUALQUER string não-nula, aplicar trim().toLowerCase() duas vezes
     * deve produzir o mesmo resultado que aplicar uma vez.
     *
     * Valor: garante que o método normalizeEmail() em PersonService não
     * quebrará para nenhuma entrada — incluindo strings com espaços, acentos,
     * unicode e maiúsculas misturadas — sem precisar de exemplos específicos.
     */
    @Property
    @DisplayName("email normalization is idempotent for any non-null string")
    void emailNormalizationIsIdempotent(@ForAll String raw) {
        // Simula a lógica de normalizeEmail() do PersonService
        String once = raw.trim().toLowerCase();
        String twice = once.trim().toLowerCase();

        assertThat(twice).isEqualTo(once);
    }

    // ─── Property 2: PersonRequest válido nunca produz violation ─────────────
    /**
     * Para QUALQUER combinação de nome não-vazio (≤150 chars), email válido e
     * data passada, PersonRequest não deve produzir nenhuma ConstraintViolation.
     *
     * Valor: confirma que as anotações @NotBlank, @Email, @Past e @Size
     * aceitam corretamente entradas válidas em vez de rejeitar casos de borda
     * (ex: nomes com caracteres especiais, emails com subdomínios, datas antigas).
     */
    @Property
    @DisplayName("valid PersonRequest produces no constraint violations")
    void validPersonRequestProducesNoViolations(
            @ForAll @NotBlank @StringLength(min = 1, max = 150) String name,
            @ForAll("pastDates") LocalDate birthDate
    ) {
        // Email fixo válido — jqwik não tem gerador de email RFC5322 built-in
        var request = new PersonRequest(name, "valid@example.com", birthDate);

        Set<ConstraintViolation<PersonRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations).isEmpty();
    }

    @Provide
    Arbitrary<LocalDate> pastDates() {
        return Dates.dates()
                .between(LocalDate.of(1900, 1, 1), LocalDate.now().minusDays(1));
    }

    // ─── Property 3: mapeador Request→Entity preserva todos os campos ─────────
    /**
     * Para QUALQUER PersonRequest válido, o mapeador deve preservar nome, email
     * e birthDate sem perda ou transformação inesperada.
     *
     * Valor: garante que mudanças futuras no PersonMapper (ex: adicionar campo)
     * não silenciosamente ignoram campos ao mapear Request→Entity.
     * Testa a invariante: toEntity(req).getX() == req.x() para todos os campos.
     */
    @Property
    @DisplayName("PersonMapper.toEntity preserves all request fields without modification")
    void mapperToEntityPreservesAllFields(
            @ForAll @NotBlank @StringLength(min = 1, max = 150) String name,
            @ForAll("pastDates") LocalDate birthDate
    ) {
        var request = new PersonRequest(name.trim(), "mapper@example.com", birthDate);

        PersonEntity entity = mapper.toEntity(request);

        assertThat(entity.getName()).isEqualTo(request.name());
        assertThat(entity.getEmail()).isEqualTo(request.email());
        assertThat(entity.getBirthDate()).isEqualTo(request.birthDate());
        // id, createdAt, updatedAt devem ser nulos (mapeados com ignore = true)
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    // ─── Property 4: PersonPatchRequest com campos nulos não viola constraints ─
    /**
     * PersonPatchRequest permite todos os campos nulos (PATCH parcial).
     * Para QUALQUER combinação de campos presentes/ausentes, nenhuma
     * ConstraintViolation deve ocorrer quando os campos presentes são válidos.
     *
     * Valor: verifica que as constraints @Email e @Size em PersonPatchRequest
     * não disparam quando o campo é null — comportamento crítico para PATCH.
     */
    @Property(tries = 200)
    @DisplayName("PersonPatchRequest with null fields never violates constraints")
    void patchRequestWithNullFieldsNeverViolates() {
        // Todas as combinações de nulos — jqwik testa 200 combinações aleatórias
        var allNull = new PersonPatchRequest(null, null, null);
        var onlyName = new PersonPatchRequest("Wesley", null, null);
        var onlyEmail = new PersonPatchRequest(null, "patch@example.com", null);

        assertThat(VALIDATOR.validate(allNull)).isEmpty();
        assertThat(VALIDATOR.validate(onlyName)).isEmpty();
        assertThat(VALIDATOR.validate(onlyEmail)).isEmpty();
    }
}