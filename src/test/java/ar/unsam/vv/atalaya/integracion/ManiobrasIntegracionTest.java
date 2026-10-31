package ar.unsam.vv.atalaya.integracion;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.unsam.vv.atalaya.PruebaApiQa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * RF-09 · Maniobras de punta a punta con H2 y reloj de QA. Agrupación en una alerta INFORMATIVA
 * (CP-27), bordes de la banda (CP-28), aviso de canal fuera de la agrupación (CP-29) y expiración
 * (CP-30). La banda se evalúa contra el valor consolidado de la potencia térmica.
 */
class ManiobrasIntegracionTest extends PruebaApiQa {

    @Test
    @DisplayName("CP-27 · 10 estimaciones dentro de la banda 70 a 90 generan una sola alerta INFORMATIVA")
    void variacionesDentroDeLaBanda() throws Exception {
        enviarPotencia("80", "80", "80");
        idManiobra("70", "90", 30);

        for (int i = 0; i < 10; i++) {
            inyectarEstimacion(i % 2 == 0 ? "PROCESO" : "BOMBA", "50");
        }

        consultarAlertas()
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].clase").value("INFORMATIVA"))
                .andExpect(jsonPath("$[0].agrupadas").value(10))
                .andExpect(jsonPath("$[0].requiereReconocimiento").value(false));
    }

    @ParameterizedTest(name = "CP-28 · banda 70 a 90, potencia {0} % → clase de alerta {1}")
    @CsvSource({"70, INFORMATIVA", "69.9, RIESGO", "90, INFORMATIVA", "90.1, RIESGO"})
    void bordesDeLaBanda(String potencia, String clase) throws Exception {
        enviarPotencia(potencia, potencia, potencia);
        idManiobra("70", "90", 30);

        inyectarEstimacion("PROCESO", "50");

        consultarAlertas().andExpect(jsonPath("$[0].clase").value(clase));
    }

    @Test
    @DisplayName("CP-29 · con la maniobra activa, el canal C que discrepa 2,5 % se avisa aparte")
    void avisoDeCanalFueraDeLaAgrupacion() throws Exception {
        idManiobra("70", "90", 30);
        enviarPotencia("80", "80", "83");

        inyectarEstimacion("PROCESO", "50");

        consultarAlertas().andExpect(jsonPath("$[0].clase").value("INFORMATIVA"));
        consultarMagnitud("POTENCIA_TERMICA")
                .andExpect(jsonPath("$.canales[2].motivo").value("DISCREPANCIA"))
                .andExpect(jsonPath("$.avisos", contains("CANAL_EXCLUIDO:C")));
    }

    @Test
    @DisplayName("CP-29 · si la potencia no es utilizable, la estimación genera una alerta normal")
    void magnitudNoUtilizableNoSeAgrupa() throws Exception {
        idManiobra("70", "90", 30);
        enviarLectura("POTENCIA_TERMICA", "A", "80");

        inyectarEstimacion("PROCESO", "50");

        consultarAlertas()
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].clase").value("RIESGO"));
    }

    @Test
    @DisplayName("CP-30 · a 29:59 la estimación se agrupa; a 30:00 la maniobra expira y se alerta normal")
    void maniobraQueExpira() throws Exception {
        long id = idManiobra("70", "90", 30);

        irASegundo(1799);
        enviarPotencia("80", "80", "80");
        inyectarEstimacion("PROCESO", "50");
        consultarAlertas().andExpect(jsonPath("$[0].clase").value("INFORMATIVA"));

        irASegundo(1800);
        enviarPotencia("80", "80", "80");
        inyectarEstimacion("PROCESO", "50");
        consultarAlertas()
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].clase").value("RIESGO"));

        mvc.perform(delete("/api/v1/maniobras/" + id).with(como("jefe")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MANIOBRA_NO_ACTIVA"));
    }

    @Test
    @DisplayName("RF-09 · DELETE pasa la maniobra a FINALIZADA y se vuelve a alertar normal")
    void finalizarManiobra() throws Exception {
        enviarPotencia("80", "80", "80");
        long id = idManiobra("70", "90", 30);

        mvc.perform(delete("/api/v1/maniobras/" + id).with(como("jefe")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"));

        inyectarEstimacion("PROCESO", "50");
        consultarAlertas().andExpect(jsonPath("$[0].clase").value("RIESGO"));
    }

    @Test
    @DisplayName("RF-09 · no se puede declarar una segunda maniobra mientras hay una activa (409)")
    void unaSolaManiobraActiva() throws Exception {
        idManiobra("70", "90", 30);

        declararManiobra("60", "80", 30)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MANIOBRA_ACTIVA"));
    }

    @Test
    @DisplayName("RF-09 · banda con mínimo mayor que el máximo → 400")
    void bandaInvalida() throws Exception {
        declararManiobra("90", "70", 30).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RF-09 · la duración máxima por defecto es 30 minutos")
    void duracionPorDefecto() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/maniobras")
                        .with(como("jefe"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"magnitud\": \"POTENCIA_TERMICA\", \"bandaMin\": 70, \"bandaMax\": 90}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracionMaximaMin").value(30));
    }
}
