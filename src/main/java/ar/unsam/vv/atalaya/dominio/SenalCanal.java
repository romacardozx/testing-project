package ar.unsam.vv.atalaya.dominio;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lo último que se sabe de un canal.
 *
 * @param valor              último valor aceptado, o null si nunca hubo uno
 * @param ultimaRecepcion    instante de la última lectura recibida (aceptada o descartada por rango),
 *                           o null si el canal nunca envió nada
 * @param ultimaFueraDeRango true si la última lectura recibida estaba fuera de rango (A17)
 */
public record SenalCanal(Canal canal, BigDecimal valor, Instant ultimaRecepcion, boolean ultimaFueraDeRango) {

    public static SenalCanal sinDatos(Canal canal) {
        return new SenalCanal(canal, null, null, false);
    }
}
