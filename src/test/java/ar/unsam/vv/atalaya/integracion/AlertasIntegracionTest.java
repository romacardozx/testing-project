package ar.unsam.vv.atalaya.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import ar.unsam.vv.atalaya.PruebaApiQa;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RF-07/08 · Alertas de punta a punta con H2 y reloj de QA. Destinatarios por nivel (CP-18),
 * repetición (CP-19, CP-20), escalamiento (CP-21, CP-22) y reconocimiento con registro en la
 * base (CP-23, caja gris).
 */
class AlertasIntegracionTest extends PruebaApiQa {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("CP-18 · 20 %, 50 % y 80 %: canales y destinatarios según el nivel")
    void destinatariosSegunNivel() throws Exception {
        inyectarEstimacion("PROCESO", "20");
        inyectarEstimacion("PROCESO", "50");
        inyectarEstimacion("PROCESO", "80");

        consultarAlertas()
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].nivel").value("ATENCION"))
                .andExpect(jsonPath("$[0].canales", contains("VISUAL")))
                .andExpect(jsonPath("$[0].destinatarios", hasSize(0)))
                .andExpect(jsonPath("$[0].requiereReconocimiento").value(false))
                .andExpect(jsonPath("$[1].nivel").value("ALTO"))
                .andExpect(jsonPath("$[1].destinatarios", contains("OPERADOR_REACTOR")))
                .andExpect(jsonPath("$[1].requiereReconocimiento").value(true))
                .andExpect(jsonPath("$[2].nivel").value("CRITICO"))
                .andExpect(jsonPath("$[2].canales", contains("VISUAL", "SONORA")))
                .andExpect(jsonPath("$[2].destinatarios", contains("OPERADOR_REACTOR", "JEFE_TURNO")));
    }

    @Test
    @DisplayName("CP-18 · una estimación NORMAL no genera alerta")
    void normalNoGeneraAlerta() throws Exception {
        inyectarEstimacion("PROCESO", "10").andExpect(jsonPath("$.alertaId").doesNotExist());

        consultarAlertas().andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("CP-19 · misma estimación a T0+179 s no se repite; a T0+180 s sí (2 alertas)")
    void noRepetirDuranteTresMinutos() throws Exception {
        inyectarEstimacion("PROCESO", "50");
        irASegundo(179);
        inyectarEstimacion("PROCESO", "50");
        irASegundo(180);
        inyectarEstimacion("PROCESO", "50");

        consultarAlertas().andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("CP-19 · la ventana es por tipo: otro tipo con el mismo nivel sí genera alerta")
    void ventanaPorTipo() throws Exception {
        inyectarEstimacion("PROCESO", "50");
        inyectarEstimacion("FUGA", "50");

        consultarAlertas().andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("CP-20 · ALTO en T0 y 80 % en T0+60 s: se emite la alerta CRITICO")
    void empeoraAntesDeTresMinutos() throws Exception {
        inyectarEstimacion("PROCESO", "50");
        irASegundo(60);
        inyectarEstimacion("PROCESO", "80");

        consultarAlertas()
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].nivel").value("CRITICO"));
    }

    @Test
    @DisplayName("CP-21 · crítico sin reconocer: 44 s no escala, 45 s suma al supervisor de SO, 46 s no duplica")
    void escalamientoCritico() throws Exception {
        alertaDe("PROCESO", "80");

        irASegundo(44);
        consultarAlertas().andExpect(jsonPath("$[0].estado").value("EMITIDA"));

        irASegundo(45);
        consultarAlertas()
                .andExpect(jsonPath("$[0].estado").value("ESCALADA"))
                .andExpect(jsonPath("$[0].destinatarios",
                        contains("OPERADOR_REACTOR", "JEFE_TURNO", "SUPERVISOR_SO")));

        irASegundo(46);
        consultarAlertas().andExpect(jsonPath("$[0].destinatarios", hasSize(3)));
    }

    @Test
    @DisplayName("CP-22 · alto sin reconocer: 89 s no escala, 90 s suma al jefe de turno, 91 s no duplica")
    void escalamientoAlto() throws Exception {
        alertaDe("PROCESO", "50");

        irASegundo(89);
        consultarAlertas().andExpect(jsonPath("$[0].estado").value("EMITIDA"));

        irASegundo(90);
        consultarAlertas()
                .andExpect(jsonPath("$[0].estado").value("ESCALADA"))
                .andExpect(jsonPath("$[0].destinatarios", contains("OPERADOR_REACTOR", "JEFE_TURNO")));

        irASegundo(91);
        consultarAlertas().andExpect(jsonPath("$[0].destinatarios", hasSize(2)));
    }

    @Test
    @DisplayName("CP-23 · reconocer a los 44 s evita el escalamiento y guarda usuario, rol y hora")
    void reconocimientoAntesDelPlazo() throws Exception {
        long id = alertaDe("PROCESO", "80");
        irASegundo(44);
        reconocer(id, "operador");
        irASegundo(60);

        consultarAlertas()
                .andExpect(jsonPath("$[0].estado").value("RECONOCIDA"))
                .andExpect(jsonPath("$[0].destinatarios", hasSize(2)));

        Map<String, Object> fila = jdbc.queryForMap(
                "select usuario, rol, instante from reconocimiento where alerta_id = ?", id);
        assertThat(fila.get("USUARIO")).isEqualTo("operador");
        assertThat(fila.get("ROL")).isEqualTo("OPERADOR_REACTOR");
        assertThat(((OffsetDateTime) fila.get("INSTANTE")).toInstant())
                .isEqualTo(Instant.parse("2026-01-01T00:00:44Z"));
    }

    @Test
    @DisplayName("CP-31 · una alerta ESCALADA se puede reconocer")
    void reconocerEscalada() throws Exception {
        long id = alertaDe("PROCESO", "50");
        irASegundo(95);

        reconocer(id, "jefe").andExpect(jsonPath("$.estado").value("RECONOCIDA"));
    }
}
