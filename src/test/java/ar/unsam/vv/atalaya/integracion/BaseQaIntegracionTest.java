package ar.unsam.vv.atalaya.integracion;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * credenciales y el reloj de QA avanza y se reinicia. No corresponde a un CP del catálogo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("qa")
class BaseQaIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Infra · GET /version con operador devuelve 3.1.0")
    void versionConCredenciales() throws Exception {
        mvc.perform(get("/api/v1/version").with(httpBasic("operador", "operador123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.software").value("3.1.0"));
    }

    @Test
    @DisplayName("Infra · GET /version sin credenciales devuelve 401")
    void versionSinCredenciales() throws Exception {
        mvc.perform(get("/api/v1/version"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Infra · /qa/reloj avanza 10,1 s y /qa/reset vuelve a T0")
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
    @DisplayName("Infra · /qa/reloj con segundos negativos devuelve 400")
    void relojNegativo() throws Exception {
        mvc.perform(post("/api/v1/qa/reloj")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"segundos\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("segundos"));
    }
}
