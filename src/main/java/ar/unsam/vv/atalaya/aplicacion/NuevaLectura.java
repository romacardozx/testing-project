package ar.unsam.vv.atalaya.aplicacion;

import ar.unsam.vv.atalaya.dominio.Canal;
import ar.unsam.vv.atalaya.dominio.Magnitud;
import java.math.BigDecimal;
import java.time.Instant;

/** Datos de una lectura ya validados en formato. */
public record NuevaLectura(Magnitud magnitud, Canal canal, BigDecimal valor, long secuencia, Instant timestampOrigen) {
}
