package ar.unsam.vv.atalaya.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.unsam.vv.atalaya.PruebaApiQa;
import ar.unsam.vv.atalaya.config.RelojMutable;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

/**
 * CP-31 · Pairwise sobre RF-03, RF-07/08 y RF-09 (sección 7.5 del informe). Cinco parámetros
 * (maniobra 3 × canales 3 × rol 3 × reconocimiento 3 × nivel 2 = 162 combinaciones) reducidos a 10
 * filas que cubren todos los pares (datos/pairwise.csv). El esperado se deriva con las reglas:
 *
 * <ul>
 *   <li>Siempre se emite una alerta de RIESGO: en ACTIVA la potencia (80 %) queda fuera de la banda
 *       90 a 100; en EXPIRADA la banda 70 a 90 ya no se aplica.</li>
 *   <li>Canales distintos de 3_CONFIABLES → la magnitud informa avisos.</li>
 *   <li>Reconocimiento ANTES (30 s) → no escala. DESPUES o NUNCA → escala justo en el plazo (90 s
 *       alto, 45 s crítico) y suma al jefe de turno o al supervisor de SO.</li>
 *   <li>Estado final: NUNCA → ESCALADA; si no, RECONOCIDA.</li>
 * </ul>
 */
class PairwiseIntegracionTest extends PruebaApiQa {

    private static final Map<String, String> USUARIO = Map.of(
            "OPERADOR_REACTOR", "operador", "JEFE_TURNO", "jefe", "SUPERVISOR_SO", "supervisor");

    @ParameterizedTest(name = "CP-31 · fila {0}: maniobra {1}, canales {2}, reconoce {3} {4}, nivel {5}")
    @CsvFileSource(resources = "/datos/pairwise.csv", numLinesToSkip = 1)
    void combinacionesPairwise(int fila, String maniobra, String canales, String rol, String reconocimiento,
                               String nivel) throws Exception {
        prepararManiobra(maniobra);
        prepararCanales(canales);

        double emision = segundoActual();
        long id = alertaDe("PROCESO", "ALTO".equals(nivel) ? "50" : "80");
        assertThat(alerta(id).get("clase")).isEqualTo("RIESGO");
        List<String> avisos = avisosDePotencia();
        if ("3_CONFIABLES".equals(canales)) {
            assertThat(avisos).as("avisos de canal").isEmpty();
        } else {
            assertThat(avisos).as("avisos de canal").isNotEmpty();
        }

        int plazo = "ALTO".equals(nivel) ? 90 : 45;
        String escalaA = "ALTO".equals(nivel) ? "JEFE_TURNO" : "SUPERVISOR_SO";

        if ("ANTES".equals(reconocimiento)) {
            irASegundo(emision + 30);
            reconocer(id, USUARIO.get(rol)).andExpect(status().isOk());
            irASegundo(emision + plazo + 5);
            assertThat(alerta(id).get("estado")).isEqualTo("RECONOCIDA");
            assertThat(destinatarios(id)).doesNotContain(escalaA);
            return;
        }

        irASegundo(emision + plazo - 1);
        assertThat(alerta(id).get("estado")).as("un segundo antes del plazo").isEqualTo("EMITIDA");
        irASegundo(emision + plazo);
        assertThat(alerta(id).get("estado")).as("en el plazo").isEqualTo("ESCALADA");
        assertThat(destinatarios(id)).contains(escalaA);

        irASegundo(emision + plazo + 5);
        if ("DESPUES".equals(reconocimiento)) {
            reconocer(id, USUARIO.get(rol)).andExpect(status().isOk());
        }
        assertThat(alerta(id).get("estado")).isEqualTo("NUNCA".equals(reconocimiento) ? "ESCALADA" : "RECONOCIDA");
    }

    private void prepararManiobra(String maniobra) throws Exception {
        switch (maniobra) {
            case "EXPIRADA" -> {
                idManiobra("70", "90", 1);
                avanzarSegundos(60);
            }
            case "ACTIVA" -> idManiobra("90", "100", 30);
            default -> { }
        }
    }

    private void prepararCanales(String canales) throws Exception {
        switch (canales) {
            case "3_CONFIABLES" -> enviarPotencia("80", "80", "80");
            case "1_NO_CONFIABLE" -> enviarPotencia("80", "80", "85");
            case "MENOS_DE_2" -> {
                enviarPotencia("80", "80", "80");
                for (int i = 0; i < 11; i++) {
                    avanzarSegundos(1);
                    enviarLectura("POTENCIA_TERMICA", "A", "80");
                }
            }
            default -> throw new IllegalArgumentException(canales);
        }
    }

    private double segundoActual() {
        return Duration.between(RelojMutable.T0, reloj.instant()).toMillis() / 1000.0;
    }

    private Map<String, Object> alerta(long id) throws Exception {
        String cuerpo = consultarAlertas().andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> encontradas = JsonPath.read(cuerpo, "$[?(@.id == " + id + ")]");
        return encontradas.get(0);
    }

    @SuppressWarnings("unchecked")
    private List<String> destinatarios(long id) throws Exception {
        return (List<String>) alerta(id).get("destinatarios");
    }

    private List<String> avisosDePotencia() throws Exception {
        String cuerpo = consultarMagnitud("POTENCIA_TERMICA").andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.avisos");
    }
}
