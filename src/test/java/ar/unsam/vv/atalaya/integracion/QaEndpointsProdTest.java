package ar.unsam.vv.atalaya.integracion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CP-36 · RNF seguridad. Caja gris: con el perfil prod los endpoints de preparación de pruebas no
 * existen. Si alguno respondiera, se podría manipular el reloj o inyectar estimaciones en producción.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class QaEndpointsProdTest {

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest(name = "CP-36 · {0} con perfil prod devuelve 404")
    @ValueSource(strings = {"/api/v1/qa/reloj", "/api/v1/qa/estimaciones", "/api/v1/qa/reset"})
    void endpointsQaNoExistenEnProd(String ruta) throws Exception {
        mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }
}
