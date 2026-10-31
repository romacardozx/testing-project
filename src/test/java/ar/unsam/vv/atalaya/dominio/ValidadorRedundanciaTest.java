package ar.unsam.vv.atalaya.dominio;

import static ar.unsam.vv.atalaya.dominio.Magnitud.TEMPERATURA_PRIMARIO;
import static ar.unsam.vv.atalaya.dominio.MotivoNoConfiable.DISCREPANCIA;
import static ar.unsam.vv.atalaya.dominio.MotivoNoConfiable.FUERA_DE_RANGO;
import static ar.unsam.vv.atalaya.dominio.MotivoNoConfiable.SIN_ACTUALIZACION;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * RF-03 · Redundancia de tres canales. Pruebas unitarias escritas antes del código (TDD) a partir
 * de la tabla de decisión 7.1 del informe y de las decisiones A1, A2, A3 y A17. Todos los tiempos
 * son relativos a T0 y se pasan explícitos: no hay reloj real.
 */
class ValidadorRedundanciaTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final ValidadorRedundancia validador = new ValidadorRedundancia();

    private static SenalCanal senal(Canal canal, String valor) {
        return new SenalCanal(canal, new BigDecimal(valor), T0, false);
    }

    /** Reglas 1 a 3 de la tabla de decisión, evaluadas en orden. */
    @Nested
    class Reglas {

        @Test
        @DisplayName("CP-06 · tres canales coherentes son confiables (R1)")
        void tresCanalesCoherentes() {
            var a = senal(Canal.A, "300.0");
            var b = senal(Canal.B, "300.5");
            var c = senal(Canal.C, "301.0");
            var todos = List.of(a, b, c);

            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, a, todos, T0)).isEmpty();
            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, b, todos, T0)).isEmpty();
            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, c, todos, T0)).isEmpty();
        }

        @Test
        @DisplayName("CP-07 · C a 306,64 °C discrepa más del 2 % de la mediana (R4)")
        void canalQueDiscrepa() {
            var a = senal(Canal.A, "300.00");
            var b = senal(Canal.B, "300.00");
            var c = senal(Canal.C, "306.64");
            var todos = List.of(a, b, c);

            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, c, todos, T0)).contains(DISCREPANCIA);
            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, a, todos, T0)).isEmpty();
        }

        @Test
        @DisplayName("CP-08 · C a 306,60 °C (exactamente 2,00 %) sigue confiable")
        void discrepanciaExactaDelDosPorCiento() {
            var c = senal(Canal.C, "306.60");
            var todos = List.of(senal(Canal.A, "300.00"), senal(Canal.B, "300.00"), c);

            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, c, todos, T0)).isEmpty();
        }

        @Test
        @DisplayName("CP-08 · el 2 % se toma del rango físico de cada magnitud (presión: 3,6 bar)")
        void discrepanciaSobreOtroRango() {
            var enElLimite = new SenalCanal(Canal.C, new BigDecimal("103.6"), T0, false);
            var pasado = new SenalCanal(Canal.C, new BigDecimal("103.61"), T0, false);
            var a = new SenalCanal(Canal.A, new BigDecimal("100"), T0, false);
            var b = new SenalCanal(Canal.B, new BigDecimal("100"), T0, false);

            assertThat(validador.evaluar(Magnitud.PRESION_PRIMARIO, enElLimite, List.of(a, b, enElLimite), T0))
                    .isEmpty();
            assertThat(validador.evaluar(Magnitud.PRESION_PRIMARIO, pasado, List.of(a, b, pasado), T0))
                    .contains(DISCREPANCIA);
        }

        @Test
        @DisplayName("CP-06 · con tres valores distintos la mediana es el del medio (A1)")
        void medianaConTresValores() {
            var c = senal(Canal.C, "306");
            var todos = List.of(senal(Canal.A, "294"), senal(Canal.B, "300"), c);

            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, c, todos, T0)).isEmpty();
        }

        @Test
        @DisplayName("CP-11 · con solo dos canales con valor la mediana es su promedio (A1)")
        void medianaConDosValores() {
            var a = senal(Canal.A, "300");
            var b = senal(Canal.B, "310");
            var todos = List.of(a, b, SenalCanal.sinDatos(Canal.C));

            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, a, todos, T0)).isEmpty();
            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, b, todos, T0)).isEmpty();
        }

        @Test
        @DisplayName("CP-09 · 10,0 s sin actualizar es confiable; 10,1 s es SIN_ACTUALIZACION (R2)")
        void canalSinActualizar() {
            var c = senal(Canal.C, "300.0");
            var todos = List.of(senal(Canal.A, "300.0"), senal(Canal.B, "300.0"), c);

            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, c, todos, T0.plusMillis(10_000))).isEmpty();
            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, c, todos, T0.plusMillis(10_100)))
                    .contains(SIN_ACTUALIZACION);
        }

        @Test
        @DisplayName("CP-09 · un canal que nunca envió lecturas es SIN_ACTUALIZACION")
        void canalSinDatos() {
            var c = SenalCanal.sinDatos(Canal.C);
            var todos = List.of(senal(Canal.A, "300.0"), senal(Canal.B, "300.0"), c);

            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, c, todos, T0)).contains(SIN_ACTUALIZACION);
        }

        @Test
        @DisplayName("CP-10 · C con última lectura de 351 °C es FUERA_DE_RANGO (R3)")
        void canalFueraDeRango() {
            var c = new SenalCanal(Canal.C, null, T0, true);
            var todos = List.of(senal(Canal.A, "300"), senal(Canal.B, "300"), c);

            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, c, todos, T0)).contains(FUERA_DE_RANGO);
        }

        @Test
        @DisplayName("CP-10 · sin actualizar tiene prioridad sobre fuera de rango (orden de la tabla)")
        void ordenDeEvaluacion() {
            var c = new SenalCanal(Canal.C, new BigDecimal("300"), T0, true);
            var todos = List.of(senal(Canal.A, "300"), senal(Canal.B, "300"), c);

            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, c, todos, T0.plusSeconds(11)))
                    .contains(SIN_ACTUALIZACION);
        }

        @Test
        @DisplayName("CP-10 · fuera de rango tiene prioridad sobre la discrepancia del valor anterior")
        void fueraDeRangoAntesQueDiscrepancia() {
            var c = new SenalCanal(Canal.C, new BigDecimal("320"), T0, true);
            var todos = List.of(senal(Canal.A, "300"), senal(Canal.B, "300"), c);

            assertThat(validador.evaluar(TEMPERATURA_PRIMARIO, c, todos, T0)).contains(FUERA_DE_RANGO);
        }
    }

    /** Recuperación de un canal no confiable (A3, transición de estados). */
    @Nested
    class Recuperacion {

        private static final Optional<MotivoNoConfiable> BUENA = Optional.empty();

        @Test
        @DisplayName("CP-12 · un canal confiable con lectura buena sigue confiable")
        void confiableSigueConfiable() {
            assertThat(validador.registrarLectura(RecuperacionCanal.CONFIABLE, BUENA))
                    .isEqualTo(RecuperacionCanal.CONFIABLE);
        }

        @Test
        @DisplayName("CP-12 · una lectura que no cumple marca el canal con su motivo")
        void lecturaMalaMarcaElCanal() {
            assertThat(validador.registrarLectura(RecuperacionCanal.CONFIABLE, Optional.of(DISCREPANCIA)))
                    .isEqualTo(RecuperacionCanal.marcado(DISCREPANCIA, 0));
        }

        @Test
        @DisplayName("CP-12 · con 2 lecturas buenas sigue NO_CONFIABLE; con la 3.ª vuelve a CONFIABLE")
        void recuperaConTresLecturas() {
            var estado = RecuperacionCanal.marcado(DISCREPANCIA, 0);

            estado = validador.registrarLectura(estado, BUENA);
            estado = validador.registrarLectura(estado, BUENA);
            assertThat(estado).isEqualTo(RecuperacionCanal.marcado(DISCREPANCIA, 2));

            estado = validador.registrarLectura(estado, BUENA);
            assertThat(estado).isEqualTo(RecuperacionCanal.CONFIABLE);
        }

        @Test
        @DisplayName("CP-12 · una lectura mala en medio reinicia el contador y actualiza el motivo")
        void lecturaMalaReiniciaElContador() {
            var estado = RecuperacionCanal.marcado(DISCREPANCIA, 2);

            estado = validador.registrarLectura(estado, Optional.of(FUERA_DE_RANGO));
            assertThat(estado).isEqualTo(RecuperacionCanal.marcado(FUERA_DE_RANGO, 0));

            estado = validador.registrarLectura(estado, BUENA);
            estado = validador.registrarLectura(estado, BUENA);
            assertThat(estado.noConfiable()).isTrue();
        }
    }

    /** Valor consolidado, utilizabilidad y avisos de la magnitud. */
    @Nested
    class Consolidacion {

        private final Map<Canal, RecuperacionCanal> sinMarcas = Map.of();

        @Test
        @DisplayName("CP-06 · tres confiables: utilizable, promedio de los tres y sin avisos")
        void tresConfiables() {
            var senales = List.of(senal(Canal.A, "300.0"), senal(Canal.B, "300.5"), senal(Canal.C, "301.0"));

            var estado = validador.consolidar(TEMPERATURA_PRIMARIO, senales, sinMarcas, T0);

            assertThat(estado.utilizable()).isTrue();
            assertThat(estado.valorConsolidado()).isEqualByComparingTo("300.50");
            assertThat(estado.avisos()).isEmpty();
            assertThat(estado.canales()).extracting(EstadoCanal::estado)
                    .containsOnly(EstadoConfianza.CONFIABLE);
        }

        @Test
        @DisplayName("CP-07 · el valor consolidado usa solo A y B y avisa que C quedó excluido")
        void excluyeAlCanalQueDiscrepa() {
            var senales = List.of(senal(Canal.A, "300.00"), senal(Canal.B, "300.00"), senal(Canal.C, "306.64"));

            var estado = validador.consolidar(TEMPERATURA_PRIMARIO, senales, sinMarcas, T0);

            assertThat(estado.utilizable()).isTrue();
            assertThat(estado.valorConsolidado()).isEqualByComparingTo("300.00");
            assertThat(estado.canales()).contains(
                    new EstadoCanal(Canal.C, EstadoConfianza.NO_CONFIABLE, DISCREPANCIA));
            assertThat(estado.avisos()).containsExactly("CANAL_EXCLUIDO:C");
        }

        @Test
        @DisplayName("CP-11 · con un solo canal confiable la magnitud no es utilizable y se avisa")
        void menosDeDosConfiables() {
            var ahora = T0.plusSeconds(11);
            var senales = List.of(
                    new SenalCanal(Canal.A, new BigDecimal("300"), ahora, false),
                    senal(Canal.B, "300"),
                    senal(Canal.C, "300"));

            var estado = validador.consolidar(TEMPERATURA_PRIMARIO, senales, sinMarcas, ahora);

            assertThat(estado.utilizable()).isFalse();
            assertThat(estado.valorConsolidado()).isNull();
            assertThat(estado.avisos())
                    .containsExactly("CANAL_EXCLUIDO:B", "CANAL_EXCLUIDO:C", "MAGNITUD_NO_DISPONIBLE");
        }

        @Test
        @DisplayName("CP-11 · con exactamente dos confiables la magnitud es utilizable")
        void dosConfiablesAlcanzan() {
            var senales = List.of(senal(Canal.A, "300"), senal(Canal.B, "302"), SenalCanal.sinDatos(Canal.C));

            var estado = validador.consolidar(TEMPERATURA_PRIMARIO, senales, sinMarcas, T0);

            assertThat(estado.utilizable()).isTrue();
            assertThat(estado.valorConsolidado()).isEqualByComparingTo("301");
            assertThat(estado.avisos()).containsExactly("CANAL_EXCLUIDO:C");
        }

        @Test
        @DisplayName("CP-07 · el valor consolidado se redondea a 2 decimales")
        void promedioRedondeado() {
            var senales = List.of(senal(Canal.A, "300"), senal(Canal.B, "300"), senal(Canal.C, "302"));

            var estado = validador.consolidar(TEMPERATURA_PRIMARIO, senales, sinMarcas, T0);

            assertThat(estado.valorConsolidado()).isEqualTo(new BigDecimal("300.67"));
        }

        @Test
        @DisplayName("CP-12 · un canal marcado que todavía no se recuperó sigue NO_CONFIABLE con su motivo")
        void canalMarcadoSinRecuperar() {
            var senales = List.of(senal(Canal.A, "300"), senal(Canal.B, "300"), senal(Canal.C, "300"));
            var marcas = Map.of(Canal.C, RecuperacionCanal.marcado(DISCREPANCIA, 2));

            var estado = validador.consolidar(TEMPERATURA_PRIMARIO, senales, marcas, T0);

            assertThat(estado.canales()).contains(
                    new EstadoCanal(Canal.C, EstadoConfianza.NO_CONFIABLE, DISCREPANCIA));
            assertThat(estado.valorConsolidado()).isEqualByComparingTo("300");
        }

        @Test
        @DisplayName("CP-09 · el motivo actual tiene prioridad sobre el motivo con que quedó marcado")
        void motivoActualPrimeroQueElMarcado() {
            var ahora = T0.plusSeconds(11);
            var senales = List.of(
                    new SenalCanal(Canal.A, new BigDecimal("300"), ahora, false),
                    new SenalCanal(Canal.B, new BigDecimal("300"), ahora, false),
                    senal(Canal.C, "300"));
            var marcas = Map.of(Canal.C, RecuperacionCanal.marcado(DISCREPANCIA, 1));

            var estado = validador.consolidar(TEMPERATURA_PRIMARIO, senales, marcas, ahora);

            assertThat(estado.canales()).contains(
                    new EstadoCanal(Canal.C, EstadoConfianza.NO_CONFIABLE, SIN_ACTUALIZACION));
        }
    }
}
