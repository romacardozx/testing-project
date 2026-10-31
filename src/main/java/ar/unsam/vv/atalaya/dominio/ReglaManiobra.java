package ar.unsam.vv.atalaya.dominio;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * RF-09: agrupación de estimaciones durante una maniobra declarada y su expiración (A6).
 */
public class ReglaManiobra {

    /** La maniobra expira cuando ahora &gt;= declaradaEn + duración máxima (a 29:59 sigue, a 30:00 no). */
    public boolean expiro(Instant declaradaEn, int duracionMaximaMin, Instant ahora) {
        return !ahora.isBefore(declaradaEn.plus(Duration.ofMinutes(duracionMaximaMin)));
    }

    /**
     * Una estimación de nivel ATENCION o mayor se agrupa si la magnitud de la maniobra es utilizable y
     * su valor consolidado está dentro de [bandaMin, bandaMax], extremos incluidos. Si no, alerta normal.
     */
    public boolean agrupa(NivelRiesgo nivel, BigDecimal bandaMin, BigDecimal bandaMax, EstadoMagnitud magnitud) {
        if (nivel == NivelRiesgo.NORMAL || !magnitud.utilizable()) {
            return false;
        }
        BigDecimal valor = magnitud.valorConsolidado();
        return valor.compareTo(bandaMin) >= 0 && valor.compareTo(bandaMax) <= 0;
    }
}
