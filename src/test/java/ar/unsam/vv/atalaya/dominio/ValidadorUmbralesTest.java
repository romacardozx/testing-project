package ar.unsam.vv.atalaya.dominio;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * RF-12 · Valores límite de los umbrales (CP-32, caja negra). Se varía un umbral por vez y el otro
 * queda en su valor por defecto (alto 40, crítico 75).
 */
class ValidadorUmbralesTest {

    private final ValidadorUmbrales validador = new ValidadorUmbrales();

    @ParameterizedTest(name = "CP-32 · alto {0} % (crítico 75) → válido: {1}")
    @CsvSource({"29.9, false", "30, true", "50, true", "50.1, false"})
    void limitesDelAlto(BigDecimal alto, boolean valido) {
        verificar(new Umbrales(alto, new BigDecimal("75")), valido, "alto");
    }

    @ParameterizedTest(name = "CP-32 · crítico {0} % (alto 40) → válido: {1}")
    @CsvSource({"64.9, false", "65, true", "100, true", "100.1, false"})
    void limitesDelCritico(BigDecimal critico, boolean valido) {
        verificar(new Umbrales(new BigDecimal("40"), critico), valido, "critico");
    }

    private void verificar(Umbrales umbrales, boolean valido, String campo) {
        if (valido) {
            assertThatCode(() -> validador.validar(umbrales)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> validador.validar(umbrales))
                    .isInstanceOf(UmbralesInvalidosException.class)
                    .extracting(e -> ((UmbralesInvalidosException) e).campo()).isEqualTo(campo);
        }
    }
}
