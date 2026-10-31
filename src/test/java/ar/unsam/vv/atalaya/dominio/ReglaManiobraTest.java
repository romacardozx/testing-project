package ar.unsam.vv.atalaya.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * RF-09 · Maniobras. Valores límite de la banda (CP-28), maniobra con magnitud no utilizable
 * (CP-29) y límite de tiempo de la expiración (CP-30, A6).
 */
class ReglaManiobraTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final BigDecimal MIN = new BigDecimal("70");
    private static final BigDecimal MAX = new BigDecimal("90");

    private final ReglaManiobra regla = new ReglaManiobra();

    private static EstadoMagnitud potencia(String valor) {
        return new EstadoMagnitud(Magnitud.POTENCIA_TERMICA, new BigDecimal(valor), true, List.of(), List.of());
    }

    @ParameterizedTest(name = "CP-28 · banda 70 a 90, potencia {0} % → se agrupa: {1}")
    @CsvSource({"70, true", "69.9, false", "90, true", "90.1, false"})
    void bordesDeLaBanda(String valor, boolean agrupa) {
        assertThat(regla.agrupa(NivelRiesgo.ALTO, MIN, MAX, potencia(valor))).isEqualTo(agrupa);
    }

    @Test
    @DisplayName("CP-29 · si la magnitud de la maniobra no es utilizable, alerta normal")
    void magnitudNoUtilizable() {
        var noDisponible = new EstadoMagnitud(Magnitud.POTENCIA_TERMICA, null, false, List.of(),
                List.of("MAGNITUD_NO_DISPONIBLE"));

        assertThat(regla.agrupa(NivelRiesgo.ALTO, MIN, MAX, noDisponible)).isFalse();
    }

    @Test
    @DisplayName("CP-27 · una estimación NORMAL no se agrupa (no genera alerta)")
    void normalNoSeAgrupa() {
        assertThat(regla.agrupa(NivelRiesgo.NORMAL, MIN, MAX, potencia("80"))).isFalse();
    }

    @Test
    @DisplayName("CP-27 · una estimación de ATENCION dentro de la banda se agrupa")
    void atencionSeAgrupa() {
        assertThat(regla.agrupa(NivelRiesgo.ATENCION, MIN, MAX, potencia("80"))).isTrue();
    }

    @ParameterizedTest(name = "CP-30 · maniobra de 30 min a los {0} s → expiró: {1}")
    @CsvSource({"1799, false", "1800, true"})
    void expiracion(long segundos, boolean expiro) {
        assertThat(regla.expiro(T0, 30, T0.plusSeconds(segundos))).isEqualTo(expiro);
    }
}
