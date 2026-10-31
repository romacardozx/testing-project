package ar.unsam.vv.atalaya.dominio;

import static ar.unsam.vv.atalaya.dominio.EstadoAlerta.EMITIDA;
import static ar.unsam.vv.atalaya.dominio.EstadoAlerta.ESCALADA;
import static ar.unsam.vv.atalaya.dominio.EstadoAlerta.RECONOCIDA;
import static ar.unsam.vv.atalaya.dominio.NivelRiesgo.ALTO;
import static ar.unsam.vv.atalaya.dominio.NivelRiesgo.ATENCION;
import static ar.unsam.vv.atalaya.dominio.NivelRiesgo.CRITICO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/**
 * RF-08 · Escalamiento de alertas. Pruebas escritas antes del código (TDD). Valores límite de
 * tiempo (CP-21, CP-22), transición de estados (CP-23, CP-24) y cobertura de decisiones de la caja
 * blanca (CP-26: nivel × reconocida × antes/en/después del plazo). Los tiempos son relativos a T0.
 */
class ServicioEscalamientoTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final ServicioEscalamiento servicio = new ServicioEscalamiento();

    @Nested
    class Critico {

        @ParameterizedTest(name = "CP-21 · crítico sin reconocer a los {0} s → escala: {1}")
        @CsvSource({"44, false", "45, true"})
        void bordeDeLos45Segundos(long segundos, boolean escala) {
            var alerta = new SituacionAlerta(CRITICO, EMITIDA, T0);

            assertThat(servicio.evaluar(alerta, T0.plusSeconds(segundos)).isPresent()).isEqualTo(escala);
        }

        @Test
        @DisplayName("CP-21 · el crítico escala al supervisor de seguridad operacional")
        void escalaAlSupervisor() {
            var alerta = new SituacionAlerta(CRITICO, EMITIDA, T0);

            assertThat(servicio.evaluar(alerta, T0.plusSeconds(45))).contains(Rol.SUPERVISOR_SO);
        }

        @Test
        @DisplayName("CP-21 · a los 46 s una alerta ya ESCALADA no vuelve a escalar")
        void noDuplicaElEscalamiento() {
            var alerta = new SituacionAlerta(CRITICO, ESCALADA, T0);

            assertThat(servicio.evaluar(alerta, T0.plusSeconds(46))).isEmpty();
        }
    }

    @Nested
    class Alto {

        @ParameterizedTest(name = "CP-22 · alto sin reconocer a los {0} s → escala: {1}")
        @CsvSource({"89, false", "90, true"})
        void bordeDeLos90Segundos(long segundos, boolean escala) {
            var alerta = new SituacionAlerta(ALTO, EMITIDA, T0);

            assertThat(servicio.evaluar(alerta, T0.plusSeconds(segundos)).isPresent()).isEqualTo(escala);
        }

        @Test
        @DisplayName("CP-22 · el alto escala al jefe de turno")
        void escalaAlJefe() {
            var alerta = new SituacionAlerta(ALTO, EMITIDA, T0);

            assertThat(servicio.evaluar(alerta, T0.plusSeconds(90))).contains(Rol.JEFE_TURNO);
        }

        @Test
        @DisplayName("CP-22 · a los 91 s una alerta alta ya ESCALADA no vuelve a escalar")
        void noDuplicaElEscalamiento() {
            var alerta = new SituacionAlerta(ALTO, ESCALADA, T0);

            assertThat(servicio.evaluar(alerta, T0.plusSeconds(91))).isEmpty();
        }

        @Test
        @DisplayName("CP-26 · un alto a los 45 s no escala (el plazo de 45 s es solo del crítico)")
        void altoNoUsaElPlazoDelCritico() {
            var alerta = new SituacionAlerta(ALTO, EMITIDA, T0);

            assertThat(servicio.evaluar(alerta, T0.plusSeconds(45))).isEmpty();
        }

        @Test
        @DisplayName("CP-26 · ATENCION no escala nunca")
        void atencionNoEscala() {
            var alerta = new SituacionAlerta(ATENCION, EMITIDA, T0);

            assertThat(servicio.evaluar(alerta, T0.plusSeconds(3600))).isEmpty();
        }
    }

    @Nested
    class Reconocimiento {

        @Test
        @DisplayName("CP-23 · una alerta reconocida antes del plazo no escala después")
        void reconocidaNoEscala() {
            var alerta = new SituacionAlerta(CRITICO, RECONOCIDA, T0);

            assertThat(servicio.evaluar(alerta, T0.plusSeconds(60))).isEmpty();
        }

        @ParameterizedTest(name = "CP-23 · reconocer una alerta {0} la deja RECONOCIDA")
        @CsvSource({"EMITIDA", "ESCALADA"})
        void reconocerLaDejaReconocida(EstadoAlerta estado) {
            assertThat(servicio.reconocer(estado)).isEqualTo(RECONOCIDA);
        }

        @Test
        @DisplayName("CP-24 · reconocer una alerta RECONOCIDA se rechaza")
        void reconocerDosVeces() {
            assertThatThrownBy(() -> servicio.reconocer(RECONOCIDA))
                    .isInstanceOf(AlertaYaReconocidaException.class);
        }
    }
}
