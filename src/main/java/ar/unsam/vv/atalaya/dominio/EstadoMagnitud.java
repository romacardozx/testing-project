package ar.unsam.vv.atalaya.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de la redundancia para una magnitud.
 *
 * @param valorConsolidado promedio de los canales confiables, o null si no es utilizable
 * @param avisos           CANAL_EXCLUIDO:&lt;canal&gt; y MAGNITUD_NO_DISPONIBLE
 */
public record EstadoMagnitud(Magnitud magnitud, BigDecimal valorConsolidado, boolean utilizable,
                             List<EstadoCanal> canales, List<String> avisos) {

    public EstadoMagnitud {
        canales = List.copyOf(canales);
        avisos = List.copyOf(avisos);
    }
}
