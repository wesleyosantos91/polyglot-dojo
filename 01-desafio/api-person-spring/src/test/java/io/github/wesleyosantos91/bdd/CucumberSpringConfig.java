package io.github.wesleyosantos91.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import io.github.wesleyosantos91.AbstractIT;

/**
 * Conecta o contexto Spring dos testes BDD à infraestrutura do AbstractIT
 * (Testcontainers PostgreSQL + MockMvcTester + perfil "it").
 */
@CucumberContextConfiguration
public class CucumberSpringConfig extends AbstractIT {
}