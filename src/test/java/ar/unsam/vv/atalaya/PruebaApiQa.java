package ar.unsam.vv.atalaya;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.unsam.vv.atalaya.aplicacion.ServicioReset;
import ar.unsam.vv.atalaya.config.RelojMutable;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Base de las pruebas que levantan el SISTEMA completo con el perfil qa. Antes de cada prueba deja
 * la precondición "Base": H2 limpia y reloj en T0. Los datos de prueba son sintéticos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("qa")
public abstract class PruebaApiQa {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected RelojMutable reloj;

    @Autowired
    private ServicioReset servicioReset;

    private long secuencia = 1000;

    @BeforeEach
    void prepararBase() {
        servicioReset.limpiar();
        reloj.reiniciar();
    }

    /** Avanza el reloj por POST /qa/reloj, que además evalúa el escalamiento. */
    protected void avanzarSegundos(double segundos) throws Exception {
        mvc.perform(post("/api/v1/qa/reloj")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"segundos\": " + BigDecimal.valueOf(segundos).toPlainString() + "}"))
                .andExpect(status().isOk());
    }

    /** Lleva el reloj a T0 + segundos (solo hacia adelante). */
    protected void irASegundo(double segundos) throws Exception {
        double actual = Duration.between(RelojMutable.T0, reloj.instant()).toMillis() / 1000.0;
        avanzarSegundos(segundos - actual);
    }

    /** Inyecta una estimación sin factores por POST /qa/estimaciones. */
    protected ResultActions inyectarEstimacion(String tipo, String probabilidad) throws Exception {
        return inyectarJson("{\"tipo\": \"%s\", \"probabilidad\": %s}".formatted(tipo, probabilidad));
    }

    protected ResultActions inyectarJson(String json) throws Exception {
        return mvc.perform(post("/api/v1/qa/estimaciones").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Inyecta una estimación y devuelve el id de la alerta que generó. */
    protected long alertaDe(String tipo, String probabilidad) throws Exception {
        String cuerpo = inyectarEstimacion(tipo, probabilidad)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.alertaId")).longValue();
    }

    protected ResultActions reconocer(long alertaId, String usuario) throws Exception {
        return mvc.perform(post("/api/v1/alertas/" + alertaId + "/reconocimiento").with(como(usuario)));
    }

    protected ResultActions consultarAlertas() throws Exception {
        return mvc.perform(get("/api/v1/alertas").with(como("operador")));
    }

    /** El jefe de turno declara una maniobra sobre la potencia térmica. */
    protected ResultActions declararManiobra(String bandaMin, String bandaMax, int duracionMin) throws Exception {
        String json = "{\"magnitud\": \"POTENCIA_TERMICA\", \"bandaMin\": %s, \"bandaMax\": %s, \"duracionMaximaMin\": %d}"
                .formatted(bandaMin, bandaMax, duracionMin);
        return mvc.perform(post("/api/v1/maniobras").with(como("jefe"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    protected long idManiobra(String bandaMin, String bandaMax, int duracionMin) throws Exception {
        String cuerpo = declararManiobra(bandaMin, bandaMax, duracionMin)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    /** Envía una lectura de potencia térmica por cada canal A, B y C, en ese orden. */
    protected void enviarPotencia(String a, String b, String c) throws Exception {
        enviarLectura("POTENCIA_TERMICA", "A", a);
        enviarLectura("POTENCIA_TERMICA", "B", b);
        enviarLectura("POTENCIA_TERMICA", "C", c);
    }

    /** Credenciales del perfil qa: la clave es el usuario + "123". */
    protected static RequestPostProcessor como(String usuario) {
        return httpBasic(usuario, usuario + "123");
    }

    /** Envía una lectura como el usuario de adquisición con una secuencia nueva. */
    protected ResultActions enviarLectura(String magnitud, String canal, String valor) throws Exception {
        return enviarLectura(magnitud, canal, valor, ++secuencia);
    }

    protected ResultActions enviarLectura(String magnitud, String canal, String valor, long secuencia)
            throws Exception {
        String json = """
                {"magnitud": "%s", "canal": "%s", "valor": %s, "secuencia": %d,
                 "timestampOrigen": "2026-01-01T00:00:00Z"}""".formatted(magnitud, canal, valor, secuencia);
        return enviarJson(json);
    }

    protected ResultActions enviarJson(String json) throws Exception {
        return mvc.perform(post("/api/v1/lecturas")
                .with(como("adquisicion"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    protected ResultActions consultarMagnitud(String magnitud) throws Exception {
        return mvc.perform(get("/api/v1/magnitudes/" + magnitud).with(como("operador")));
    }
}
