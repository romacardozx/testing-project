package ar.unsam.vv.atalaya.integracion;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba de humo de la estructura base con el perfil qa: el contexto levanta, la API exige
 * credenciales (CP-34), informa la versión (CP-41) y el reloj de QA avanza y se reinicia (INF-02).
 * También la regresión de ATA-17: cada contexto de Spring usa su propia base H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("qa")
class BaseQaIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DataSource baseDeDatos;

    /**
     * ATA-17: con la URL fija jdbc:h2:mem:atalaya, los contextos qa y prod de la misma JVM compartían
     * la base y el tick de prod (reloj real) escalaba las alertas de qa creadas en T0. CP-18 fallaba
     * a veces según el orden de las clases. Ahora el nombre de la base es único por contexto.
     */
    @Test
    @DisplayName("CP-18 · la base H2 es propia de cada contexto, no la compartida jdbc:h2:mem:atalaya [defecto ATA-17, corregido]")
    void baseDeDatosPropiaDelContexto() throws Exception {
        try (Connection conexion = baseDeDatos.getConnection()) {
            String url = conexion.getMetaData().getURL();
            assertThat(url).startsWith("jdbc:h2:mem:").isNotEqualTo("jdbc:h2:mem:atalaya");
        }
    }

    @Test
    @DisplayName("CP-41 · GET /version con operador devuelve 3.1.0")
    void versionConCredenciales() throws Exception {
        mvc.perform(get("/api/v1/version").with(httpBasic("operador", "operador123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.software").value("3.1.0"));
    }

    @Test
    @DisplayName("CP-34 · GET /version sin credenciales devuelve 401")
    void versionSinCredenciales() throws Exception {
        mvc.perform(get("/api/v1/version"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("INF-02 · /qa/reloj avanza 10,1 s y /qa/reset vuelve a T0")
    void relojQa() throws Exception {
        mvc.perform(post("/api/v1/qa/reset")).andExpect(status().isOk());

        mvc.perform(post("/api/v1/qa/reloj")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"segundos\": 10.1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ahora").value("2026-01-01T00:00:10.100Z"));

        mvc.perform(post("/api/v1/qa/reset"))
                .andExpect(jsonPath("$.ahora").value("2026-01-01T00:00:00Z"));
    }

    @Test
    @DisplayName("INF-02 · /qa/reloj con segundos negativos devuelve 400")
    void relojNegativo() throws Exception {
        mvc.perform(post("/api/v1/qa/reloj")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"segundos\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("segundos"));
    }
}
