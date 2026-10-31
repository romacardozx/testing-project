package ar.unsam.vv.atalaya.dominio;

import static ar.unsam.vv.atalaya.dominio.NivelRiesgo.ALTO;
import static ar.unsam.vv.atalaya.dominio.NivelRiesgo.ATENCION;
import static ar.unsam.vv.atalaya.dominio.NivelRiesgo.CRITICO;
import static ar.unsam.vv.atalaya.dominio.NivelRiesgo.NORMAL;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * RF-07 · Generación y repetición de alertas. Partición por nivel de la tabla de destinatarios
 * (CP-18), valores límite de la ventana de 3 min (CP-19) y transición "empeora" (CP-20, A4).
 */
class AlertasTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Nested
    class Definicion {

        @Test
        @DisplayName("CP-18 · NORMAL no genera alerta")
        void normal() {
            assertThat(DefinicionAlerta.para(NORMAL)).isEmpty();
        }

        @Test
        @DisplayName("CP-18 · ATENCION: solo visual, sin destinatarios ni reconocimiento")
        void atencion() {
            assertThat(DefinicionAlerta.para(ATENCION)).contains(
                    new DefinicionAlerta(List.of(CanalAlerta.VISUAL), List.of(), false));
        }

        @Test
        @DisplayName("CP-18 · ALTO: visual al operador, requiere reconocimiento")
        void alto() {
            assertThat(DefinicionAlerta.para(ALTO)).contains(
                    new DefinicionAlerta(List.of(CanalAlerta.VISUAL), List.of(Rol.OPERADOR_REACTOR), true));
        }

        @Test
        @DisplayName("CP-18 · CRITICO: visual y sonora al operador y al jefe de turno")
        void critico() {
            assertThat(DefinicionAlerta.para(CRITICO)).contains(new DefinicionAlerta(
                    List.of(CanalAlerta.VISUAL, CanalAlerta.SONORA),
                    List.of(Rol.OPERADOR_REACTOR, Rol.JEFE_TURNO), true));
        }
    }

    @Nested
    class Repeticion {

        private final PoliticaRepeticion politica = new PoliticaRepeticion();
        private final Optional<UltimaAlerta> altoEnT0 = Optional.of(new UltimaAlerta(ALTO, T0));

        @Test
        @DisplayName("CP-19 · la primera alerta de un tipo se emite")
        void primeraAlerta() {
            assertThat(politica.debeEmitir(Optional.empty(), ALTO, T0)).isTrue();
        }

        @Test
        @DisplayName("CP-19 · mismo nivel a los 179 s no se repite; a los 180 s sí")
        void ventanaDeTresMinutos() {
            assertThat(politica.debeEmitir(altoEnT0, ALTO, T0.plusSeconds(179))).isFalse();
            assertThat(politica.debeEmitir(altoEnT0, ALTO, T0.plusSeconds(180))).isTrue();
        }

        @Test
        @DisplayName("CP-20 · si sube de nivel dentro de los 3 min se emite igual")
        void empeora() {
            assertThat(politica.debeEmitir(altoEnT0, CRITICO, T0.plusSeconds(60))).isTrue();
        }

        @Test
        @DisplayName("CP-19 · si baja de nivel dentro de los 3 min no se emite")
        void mejoraDentroDeLaVentana() {
            assertThat(politica.debeEmitir(altoEnT0, ATENCION, T0.plusSeconds(60))).isFalse();
        }

        @Test
        @DisplayName("CP-18 · NORMAL nunca genera alerta, aunque no haya una previa")
        void normalNuncaSeEmite() {
            assertThat(politica.debeEmitir(Optional.empty(), NORMAL, T0)).isFalse();
        }
    }
}
