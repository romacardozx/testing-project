package ar.unsam.vv.atalaya.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.unsam.vv.atalaya.PruebaApiQa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RF-08 · Reconocimiento por la API con MockMvc (caja negra): segundo reconocimiento (CP-24),
 * alerta inexistente y rol sin permiso.
 */
class AlertasApiTest extends PruebaApiQa {

    @Test
    @DisplayName("CP-24 · reconocer una alerta RECONOCIDA devuelve 409 y el estado no cambia")
    void reconocerDosVeces() throws Exception {
        long id = alertaDe("PROCESO", "50");
        reconocer(id, "operador").andExpect(status().isOk());

        reconocer(id, "jefe")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ALERTA_YA_RECONOCIDA"));

        consultarAlertas().andExpect(jsonPath("$[0].estado").value("RECONOCIDA"));
    }

    @Test
    @DisplayName("RF-08 · reconocer una alerta inexistente devuelve 404")
    void alertaInexistente() throws Exception {
        reconocer(999_999, "operador").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CP-35 · el auditor no puede reconocer alertas (403)")
    void auditorNoReconoce() throws Exception {
        long id = alertaDe("PROCESO", "50");

        reconocer(id, "auditor").andExpect(status().isForbidden());
    }
}
