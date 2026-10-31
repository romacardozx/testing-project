package ar.unsam.vv.atalaya.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * RF-03: confiabilidad de cada canal y valor consolidado de la magnitud. Desarrollado con TDD.
 *
 * <p>Los límites son campos de instancia o literales: PIT no muta inicializadores estáticos y un
 * campo {@code final int} constante se inlinea.
 */
public class ValidadorRedundancia {

    /** Regla 1: más de 10 s sin actualizar (10,0 s sigue confiable). */
    private final Duration plazoActualizacion = Duration.ofSeconds(10);

    /** Regla 3: más del 2 % del rango físico respecto de la mediana (A1, A2). */
    private final BigDecimal porcentajeDiscrepancia = BigDecimal.valueOf(2);

    /** Evalúa las reglas 1 a 3 en orden y devuelve el primer motivo que no se cumple. */
    public Optional<MotivoNoConfiable> evaluar(Magnitud magnitud, SenalCanal canal,
                                               Collection<SenalCanal> todos, Instant ahora) {
        if (sinActualizar(canal, ahora)) {
            return Optional.of(MotivoNoConfiable.SIN_ACTUALIZACION);
        }
        if (canal.ultimaFueraDeRango()) {
            return Optional.of(MotivoNoConfiable.FUERA_DE_RANGO);
        }
        if (discrepa(magnitud, canal, todos)) {
            return Optional.of(MotivoNoConfiable.DISCREPANCIA);
        }
        return Optional.empty();
    }

    /**
     * Actualiza la memoria del canal con el resultado de una lectura nueva (A3).
     *
     * @param evaluacion resultado de {@link #evaluar} para esa lectura
     */
    public RecuperacionCanal registrarLectura(RecuperacionCanal previa, Optional<MotivoNoConfiable> evaluacion) {
        if (evaluacion.isPresent()) {
            return RecuperacionCanal.marcado(evaluacion.get(), 0);
        }
        if (!previa.noConfiable()) {
            return RecuperacionCanal.CONFIABLE;
        }
        int buenas = previa.buenasSeguidas() + 1;
        if (buenas >= 3) {                     // A3: 3 lecturas buenas seguidas
            return RecuperacionCanal.CONFIABLE;
        }
        return RecuperacionCanal.marcado(previa.motivo(), buenas);
    }

    /**
     * Estado de la magnitud a partir de las señales de los tres canales y su memoria de recuperación.
     * Un canal no confiable lo es por una regla que hoy no cumple o porque todavía no se recuperó.
     */
    public EstadoMagnitud consolidar(Magnitud magnitud, Collection<SenalCanal> senales,
                                     Map<Canal, RecuperacionCanal> recuperaciones, Instant ahora) {
        List<EstadoCanal> canales = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        List<BigDecimal> confiables = new ArrayList<>();
        for (SenalCanal senal : senales) {
            EstadoCanal estado = estadoDe(magnitud, senal, senales,
                    recuperaciones.getOrDefault(senal.canal(), RecuperacionCanal.CONFIABLE), ahora);
            canales.add(estado);
            if (estado.estado() == EstadoConfianza.CONFIABLE) {
                confiables.add(senal.valor());
            } else {
                avisos.add("CANAL_EXCLUIDO:" + senal.canal());
            }
        }
        boolean utilizable = confiables.size() >= 2;   // 2 o más canales confiables
        if (!utilizable) {
            avisos.add("MAGNITUD_NO_DISPONIBLE");
        }
        BigDecimal valor = utilizable ? promedio(confiables) : null;
        return new EstadoMagnitud(magnitud, valor, utilizable, canales, avisos);
    }

    private EstadoCanal estadoDe(Magnitud magnitud, SenalCanal senal, Collection<SenalCanal> todos,
                                 RecuperacionCanal recuperacion, Instant ahora) {
        Optional<MotivoNoConfiable> actual = evaluar(magnitud, senal, todos, ahora);
        if (actual.isPresent()) {
            return new EstadoCanal(senal.canal(), EstadoConfianza.NO_CONFIABLE, actual.get());
        }
        if (recuperacion.noConfiable()) {
            return new EstadoCanal(senal.canal(), EstadoConfianza.NO_CONFIABLE, recuperacion.motivo());
        }
        return new EstadoCanal(senal.canal(), EstadoConfianza.CONFIABLE, null);
    }

    private BigDecimal promedio(List<BigDecimal> valores) {
        BigDecimal suma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(valores.size()), 2, RoundingMode.HALF_UP);
    }

    private boolean sinActualizar(SenalCanal canal, Instant ahora) {
        return canal.ultimaRecepcion() == null
                || Duration.between(canal.ultimaRecepcion(), ahora).compareTo(plazoActualizacion) > 0;
    }

    private boolean discrepa(Magnitud magnitud, SenalCanal canal, Collection<SenalCanal> todos) {
        BigDecimal desvio = canal.valor().subtract(mediana(todos)).abs();
        return desvio.compareTo(limiteDiscrepancia(magnitud)) > 0;
    }

    private BigDecimal limiteDiscrepancia(Magnitud magnitud) {
        return magnitud.rangoFisico().multiply(porcentajeDiscrepancia).movePointLeft(2);
    }

    /** Mediana de los canales con valor. Con dos valores es su promedio (división exacta por 2). */
    private BigDecimal mediana(Collection<SenalCanal> todos) {
        List<BigDecimal> valores = todos.stream()
                .map(SenalCanal::valor)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        int medio = valores.size() / 2;
        if (valores.size() % 2 == 1) {
            return valores.get(medio);
        }
        return valores.get(medio - 1).add(valores.get(medio)).divide(BigDecimal.valueOf(2));
    }
}
