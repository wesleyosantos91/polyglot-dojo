package io.github.wesleyosantos91;

import io.github.wesleyosantos91.domain.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Classe base para todos os testes de integração.
 *
 * Usa a API moderna do Spring Framework 7:
 * - MockMvcTester (AssertJ) no lugar do MockMvc (Hamcrest)
 * - WebEnvironment.MOCK: contexto completo sem servidor HTTP real —
 *   mais rápido que RANDOM_PORT e suficiente para testar toda a stack MVC,
 *   filtros, validação, exception handlers e JPA.
 *
 * Decisões de design:
 * - Container iniciado via bloco static (não via @Container/@Testcontainers):
 *   garante que o container sobe UMA vez para toda a suite e nunca para
 *   entre classes, evitando conflito com o cache de contexto do Spring Test.
 * - @DynamicPropertySource: injeta a URL do container nos datasources customizados
 *   (app.datasource.writer/reader) — @ServiceConnection não funciona aqui pois
 *   a app não usa o spring.datasource padrão.
 * - Sem @Transactional: rollback automático esconde bugs de routing writer/reader.
 * - deleteAll() no @BeforeEach: isolamento real entre cenários.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("it")
public abstract class AbstractIT {

    // ─── Container ────────────────────────────────────────────────────────────
    // Iniciado uma única vez para toda a suite via bloco static.
    // NÃO usar @Container/@Testcontainers: o ciclo de vida automático para o
    // container após cada classe de teste, invalidando o contexto cacheado
    // pelo Spring Test (a URL do container muda de porta a cada reinício).
    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18.3"))
                    .withDatabaseName("it_db")
                    .withUsername("it_user")
                    .withPassword("it_pass")
                    .withReuse(true);

    static {
        POSTGRES.start();
    }

    // ─── Datasources dinâmicos ────────────────────────────────────────────────
    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("app.datasource.writer.url",      POSTGRES::getJdbcUrl);
        registry.add("app.datasource.writer.username", POSTGRES::getUsername);
        registry.add("app.datasource.writer.password", POSTGRES::getPassword);
        registry.add("app.datasource.reader.url",      POSTGRES::getJdbcUrl);
        registry.add("app.datasource.reader.username", POSTGRES::getUsername);
        registry.add("app.datasource.reader.password", POSTGRES::getPassword);
    }

    // ─── Injeções ─────────────────────────────────────────────────────────────
    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    protected PersonRepository personRepository;

    // ─── Isolamento ───────────────────────────────────────────────────────────
    @BeforeEach
    void cleanDatabase() {
        personRepository.deleteAll();
    }
}