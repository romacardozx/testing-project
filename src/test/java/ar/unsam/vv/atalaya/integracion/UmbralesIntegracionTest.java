package ar.unsam.vv.atalaya.integracion;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.unsam.vv.atalaya.PruebaApiQa;
import ar.unsam.vv.atalaya.aplicacion.ConflictoException;
import ar.unsam.vv.atalaya.aplicacion.ServicioUmbrales;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * RF-12 · Cambio de umbrales con doble aprobación, de punta a punta. Límites por la API (CP-32) y
 * tabla de decisión de la aprobación (CP-33): sin aprobar, aprueba el mismo usuario, aprueba el jefe.
 * Aprobar dos veces la misma solicitud (CP-40).
 */
class UmbralesIntegracionTest extends PruebaApiQa {

    @Autowired
    private ServicioUmbrales servicioUmbrales;

    private ResultActions solicitar(String alto, String critico) throws Exception {
        return mvc.perform(put("/api/v1/config/umbrales").with(como("supervisor"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"alto\": " + alto + ", \"critico\": " + critico + "}"));
    }

    private long idSolicitud(String alto, String critico) throws Exception {
        String cuerpo = solicitar(alto, critico).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private ResultActions aprobar(long id, String usuario) throws Exception {
        return mvc.perform(post("/api/v1/config/umbrales/" + id + "/aprobacion").with(como(usuario)));
    }

    private ResultActions vigentes() throws Exception {
        return mvc.perform(get("/api/v1/config/umbrales").with(como("operador")));
    }

    @ParameterizedTest(name = "CP-32 · PUT alto {0} / crítico {1} → HTTP {2}")
    @CsvSource({
            "29.9, 75,    422", "30, 75,    201", "50, 75,     201", "50.1, 75,  422",
            "40,   64.9,  422", "40, 65,    201", "40, 100,    201", "40, 100.1, 422"
    })
    void limitesDeUmbrales(String alto, String critico, int esperado) throws Exception {
        solicitar(alto, critico).andExpect(status().is(esperado));

        vigentes().andExpect(jsonPath("$.alto").value(40)).andExpect(jsonPath("$.critico").value(75));
    }

    @Test
    @DisplayName("CP-32 · la solicitud válida queda PENDIENTE")
    void solicitudPendiente() throws Exception {
        solicitar("35", "75").andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("CP-33 · sin aprobar, el umbral alto sigue en 40 %")
    void sinAprobar() throws Exception {
        idSolicitud("35", "75");

        vigentes().andExpect(jsonPath("$.alto").value(40));
    }

    @Test
    @DisplayName("CP-33 · el supervisor que solicitó no puede aprobar: la API responde 403 por rol")
    void apruebaElMismoUsuarioPorApi() throws Exception {
        long id = idSolicitud("35", "75");

        aprobar(id, "supervisor").andExpect(status().isForbidden());
        vigentes().andExpect(jsonPath("$.alto").value(40));
    }

    @Test
    @DisplayName("CP-33 · aunque pasara el control de rol, el servicio rechaza al mismo usuario (409)")
    void apruebaElMismoUsuarioEnElServicio() throws Exception {
        long id = idSolicitud("35", "75");

        assertThatThrownBy(() -> servicioUmbrales.aprobar(id, "supervisor"))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("no puede ser quien solicitó");
    }

    @Test
    @DisplayName("CP-33 · con la aprobación del jefe de turno el umbral alto pasa a 35 %")
    void apruebaElJefe() throws Exception {
        long id = idSolicitud("35", "75");

        aprobar(id, "jefe").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("APLICADA"));

        vigentes().andExpect(jsonPath("$.alto").value(35));
    }

    @Test
    @DisplayName("CP-16 · con umbrales 30/65 aprobados, 30 % es ALTO y 65 % es CRITICO de punta a punta")
    void umbralesAjustadosSeUsanAlClasificar() throws Exception {
        aprobar(idSolicitud("30", "65"), "jefe");

        inyectarEstimacion("PROCESO", "30").andExpect(jsonPath("$.nivel").value("ALTO"));
        inyectarEstimacion("FUGA", "65").andExpect(jsonPath("$.nivel").value("CRITICO"));
    }

    @Test
    @DisplayName("CP-40 · aprobar dos veces la misma solicitud → 409")
    void aprobarDosVeces() throws Exception {
        long id = idSolicitud("35", "75");
        aprobar(id, "jefe");

        aprobar(id, "jefe").andExpect(status().isConflict());
    }
}
