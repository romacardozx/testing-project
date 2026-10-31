package ar.unsam.vv.atalaya.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Contexto de Spring de los escenarios: SISTEMA completo con el perfil qa, accedido por MockMvc. */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("qa")
public class ConfiguracionCucumber {
}
