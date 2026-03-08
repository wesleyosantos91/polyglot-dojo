package io.github.wesleyosantos91.contract;

import io.github.wesleyosantos91.AbstractIT;
import io.github.wesleyosantos91.domain.entity.PersonEntity;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;

/**
 * Classe base para os testes gerados pelo Spring Cloud Contract (modo WEBTESTCLIENT).
 *
 * Usa WebTestClient com adapter MockMvc — compatível com Spring Framework 7
 * (evita a API `MockHttpServletRequestBuilder.header(String, Object[])` removida).
 *
 * Herda de AbstractIT para reutilizar Testcontainers (PostgreSQL) e o perfil "it".
 *
 * Pré-condições de dados:
 *  - ID fixo "00000000-0000-0000-0000-000000000001" para GET 200
 *  - "existing@example.com" já cadastrado para testar 422
 */
public abstract class ContractBase extends AbstractIT {

    private static final UUID KNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    WebApplicationContext context;

    @BeforeEach
    void setupContracts() {
        WebTestClient webTestClient = MockMvcWebTestClient
                .bindToApplicationContext(context)
                .build();
        RestAssuredWebTestClient.webTestClient(webTestClient);

        // Dados fixos para os contratos GET 200 e POST 422
        PersonEntity known = new PersonEntity();
        known.setId(KNOWN_ID);
        known.setName("Contract Person");
        known.setEmail("contract.known@example.com");
        known.setBirthDate(LocalDate.of(1991, 1, 15));
        known.setCreatedAt(OffsetDateTime.now());
        personRepository.save(known);

        PersonEntity existing = new PersonEntity();
        existing.setId(UUID.randomUUID());
        existing.setName("Existing Person");
        existing.setEmail("existing@example.com");
        existing.setBirthDate(LocalDate.of(1990, 6, 20));
        existing.setCreatedAt(OffsetDateTime.now());
        personRepository.save(existing);
    }
}