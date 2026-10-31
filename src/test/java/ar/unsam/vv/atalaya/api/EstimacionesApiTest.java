package ar.unsam.vv.atalaya.api;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.unsam.vv.atalaya.PruebaApiQa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

/**
 * RF-06 · Estimaciones por la API con MockMvc (caja negra). Bordes de nivel de punta a punta
 * (CP-13), probabilidades inválidas (CP-14) y factores principales (CP-15).
 */
class EstimacionesApiTest extends PruebaApiQa {

    private ResultActions consultarActual() throws Exception {
        return mvc.perform(get("/api/v1/estimaciones/actual").with(como("operador")));
    }

    @ParameterizedTest(name = "CP-13 · inyectar {0} % → nivel {1}")
    @CsvSource({
            "0, NORMAL", "14.9, NORMAL", "15, ATENCION", "39.9, ATENCION",
            "40, ALTO", "74.9, ALTO", "75, CRITICO", "100, CRITICO"
    })
    void bordesDeNivel(String probabilidad, String nivel) throws Exception {
        inyectarEstimacion("PROCESO", probabilidad).andExpect(status().isOk());

        consultarActual()
                .andExpect(jsonPath("$[0].tipo").value("PROCESO"))
                .andExpect(jsonPath("$[0].nivel").value(nivel));
    }

    @ParameterizedTest(name = "CP-14 · probabilidad {0} → 400 y el nivel vigente no cambia")
    @ValueSource(strings = {"-0.1", "100.1", "null"})
    void probabilidadInvalida(String probabilidad) throws Exception {
        inyectarEstimacion("PROCESO", "50");

        inyectarEstimacion("PROCESO", probabilidad)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("probabilidad"));

        consultarActual()
                .andExpect(jsonPath("$[0].probabilidad").value(50))
                .andExpect(jsonPath("$[0].nivel").value("ALTO"));
    }

    @Test
    @DisplayName("CP-15 · con 15 % y 5 factores se informan los 3 de mayor peso, ordenados")
    void factoresPrincipales() throws Exception {
        inyectarJson("""
                {"tipo": "FUGA", "probabilidad": 15, "factores": [
                  {"nombre": "caudal", "peso": 0.10}, {"nombre": "presion", "peso": 0.30},
                  {"nombre": "temperatura", "peso": 0.25}, {"nombre": "vibracion", "peso": 0.05},
                  {"nombre": "nivel", "peso": 0.20}]}""");

        consultarActual()
                .andExpect(jsonPath("$[0].factores[*].nombre", contains("presion", "temperatura", "nivel")));
    }

    @Test
    @DisplayName("CP-34 · GET /estimaciones/actual sin credenciales → 401")
    void sinCredenciales() throws Exception {
        mvc.perform(get("/api/v1/estimaciones/actual")).andExpect(status().isUnauthorized());
    }
}
