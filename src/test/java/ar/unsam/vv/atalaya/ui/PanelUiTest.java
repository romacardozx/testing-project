package ar.unsam.vv.atalaya.ui;

import static org.assertj.core.api.Assertions.assertThat;

import ar.unsam.vv.atalaya.aplicacion.AlertaVista;
import ar.unsam.vv.atalaya.aplicacion.NuevaEstimacion;
import ar.unsam.vv.atalaya.aplicacion.NuevaLectura;
import ar.unsam.vv.atalaya.aplicacion.ServicioAlertas;
import ar.unsam.vv.atalaya.aplicacion.ServicioEstimaciones;
import ar.unsam.vv.atalaya.aplicacion.ServicioLecturas;
import ar.unsam.vv.atalaya.aplicacion.ServicioReset;
import ar.unsam.vv.atalaya.config.RelojMutable;
import ar.unsam.vv.atalaya.dominio.Canal;
import ar.unsam.vv.atalaya.dominio.EstadoAlerta;
import ar.unsam.vv.atalaya.dominio.Magnitud;
import ar.unsam.vv.atalaya.dominio.TipoEstimacion;
import ar.unsam.vv.atalaya.ui.paginas.PaginaLogin;
import ar.unsam.vv.atalaya.ui.paginas.PaginaPanel;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Pruebas de interfaz con Selenium sobre el panel (UI-01 a UI-03). Levanta el SISTEMA en un puerto
 * real con el perfil qa. Los datos se preparan con los servicios (equivalente a los endpoints de QA)
 * y la verificación se hace solo por lo que muestra la página. Por defecto HtmlUnit; para la demo,
 * {@code make ui-chrome}; para que el navegador quede abierto al final de cada prueba,
 * {@code make ui-demo} (modo pausa).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("qa")
class PanelUiTest {

    @LocalServerPort
    private int puerto;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ServicioReset servicioReset;

    @Autowired
    private RelojMutable reloj;

    @Autowired
    private ServicioEstimaciones estimaciones;

    @Autowired
    private ServicioLecturas lecturas;

    @Autowired
    private ServicioAlertas alertas;

    private WebDriver driver;
    private long secuencia = 1000;

    /**
     * Captura y modo pausa: corren antes de @AfterEach, cuando ya se sabe si la prueba pasó (ver
     * Navegador).
     */
    @RegisterExtension
    final AfterTestExecutionCallback alTerminar = contexto -> {
        boolean aprobada = contexto.getExecutionException().isEmpty();
        Navegador.capturarSiCorresponde(driver, contexto.getDisplayName(),
                contexto.getRequiredTestMethod().getName(), aprobada);
        Navegador.pausarSiCorresponde(driver, contexto.getDisplayName(), aprobada);
    };

    @BeforeEach
    void preparar() {
        servicioReset.limpiar();
        reloj.reiniciar();
        driver = Navegador.crear();
    }

    @AfterEach
    void cerrar() {
        try {
            driver.quit();
        } catch (org.openqa.selenium.WebDriverException e) {
            // En modo pausa el usuario ya cerró la ventana.
        }
    }

    private PaginaPanel ingresarComo(String usuario) {
        return new PaginaLogin(driver, "http://localhost:" + puerto).abrir().ingresar(usuario, usuario + "123");
    }

    private Long inyectar(String probabilidad) {
        return estimaciones.registrar(new NuevaEstimacion(TipoEstimacion.PROCESO, new BigDecimal(probabilidad),
                List.of())).alertaId();
    }

    private void lectura(Canal canal, String valor) {
        lecturas.registrar(new NuevaLectura(Magnitud.TEMPERATURA_PRIMARIO, canal, new BigDecimal(valor),
                ++secuencia, RelojMutable.T0));
    }

    @Test
    @DisplayName("UI-01 · el operador ve la alerta ALTO y la reconoce en 2 interacciones o menos")
    void reconocerDesdeElPanel() {
        long id = inyectar("50");

        PaginaPanel panel = ingresarComo("operador");
        assertThat(panel.nivelDeAlerta(id)).isEqualTo("ALTO");

        panel.reconocer(id);

        assertThat(panel.clics()).isLessThanOrEqualTo(2);
        assertThat(panel.muestraAlerta(id)).as("la alerta reconocida deja de estar activa").isFalse();
        AlertaVista guardada = alertas.listar(Optional.empty()).get(0);
        assertThat(guardada.estado()).isEqualTo(EstadoAlerta.RECONOCIDA);
    }

    @Test
    @DisplayName("UI-02 · la probabilidad y el nivel del panel coinciden con GET /estimaciones/actual")
    void panelCoincideConLaApi() {
        inyectar("62.5");

        PaginaPanel panel = ingresarComo("operador");
        String api = rest.withBasicAuth("operador", "operador123")
                .getForObject("/api/v1/estimaciones/actual", String.class);

        assertThat(new BigDecimal(panel.probabilidad("PROCESO")))
                .isEqualByComparingTo(new BigDecimal(JsonPath.read(api, "$[0].probabilidad").toString()));
        assertThat(panel.nivelEstimado("PROCESO")).isEqualTo(JsonPath.read(api, "$[0].nivel"));
    }

    @Test
    @DisplayName("UI-03 · el panel muestra el aviso CANAL_EXCLUIDO del canal que discrepa")
    void panelMuestraAvisoDeCanal() {
        lectura(Canal.A, "300.00");
        lectura(Canal.B, "300.00");
        lectura(Canal.C, "306.64");

        PaginaPanel panel = ingresarComo("operador");

        assertThat(panel.avisos("TEMPERATURA_PRIMARIO")).contains("CANAL_EXCLUIDO:C");
        assertThat(panel.canal("TEMPERATURA_PRIMARIO", "C")).contains("NO_CONFIABLE").contains("DISCREPANCIA");
    }

    @Test
    @DisplayName("UI-03 · cada canal se muestra en su propio renglón [defecto ATA-18, corregido]")
    void canalesEnRenglonesSeparados() {
        lectura(Canal.A, "300.00");
        lectura(Canal.B, "300.00");
        lectura(Canal.C, "306.64");

        PaginaPanel panel = ingresarComo("operador");

        assertThat(panel.lineasDeCanales("TEMPERATURA_PRIMARIO"))
                .containsExactly("A: CONFIABLE", "B: CONFIABLE", "C: NO_CONFIABLE (DISCREPANCIA)");
    }

    @Test
    @DisplayName("UI-01 · el auditor ve la alerta pero no tiene botón Reconocer")
    void auditorNoReconoce() {
        long id = inyectar("50");

        PaginaPanel panel = ingresarComo("auditor");

        assertThat(panel.filaAlerta(id).findElements(org.openqa.selenium.By.cssSelector("[data-test=reconocer]")))
                .isEmpty();
    }
}
