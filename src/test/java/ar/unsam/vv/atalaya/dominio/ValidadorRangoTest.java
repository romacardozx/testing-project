package ar.unsam.vv.atalaya.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * RF-01/02 · Valores límite del rango físico (caja negra). Cada magnitud se prueba en los dos
 * extremos y a una décima por fuera de cada uno: los extremos son inclusivos.
 */
class ValidadorRangoTest {

    private final ValidadorRango validador = new ValidadorRango();

    @ParameterizedTest(name = "CP-01 · temperatura {0} °C → dentro de rango: {1}")
    @CsvSource({"19.9, false", "20, true", "350, true", "350.1, false"})
    void limitesDeTemperatura(BigDecimal valor, boolean esperado) {
        assertThat(validador.dentroDeRango(Magnitud.TEMPERATURA_PRIMARIO, valor)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "CP-02 · {0} = {1} → dentro de rango: {2}")
    @CsvSource({
            "PRESION_PRIMARIO,   -0.1,    false",
            "PRESION_PRIMARIO,   0,       true",
            "PRESION_PRIMARIO,   180,     true",
            "PRESION_PRIMARIO,   180.1,   false",
            "NIVEL_PRESURIZADOR, -0.1,    false",
            "NIVEL_PRESURIZADOR, 0,       true",
            "NIVEL_PRESURIZADOR, 100,     true",
            "NIVEL_PRESURIZADOR, 100.1,   false",
            "POTENCIA_TERMICA,   -0.1,    false",
            "POTENCIA_TERMICA,   0,       true",
            "POTENCIA_TERMICA,   120,     true",
            "POTENCIA_TERMICA,   120.1,   false",
            "TASA_DOSIS,         -0.1,    false",
            "TASA_DOSIS,         0,       true",
            "TASA_DOSIS,         10000,   true",
            "TASA_DOSIS,         10000.1, false"
    })
    void limitesDeLasOtrasMagnitudes(Magnitud magnitud, BigDecimal valor, boolean esperado) {
        assertThat(validador.dentroDeRango(magnitud, valor)).isEqualTo(esperado);
    }
}
