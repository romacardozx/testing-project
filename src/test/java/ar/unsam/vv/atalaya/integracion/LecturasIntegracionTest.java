package ar.unsam.vv.atalaya.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import ar.unsam.vv.atalaya.PruebaApiQa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * RF-01/02 y RF-03 · Integración de punta a punta con H2. Los casos de caja gris revisan además lo
 * que queda guardado en la tabla de lecturas.
 */
class LecturasIntegracionTest extends PruebaApiQa {

    private static final String TEMP = "TEMPERATURA_PRIMARIO";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("CP-04 · la lectura duplicada vuelve como DUPLICADA y queda un solo registro en H2")
    void lecturaDuplicada() throws Exception {
        enviarLectura(TEMP, "A", "300", 1001).andExpect(jsonPath("$.estado").value("ACEPTADA"));
        enviarLectura(TEMP, "A", "300", 1001).andExpect(jsonPath("$.estado").value("DUPLICADA"));

        Integer registros = jdbc.queryForObject(
                "select count(*) from lectura where magnitud = ? and canal = 'A' and secuencia = 1001",
                Integer.class, TEMP);
        assertThat(registros).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-05 · la lectura fuera de orden no pisa el valor vigente")
    void lecturaFueraDeOrden() throws Exception {
        enviarLectura(TEMP, "A", "300", 1002);
        enviarLectura(TEMP, "B", "300", 1002);
        enviarLectura(TEMP, "C", "302", 1002);

        enviarLectura(TEMP, "C", "301", 1001).andExpect(jsonPath("$.estado").value("FUERA_DE_ORDEN"));

        consultarMagnitud(TEMP).andExpect(jsonPath("$.valorConsolidado").value(300.67));
    }

    @Test
    @DisplayName("CP-07 · C a 306,64 °C queda NO_CONFIABLE por DISCREPANCIA y el valor usa A y B")
    void canalQueDiscrepa() throws Exception {
        enviarLectura(TEMP, "A", "300.00");
        enviarLectura(TEMP, "B", "300.00");
        enviarLectura(TEMP, "C", "306.64");

        consultarMagnitud(TEMP)
                .andExpect(jsonPath("$.canales[2].estado").value("NO_CONFIABLE"))
                .andExpect(jsonPath("$.canales[2].motivo").value("DISCREPANCIA"))
                .andExpect(jsonPath("$.valorConsolidado").value(300.0))
                .andExpect(jsonPath("$.avisos", contains("CANAL_EXCLUIDO:C")));
    }

    @Test
    @DisplayName("CP-11 · con B y C sin actualizar más de 10 s la magnitud no es utilizable")
    void menosDeDosConfiables() throws Exception {
        enviarLectura(TEMP, "A", "300");
        enviarLectura(TEMP, "B", "300");
        enviarLectura(TEMP, "C", "300");
        for (int i = 0; i < 11; i++) {
            avanzarSegundos(1);
            enviarLectura(TEMP, "A", "300");
        }

        consultarMagnitud(TEMP)
                .andExpect(jsonPath("$.canales[0].estado").value("CONFIABLE"))
                .andExpect(jsonPath("$.utilizable").value(false))
                .andExpect(jsonPath("$.avisos",
                        containsInAnyOrder("CANAL_EXCLUIDO:B", "CANAL_EXCLUIDO:C", "MAGNITUD_NO_DISPONIBLE")));
    }

    @Test
    @DisplayName("CP-12 · C se recupera con la tercera lectura válida seguida")
    void recuperacionDeUnCanal() throws Exception {
        enviarLectura(TEMP, "A", "300");
        enviarLectura(TEMP, "B", "300");
        enviarLectura(TEMP, "C", "306.64");

        enviarLectura(TEMP, "C", "300");
        enviarLectura(TEMP, "C", "300");
        consultarMagnitud(TEMP).andExpect(jsonPath("$.canales[2].estado").value("NO_CONFIABLE"));

        enviarLectura(TEMP, "C", "300");
        consultarMagnitud(TEMP).andExpect(jsonPath("$.canales[2].estado").value("CONFIABLE"));
    }

    @Test
    @DisplayName("CP-10 · una lectura de 351 °C en C la marca FUERA_DE_RANGO sin cambiar el valor")
    void lecturaFueraDeRangoMarcaElCanal() throws Exception {
        enviarLectura(TEMP, "A", "300");
        enviarLectura(TEMP, "B", "300");
        enviarLectura(TEMP, "C", "351").andExpect(jsonPath("$.estado").value("DESCARTADA_RANGO"));

        consultarMagnitud(TEMP)
                .andExpect(jsonPath("$.canales[2].motivo").value("FUERA_DE_RANGO"))
                .andExpect(jsonPath("$.valorConsolidado").value(300.0));
    }

    @Test
    @DisplayName("CP-09 · un canal que vuelve después de 10 s sin actualizar necesita 3 lecturas para recuperarse")
    void vuelveDespuesDeSinActualizar() throws Exception {
        enviarLectura(TEMP, "A", "300");
        enviarLectura(TEMP, "B", "300");
        enviarLectura(TEMP, "C", "300");
        avanzarSegundos(10.1);
        enviarLectura(TEMP, "A", "300");
        enviarLectura(TEMP, "B", "300");
        enviarLectura(TEMP, "C", "300");

        consultarMagnitud(TEMP)
                .andExpect(jsonPath("$.canales[*].estado", contains("NO_CONFIABLE", "NO_CONFIABLE", "NO_CONFIABLE")))
                .andExpect(jsonPath("$.canales[0].motivo").value("SIN_ACTUALIZACION"));
    }
}
