package ar.unsam.vv.atalaya.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifica el reloj de pruebas del que dependen todos los casos con tiempo (CP-09, CP-19 a CP-23,
 * CP-30). Si este reloj falla, esos casos dan resultados no repetibles.
 */
class RelojMutableTest {

    @Test
    @DisplayName("Infra · el reloj arranca en T0")
    void arrancaEnT0() {
        assertThat(new RelojMutable().instant()).isEqualTo(RelojMutable.T0);
    }

    @Test
    @DisplayName("Infra · avanza décimas de segundo y vuelve a T0")
    void avanzaYReinicia() {
        var reloj = new RelojMutable();

        reloj.avanzar(Duration.ofMillis(10_100));
        assertThat(reloj.instant()).isEqualTo(RelojMutable.T0.plusMillis(10_100));

        reloj.reiniciar();
        assertThat(reloj.instant()).isEqualTo(RelojMutable.T0);
    }

    @Test
    @DisplayName("Infra · el reloj no retrocede")
    void noRetrocede() {
        var reloj = new RelojMutable();

        assertThatThrownBy(() -> reloj.avanzar(Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
