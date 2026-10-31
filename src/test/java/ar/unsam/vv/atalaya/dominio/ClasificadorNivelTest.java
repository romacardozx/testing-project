package ar.unsam.vv.atalaya.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * RF-06 · Clasificación de nivel de riesgo. Valores límite con umbrales por defecto (CP-13) y
 * ajustados (CP-16), partición de equivalencia (CP-14, CP-15) y caminos básicos de la caja blanca
 * (CP-17, V(G) = 5, ver sección 7.3 del informe).
 */
class ClasificadorNivelTest {

    private final ClasificadorNivel clasificador = new ClasificadorNivel();

    @ParameterizedTest(name = "CP-13 · {0} % con umbrales 40/75 → {1}")
    @CsvSource({
            "0, NORMAL", "14.9, NORMAL", "15, ATENCION", "39.9, ATENCION",
            "40, ALTO", "74.9, ALTO", "75, CRITICO", "100, CRITICO"
    })
    void bordesDeNivel(BigDecimal probabilidad, NivelRiesgo esperado) {
        assertThat(clasificador.clasificar(probabilidad, Umbrales.POR_DEFECTO)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "CP-14 · {0} % se rechaza")
    @ValueSource(strings = {"-0.1", "100.1"})
    void probabilidadFueraDeRango(BigDecimal probabilidad) {
        assertThatThrownBy(() -> clasificador.clasificar(probabilidad, Umbrales.POR_DEFECTO))
                .isInstanceOf(ProbabilidadInvalidaException.class);
    }

    @Test
    @DisplayName("CP-14 · probabilidad nula se rechaza")
    void probabilidadNula() {
        assertThatThrownBy(() -> clasificador.clasificar(null, Umbrales.POR_DEFECTO))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest(name = "CP-16 · {0} % con umbrales 30/65 → {1}")
    @CsvSource({"29.9, ATENCION", "30, ALTO", "64.9, ALTO", "65, CRITICO"})
    void umbralesAjustados(BigDecimal probabilidad, NivelRiesgo esperado) {
        var umbrales = new Umbrales(new BigDecimal("30"), new BigDecimal("65"));

        assertThat(clasificador.clasificar(probabilidad, umbrales)).isEqualTo(esperado);
    }

    /** CP-17: un dato por camino básico, más 100,1 para la segunda condición de D1. */
    @Nested
    class CaminosBasicos {

        @ParameterizedTest(name = "CP-17 · camino {0}: p = {1} → {2}")
        @CsvSource({
                "C2 (D2 verdadera), 80, CRITICO",
                "C3 (D3 verdadera), 50, ALTO",
                "C4 (D4 verdadera), 20, ATENCION",
                "C5 (todas falsas), 5,  NORMAL"
        })
        void caminosQueClasifican(String camino, BigDecimal p, NivelRiesgo esperado) {
            assertThat(clasificador.clasificar(p, Umbrales.POR_DEFECTO)).isEqualTo(esperado);
        }

        @ParameterizedTest(name = "CP-17 · camino C1 (D1 verdadera): p = {0} → excepción")
        @ValueSource(strings = {"-1", "100.1"})
        void caminoQueRechaza(BigDecimal p) {
            assertThatThrownBy(() -> clasificador.clasificar(p, Umbrales.POR_DEFECTO))
                    .isInstanceOf(ProbabilidadInvalidaException.class);
        }
    }

    @Nested
    class Factores {

        private final List<Factor> cinco = List.of(
                new Factor("caudal", new BigDecimal("0.10")),
                new Factor("presion", new BigDecimal("0.30")),
                new Factor("temperatura", new BigDecimal("0.25")),
                new Factor("vibracion", new BigDecimal("0.05")),
                new Factor("nivel", new BigDecimal("0.20")));

        @Test
        @DisplayName("CP-15 · con 15 % se devuelven los 3 factores de mayor peso, ordenados")
        void tresFactoresDesdeAtencion() {
            var nivel = clasificador.clasificar(new BigDecimal("15"), Umbrales.POR_DEFECTO);

            assertThat(clasificador.factoresPrincipales(nivel, cinco))
                    .extracting(Factor::nombre)
                    .containsExactly("presion", "temperatura", "nivel");
        }

        @Test
        @DisplayName("CP-15 · con 14,9 % (NORMAL) no se devuelven factores")
        void sinFactoresEnNormal() {
            var nivel = clasificador.clasificar(new BigDecimal("14.9"), Umbrales.POR_DEFECTO);

            assertThat(clasificador.factoresPrincipales(nivel, cinco)).isEmpty();
        }

        @Test
        @DisplayName("CP-15 · con menos de 3 factores se devuelven todos, ordenados")
        void menosDeTresFactores() {
            var dos = List.of(new Factor("a", new BigDecimal("0.1")), new Factor("b", new BigDecimal("0.9")));

            assertThat(clasificador.factoresPrincipales(NivelRiesgo.CRITICO, dos))
                    .extracting(Factor::nombre)
                    .containsExactly("b", "a");
        }
    }
}
