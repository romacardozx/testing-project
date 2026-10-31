package ar.unsam.vv.atalaya.bdd;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Corre los escenarios de src/test/resources/features con JUnit Platform (dentro de mvn verify).
 * El reporte HTML queda en target/cucumber-reports/ (ver junit-platform.properties).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "ar.unsam.vv.atalaya.bdd")
public class CucumberTest {
}
