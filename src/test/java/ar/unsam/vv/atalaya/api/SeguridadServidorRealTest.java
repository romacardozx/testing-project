package ar.unsam.vv.atalaya.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * CP-34 y CP-35 contra Tomcat real (no MockMvc). Regresión del defecto ATA-16, encontrado con Postman: sin
 * credenciales la API respondía 302 al login, porque el 401 se reenviaba a /error y lo atendía la
 * cadena de seguridad del panel. MockMvc no hace ese reenvío y por eso PermisosApiTest no lo veía.
 * El cliente no sigue redirecciones, para que un 302 quede a la vista.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("qa")
class SeguridadServidorRealTest {

    private final HttpClient cliente = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @LocalServerPort
    private int puerto;

    private HttpResponse<String> enviar(String metodo, String ruta, String credenciales) throws Exception {
        var pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .header("Content-Type", "application/json")
                .method(metodo, HttpRequest.BodyPublishers.ofString("{}"));
        if (credenciales != null) {
            pedido.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes()));
        }
        return cliente.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
    }

    @ParameterizedTest(name = "CP-34 · servidor real: {0} {1} sin credenciales → 401 JSON [defecto ATA-16, corregido]")
    @CsvSource({
            "POST, /api/v1/lecturas",
            "GET,  /api/v1/magnitudes/TEMPERATURA_PRIMARIO",
            "GET,  /api/v1/alertas",
            "PUT,  /api/v1/config/umbrales",
            "GET,  /api/v1/version"
    })
    void sinCredencialesDa401(String metodo, String ruta) throws Exception {
        var respuesta = enviar(metodo, ruta, null);

        assertThat(respuesta.statusCode()).isEqualTo(401);
        assertThat(respuesta.body()).contains("\"codigo\":\"NO_AUTENTICADO\"");
        assertThat(respuesta.headers().firstValue("WWW-Authenticate")).isPresent();
    }

    @Test
    @DisplayName("CP-34 · servidor real: clave incorrecta → 401")
    void claveIncorrecta() throws Exception {
        assertThat(enviar("GET", "/api/v1/alertas", "operador:mal").statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("CP-35 · servidor real: rol sin permiso → 403 JSON (no 302) [defecto ATA-16, corregido]")
    void rolSinPermisoDa403() throws Exception {
        var respuesta = enviar("POST", "/api/v1/lecturas", "operador:operador123");

        assertThat(respuesta.statusCode()).isEqualTo(403);
        assertThat(respuesta.body()).contains("\"codigo\":\"SIN_PERMISO\"");
    }
}
