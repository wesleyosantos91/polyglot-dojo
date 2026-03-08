package io.github.wesleyosantos91.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Suite JUnit 5 para execução dos cenários BDD com Cucumber.
 * Executado como IT pelo maven-failsafe-plugin via naming *IT.java — não!
 * Essa classe é descoberta pelo JUnit Platform Suite engine.
 * Para rodar: mvn verify (failsafe detecta a suite pelo @Suite)
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "io.github.wesleyosantos91.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
public class CucumberSuite {
}