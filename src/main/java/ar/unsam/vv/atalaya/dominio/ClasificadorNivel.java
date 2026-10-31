package ar.unsam.vv.atalaya.dominio;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * RF-06: nivel de riesgo de una estimación. {@link #clasificar} conserva la forma del informe
 * (sección 7.3): cuatro decisiones D1 a D4, V(G) = 5. Es la base de la caja blanca de CP-17.
 *
 * <p>Los límites son campos de instancia y no {@code static final}: PIT no muta inicializadores
 * estáticos y así INLINE_CONSTS puede cambiar el 15 (sección 10.2).
 */
public class ClasificadorNivel {

    private final BigDecimal minimo = BigDecimal.valueOf(0);
    private final BigDecimal maximo = BigDecimal.valueOf(100);
    private final BigDecimal inicioAtencion = BigDecimal.valueOf(15);

    public NivelRiesgo clasificar(BigDecimal p, Umbrales u) {
        Objects.requireNonNull(p, "La probabilidad es obligatoria");
        if (p.compareTo(minimo) < 0 || p.compareTo(maximo) > 0) {   // D1
            throw new ProbabilidadInvalidaException(p);
        }
        if (p.compareTo(u.critico()) >= 0) {                         // D2
            return NivelRiesgo.CRITICO;
        }
        if (p.compareTo(u.alto()) >= 0) {                            // D3
            return NivelRiesgo.ALTO;
        }
        if (p.compareTo(inicioAtencion) >= 0) {                      // D4
            return NivelRiesgo.ATENCION;
        }
        return NivelRiesgo.NORMAL;
    }

    /** Desde ATENCION, los 3 factores de mayor peso de mayor a menor; en NORMAL, ninguno. */
    public List<Factor> factoresPrincipales(NivelRiesgo nivel, List<Factor> factores) {
        if (nivel == NivelRiesgo.NORMAL) {
            return List.of();
        }
        return factores.stream()
                .sorted(Comparator.comparing(Factor::peso).reversed())
                .limit(3)   // literal: un campo final int se inlinea y PIT no lo podría mutar
                .toList();
    }
}
