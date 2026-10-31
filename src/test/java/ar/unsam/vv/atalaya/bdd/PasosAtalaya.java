package ar.unsam.vv.atalaya.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import ar.unsam.vv.atalaya.aplicacion.ServicioReset;
import ar.unsam.vv.atalaya.config.RelojMutable;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Steps de las cuatro features (CP-07, CP-21/23, CP-27, CP-33). Cucumber crea una instancia por
 * escenario, así que el estado (alerta, solicitud, última respuesta) no pasa de un escenario a otro.
 * Los números de los .feature van con coma decimal ({bigdecimal} usa el idioma de la feature).
 */
public class PasosAtalaya {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RelojMutable reloj;

    @Autowired
    private ServicioReset servicioReset;

    private long secuencia = 1000;
    private long alertaId;
    private long solicitudId;
    private int ultimoEstado;

    @Before
    public void precondicionBase() {
        servicioReset.limpiar();
        reloj.reiniciar();
    }

    // ---------- comunes ----------

    @Dado("que el sistema está en la precondición base")
    public void sistemaEnBase() {
        // Lo hace @Before: H2 limpia, reloj en T0, sin maniobra, umbrales 40/75.
    }

    // ---------- redundancia (CP-07, CP-08) ----------

    @Dado("que los canales A y B de {word} informan {bigdecimal}")
    public void canalesAyBInforman(String magnitud, BigDecimal valor) throws Exception {
        lectura(magnitud, "A", valor);
        lectura(magnitud, "B", valor);
    }

    @Cuando("el canal {word} de {word} informa {bigdecimal}")
    public void canalInforma(String canal, String magnitud, BigDecimal valor) throws Exception {
        lectura(magnitud, canal, valor);
    }

    @Entonces("el canal {word} de {word} queda {word} por {word}")
    public void canalQuedaPor(String canal, String magnitud, String estado, String motivo) throws Exception {
        String cuerpo = magnitud(magnitud);
        assertThat(JsonPath.<List<String>>read(cuerpo, "$.canales[?(@.canal == '" + canal + "')].estado"))
                .containsExactly(estado);
        assertThat(JsonPath.<List<String>>read(cuerpo, "$.canales[?(@.canal == '" + canal + "')].motivo"))
                .containsExactly(motivo);
    }

    @Entonces("el canal {word} de {word} queda {word}")
    public void canalQueda(String canal, String magnitud, String estado) throws Exception {
        assertThat(JsonPath.<List<String>>read(magnitud(magnitud), "$.canales[?(@.canal == '" + canal + "')].estado"))
                .containsExactly(estado);
    }

    @Entonces("el valor consolidado de {word} es {bigdecimal}")
    public void valorConsolidado(String magnitud, BigDecimal esperado) throws Exception {
        Number valor = JsonPath.read(magnitud(magnitud), "$.valorConsolidado");
        assertThat(new BigDecimal(valor.toString())).isEqualByComparingTo(esperado);
    }

    @Entonces("{word} avisa {string}")
    public void avisa(String magnitud, String aviso) throws Exception {
        assertThat(JsonPath.<List<String>>read(magnitud(magnitud), "$.avisos")).contains(aviso);
    }

    @Entonces("{word} no tiene avisos")
    public void sinAvisos(String magnitud) throws Exception {
        assertThat(JsonPath.<List<String>>read(magnitud(magnitud), "$.avisos")).isEmpty();
    }

    // ---------- escalamiento (CP-21, CP-23) ----------

    @Dado("una alerta crítica emitida al operador de reactor")
    public void alertaCritica() throws Exception {
        alertaId = ((Number) JsonPath.read(estimacion("PROCESO", "80"), "$.alertaId")).longValue();
    }

    @Cuando("pasan {int} segundos sin reconocimiento")
    @Cuando("se llega a los {int} segundos desde la emisión")
    public void pasanSegundos(int segundos) throws Exception {
        long actual = Duration.between(RelojMutable.T0, reloj.instant()).toSeconds();
        respuesta(post("/api/v1/qa/reloj").contentType(MediaType.APPLICATION_JSON)
                .content("{\"segundos\": " + (segundos - actual) + "}"));
    }

    @Cuando("el usuario {string} reconoce la alerta")
    public void reconoce(String usuario) throws Exception {
        ultimoEstado = mvc.perform(post("/api/v1/alertas/" + alertaId + "/reconocimiento").with(como(usuario)))
                .andReturn().getResponse().getStatus();
    }

    @Entonces("el estado de la alerta es {word}")
    public void estadoDeLaAlerta(String estado) throws Exception {
        assertThat(JsonPath.<List<String>>read(alertas(), "$[?(@.id == " + alertaId + ")].estado"))
                .containsExactly(estado);
    }

    @Entonces("la alerta tiene {int} destinatarios")
    public void cantidadDestinatarios(int cantidad) throws Exception {
        List<List<String>> destinatarios = JsonPath.read(alertas(), "$[?(@.id == " + alertaId + ")].destinatarios");
        assertThat(destinatarios.get(0)).hasSize(cantidad);
    }

    // ---------- maniobras (CP-27, CP-28) ----------

    @Dado("que la potencia térmica está en {bigdecimal} en los tres canales")
    public void potenciaEn(BigDecimal valor) throws Exception {
        for (String canal : List.of("A", "B", "C")) {
            lectura("POTENCIA_TERMICA", canal, valor);
        }
    }

    @Dado("el jefe de turno declara una maniobra con banda de potencia de {bigdecimal} a {bigdecimal}")
    public void declaraManiobra(BigDecimal minimo, BigDecimal maximo) throws Exception {
        mvc.perform(post("/api/v1/maniobras").with(como("jefe")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"magnitud\": \"POTENCIA_TERMICA\", \"bandaMin\": " + minimo.toPlainString()
                        + ", \"bandaMax\": " + maximo.toPlainString() + "}"));
    }

    @Cuando("llegan {int} estimaciones de nivel ALTO")
    public void llegan(int cantidad) throws Exception {
        for (int i = 0; i < cantidad; i++) {
            estimacion("PROCESO", "50");
        }
    }

    @Entonces("hay exactamente {int} alerta(s)")
    public void hayAlertas(int cantidad) throws Exception {
        assertThat(JsonPath.<List<Object>>read(alertas(), "$")).hasSize(cantidad);
    }

    @Entonces("es una alerta INFORMATIVA que agrupa {int} estimaciones")
    public void esInformativa(int agrupadas) throws Exception {
        String cuerpo = alertas();
        assertThat(JsonPath.<String>read(cuerpo, "$[0].clase")).isEqualTo("INFORMATIVA");
        assertThat(JsonPath.<Integer>read(cuerpo, "$[0].agrupadas")).isEqualTo(agrupadas);
    }

    @Entonces("es una alerta de RIESGO")
    public void esDeRiesgo() throws Exception {
        assertThat(JsonPath.<String>read(alertas(), "$[0].clase")).isEqualTo("RIESGO");
    }

    // ---------- umbrales (CP-33) ----------

    @Dado("el supervisor de SO solicitó cambiar el umbral alto a {bigdecimal}")
    public void solicitoUmbral(BigDecimal alto) throws Exception {
        String cuerpo = mvc.perform(put("/api/v1/config/umbrales").with(como("supervisor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alto\": " + alto.toPlainString() + ", \"critico\": 75}"))
                .andReturn().getResponse().getContentAsString();
        solicitudId = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    @Cuando("el usuario {string} intenta aprobar la solicitud")
    public void intentaAprobar(String usuario) throws Exception {
        ultimoEstado = mvc.perform(post("/api/v1/config/umbrales/" + solicitudId + "/aprobacion").with(como(usuario)))
                .andReturn().getResponse().getStatus();
    }

    @Entonces("la operación se rechaza con HTTP {int}")
    public void rechazada(int estado) {
        assertThat(ultimoEstado).isEqualTo(estado);
    }

    @Entonces("la operación se acepta")
    public void aceptada() {
        assertThat(ultimoEstado).isBetween(200, 299);
    }

    @Entonces("el umbral alto vigente es {bigdecimal}")
    public void umbralVigente(BigDecimal esperado) throws Exception {
        String cuerpo = mvc.perform(get("/api/v1/config/umbrales").with(como("operador")))
                .andReturn().getResponse().getContentAsString();
        Number alto = JsonPath.read(cuerpo, "$.alto");
        assertThat(new BigDecimal(alto.toString())).isEqualByComparingTo(esperado);
    }

    // ---------- apoyo ----------

    private void lectura(String magnitud, String canal, BigDecimal valor) throws Exception {
        respuesta(post("/api/v1/lecturas").with(como("adquisicion")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"magnitud\": \"%s\", \"canal\": \"%s\", \"valor\": %s, \"secuencia\": %d, \"timestampOrigen\": \"2026-01-01T00:00:00Z\"}"
                        .formatted(magnitud, canal, valor.toPlainString(), ++secuencia)));
    }

    private String estimacion(String tipo, String probabilidad) throws Exception {
        return respuesta(post("/api/v1/qa/estimaciones").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\": \"" + tipo + "\", \"probabilidad\": " + probabilidad + "}"));
    }

    private String magnitud(String magnitud) throws Exception {
        return respuesta(get("/api/v1/magnitudes/" + magnitud).with(como("operador")));
    }

    private String alertas() throws Exception {
        return respuesta(get("/api/v1/alertas").with(como("operador")));
    }

    private String respuesta(MockHttpServletRequestBuilder pedido) throws Exception {
        var resultado = mvc.perform(pedido).andReturn().getResponse();
        assertThat(resultado.getStatus()).as("HTTP de %s", pedido).isBetween(200, 299);
        return resultado.getContentAsString();
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor como(String usuario) {
        return httpBasic(usuario, usuario + "123");
    }
}
