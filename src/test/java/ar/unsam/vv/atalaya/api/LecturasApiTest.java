package ar.unsam.vv.atalaya.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.unsam.vv.atalaya.PruebaApiQa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * RF-01/02 · API de lecturas con MockMvc (caja negra). Valores límite de temperatura de punta a
 * punta (CP-01) y partición de equivalencia de mensajes inválidos (CP-03).
 */
class LecturasApiTest extends PruebaApiQa {

    @ParameterizedTest(name = "CP-01 · POST temperatura {0} °C → {1}")
    @CsvSource({
            "19.9,  DESCARTADA_RANGO",
            "20,    ACEPTADA",
            "350,   ACEPTADA",
            "350.1, DESCARTADA_RANGO"
    })
    void limitesDeTemperatura(String valor, String esperado) throws Exception {
        enviarLectura("TEMPERATURA_PRIMARIO", "A", valor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(esperado));
    }

    @ParameterizedTest(name = "CP-03 · mensaje inválido ({0}) → 400 y la magnitud no cambia")
    @ValueSource(strings = {
            """
            {"magnitud": "TEMPERATURA_PRIMARIO", "canal": "A", "valor": null, "secuencia": 1, "timestampOrigen": "2026-01-01T00:00:00Z"}""",
            """
            {"magnitud": "TEMPERATURA_PRIMARIO", "canal": "A", "valor": "abc", "secuencia": 1, "timestampOrigen": "2026-01-01T00:00:00Z"}""",
            """
            {"magnitud": "CAUDAL", "canal": "A", "valor": 300, "secuencia": 1, "timestampOrigen": "2026-01-01T00:00:00Z"}""",
            """
            {"magnitud": "TEMPERATURA_PRIMARIO", "canal": "D", "valor": 300, "secuencia": 1, "timestampOrigen": "2026-01-01T00:00:00Z"}""",
            """
            {"magnitud": "TEMPERATURA_PRIMARIO", "canal": "A", "valor": 300, """
    })
    void mensajesInvalidos(String json) throws Exception {
        enviarJson(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").exists());

        consultarMagnitud("TEMPERATURA_PRIMARIO")
                .andExpect(jsonPath("$.canales[0].motivo").value("SIN_ACTUALIZACION"))
                .andExpect(jsonPath("$.utilizable").value(false));
    }

    @Test
    @DisplayName("CP-03 · el error indica el campo inválido")
    void errorIndicaElCampo() throws Exception {
        enviarJson("""
                {"magnitud": "TEMPERATURA_PRIMARIO", "canal": "D", "valor": 300, "secuencia": 1,
                 "timestampOrigen": "2026-01-01T00:00:00Z"}""")
                .andExpect(jsonPath("$.campo").value("canal"));
    }

    @Test
    @DisplayName("RF-03 · GET de una magnitud inexistente devuelve 404")
    void magnitudInexistente() throws Exception {
        consultarMagnitud("caudal").andExpect(status().isNotFound());
    }
}
