package io.github.wesleyosantos91.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.github.wesleyosantos91.domain.entity.PersonEntity;
import io.github.wesleyosantos91.domain.repository.PersonRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

public class PersonStepDefinitions {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private PersonRepository personRepository;

    // Estado compartilhado entre steps do mesmo cenário
    private MvcTestResult lastResult;
    private UUID lastSavedId;

    // ─── Dado ─────────────────────────────────────────────────────────────────

    @Dado("que o banco de dados está limpo")
    public void bancoDeDadosEstaLimpo() {
        personRepository.deleteAll();
    }

    @Dado("que existe uma pessoa com email {string}")
    public void existePessoaComEmail(String email) {
        salvarPessoa("Pessoa Existente", email);
    }

    @Dado("que existe uma pessoa cadastrada com nome {string} e email {string}")
    public void existePessoaCadastrada(String nome, String email) {
        lastSavedId = salvarPessoa(nome, email).getId();
    }

    @Dado("que existem {int} pessoas cadastradas")
    public void existemPessoasCadastradas(int quantidade) {
        for (int i = 1; i <= quantidade; i++) {
            salvarPessoa("Pessoa " + i, "pessoa" + i + "@example.com");
        }
    }

    // ─── Quando ───────────────────────────────────────────────────────────────

    @Quando("eu envio uma requisição POST para {string} com o corpo:")
    public void envioPostComCorpo(String uri, String body) {
        lastResult = mvc.post().uri(uri)
                .contentType(APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    @Quando("eu envio uma requisição GET para {string}")
    public void envioGet(String uri) {
        lastResult = mvc.get().uri(uri).exchange();
    }

    @Quando("eu busco a pessoa pelo ID cadastrado")
    public void buscoPessoaPeloIdCadastrado() {
        lastResult = mvc.get().uri("/api/persons/{id}", lastSavedId).exchange();
    }

    @Quando("eu envio uma requisição PUT para a pessoa cadastrada com o corpo:")
    public void envioPutParaPessoaCadastrada(String body) {
        lastResult = mvc.put().uri("/api/persons/{id}", lastSavedId)
                .contentType(APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    @Quando("eu envio uma requisição PATCH para a pessoa cadastrada com o corpo:")
    public void envioPatchParaPessoaCadastrada(String body) {
        lastResult = mvc.patch().uri("/api/persons/{id}", lastSavedId)
                .contentType(APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    @Quando("eu deleto a pessoa cadastrada")
    public void deletoPessoaCadastrada() {
        lastResult = mvc.delete().uri("/api/persons/{id}", lastSavedId).exchange();
    }

    @Quando("eu envio uma requisição DELETE para {string}")
    public void envioDelete(String uri) {
        lastResult = mvc.delete().uri(uri).exchange();
    }

    // ─── Então ────────────────────────────────────────────────────────────────

    @Entao("o status da resposta deve ser {int}")
    public void statusDeveSerStatusCode(int statusCode) {
        assertThat(lastResult).hasStatus(statusCode);
    }

    @E("a resposta deve conter o campo {string} com valor {string}")
    public void respostaContemCampoComValor(String jsonPath, String valor) {
        assertThat(lastResult).matches(
                MockMvcResultMatchers.jsonPath("$." + jsonPath).value(valor));
    }

    @E("a resposta deve conter o campo {string} com valor numérico {int}")
    public void respostaContemCampoComValorNumerico(String jsonPath, int valor) {
        assertThat(lastResult).matches(
                MockMvcResultMatchers.jsonPath("$." + jsonPath).value(valor));
    }

    @E("a resposta deve conter um campo {string} não vazio")
    public void respostaContemCampoNaoVazio(String jsonPath) {
        assertThat(lastResult).matches(
                MockMvcResultMatchers.jsonPath("$." + jsonPath).isNotEmpty());
    }

    @E("a resposta deve conter o campo {string} como lista não vazia")
    public void respostaContemListaNaoVazia(String jsonPath) {
        assertThat(lastResult).matches(
                MockMvcResultMatchers.jsonPath("$." + jsonPath).isArray());
    }

    @E("o header {string} deve estar presente na resposta")
    public void headerDeveEstarPresente(String header) {
        assertThat(lastResult).headers().containsHeader(header);
    }

    @E("ao buscar a pessoa deletada o status deve ser {int}")
    public void aoBuscarPessoaDeletadaStatusDeve(int statusCode) {
        assertThat(mvc.get().uri("/api/persons/{id}", lastSavedId).exchange())
                .hasStatus(statusCode);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private PersonEntity salvarPessoa(String nome, String email) {
        PersonEntity entity = new PersonEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(nome);
        entity.setEmail(email.toLowerCase());
        entity.setBirthDate(LocalDate.of(1990, 1, 1));
        entity.setCreatedAt(OffsetDateTime.now());
        return personRepository.save(entity);
    }
}
