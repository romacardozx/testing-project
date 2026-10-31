package ar.unsam.vv.atalaya.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import ar.unsam.vv.atalaya.PruebaApiQa;
import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * RNF seguridad · Autenticación (CP-34, partición de equivalencia) y matriz de permisos de la
 * sección 7.6 del informe (CP-35, tabla de decisión: 8 operaciones × 5 roles). Una celda "Sí" espera
 * 2xx; una celda "No", 403. Cada operación prepara antes lo que necesita (alerta, maniobra o solicitud).
 */
class PermisosApiTest extends PruebaApiQa {

    private static final String[] ROLES = {"adquisicion", "operador", "jefe", "supervisor", "auditor"};

    /** Matriz 7.6: operación → usuarios que pueden ejecutarla. */
    private static final Map<String, String> PERMITIDOS = Map.of(
            "Enviar lecturas", "adquisicion",
            "Consultar magnitudes", "operador jefe supervisor auditor",
            "Consultar estimaciones", "operador jefe supervisor auditor",
            "Consultar alertas", "operador jefe supervisor auditor",
            "Reconocer alertas", "operador jefe supervisor",
            "Declarar maniobra", "jefe",
            "Finalizar maniobra", "jefe",
            "Solicitar cambio de umbral", "supervisor",
            "Aprobar cambio de umbral", "jefe");

    static Stream<Arguments> matriz() {
        return PERMITIDOS.keySet().stream().sorted().flatMap(operacion -> Arrays.stream(ROLES)
                .map(rol -> Arguments.of(operacion, rol,
                        Arrays.asList(PERMITIDOS.get(operacion).split(" ")).contains(rol))));
    }

    @ParameterizedTest(name = "CP-35 · {0} como {1} → permitido: {2}")
    @MethodSource("matriz")
    void matrizDePermisos(String operacion, String usuario, boolean permitido) throws Exception {
        int estado = mvc.perform(preparar(operacion).with(como(usuario))).andReturn().getResponse().getStatus();

        if (permitido) {
            assertThat(estado).as("HTTP de una celda permitida").isBetween(200, 299);
        } else {
            assertThat(estado).as("HTTP de una celda denegada").isEqualTo(403);
        }
    }

    @ParameterizedTest(name = "CP-34 · {0} {1} sin credenciales → 401")
    @CsvSource({
            "POST,   /api/v1/lecturas",
            "GET,    /api/v1/magnitudes/TEMPERATURA_PRIMARIO",
            "GET,    /api/v1/estimaciones/actual",
            "GET,    /api/v1/alertas",
            "POST,   /api/v1/alertas/1/reconocimiento",
            "POST,   /api/v1/maniobras",
            "DELETE, /api/v1/maniobras/1",
            "GET,    /api/v1/config/umbrales",
            "PUT,    /api/v1/config/umbrales",
            "POST,   /api/v1/config/umbrales/1/aprobacion",
            "GET,    /api/v1/version"
    })
    void sinCredenciales(String metodo, String ruta) throws Exception {
        int estado = mvc.perform(request(HttpMethod.valueOf(metodo), ruta)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();

        assertThat(estado).isEqualTo(401);
    }

    /** Arma el pedido de cada operación con datos válidos, creando antes lo que haga falta. */
    private MockHttpServletRequestBuilder preparar(String operacion) throws Exception {
        return switch (operacion) {
            case "Enviar lecturas" -> json(HttpMethod.POST, "/api/v1/lecturas", """
                    {"magnitud": "TEMPERATURA_PRIMARIO", "canal": "A", "valor": 300, "secuencia": 1,
                     "timestampOrigen": "2026-01-01T00:00:00Z"}""");
            case "Consultar magnitudes" -> request(HttpMethod.GET, "/api/v1/magnitudes/TEMPERATURA_PRIMARIO");
            case "Consultar estimaciones" -> request(HttpMethod.GET, "/api/v1/estimaciones/actual");
            case "Consultar alertas" -> request(HttpMethod.GET, "/api/v1/alertas");
            case "Reconocer alertas" ->
                    request(HttpMethod.POST, "/api/v1/alertas/" + alertaDe("PROCESO", "50") + "/reconocimiento");
            case "Declarar maniobra" -> json(HttpMethod.POST, "/api/v1/maniobras",
                    "{\"magnitud\": \"POTENCIA_TERMICA\", \"bandaMin\": 70, \"bandaMax\": 90}");
            case "Finalizar maniobra" -> request(HttpMethod.DELETE, "/api/v1/maniobras/" + idManiobra("70", "90", 30));
            case "Solicitar cambio de umbral" -> json(HttpMethod.PUT, "/api/v1/config/umbrales",
                    "{\"alto\": 35, \"critico\": 75}");
            case "Aprobar cambio de umbral" ->
                    request(HttpMethod.POST, "/api/v1/config/umbrales/" + solicitudPendiente() + "/aprobacion");
            default -> throw new IllegalArgumentException(operacion);
        };
    }

    private static MockHttpServletRequestBuilder json(HttpMethod metodo, String ruta, String cuerpo) {
        return request(metodo, ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private long solicitudPendiente() throws Exception {
        String cuerpo = mvc.perform(json(HttpMethod.PUT, "/api/v1/config/umbrales", "{\"alto\": 35, \"critico\": 75}")
                        .with(como("supervisor")))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }
}
