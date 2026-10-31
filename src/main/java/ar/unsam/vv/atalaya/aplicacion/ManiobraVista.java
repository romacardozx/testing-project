package ar.unsam.vv.atalaya.aplicacion;

import ar.unsam.vv.atalaya.dominio.EstadoManiobra;
import ar.unsam.vv.atalaya.dominio.Magnitud;
import ar.unsam.vv.atalaya.persistencia.ManiobraEntidad;
import java.math.BigDecimal;
import java.time.Instant;

/** Maniobra tal como se informa en la API. */
public record ManiobraVista(Long id, Magnitud magnitud, BigDecimal bandaMin, BigDecimal bandaMax,
                            int duracionMaximaMin, Instant declaradaEn, String declaradaPor, EstadoManiobra estado) {

    static ManiobraVista de(ManiobraEntidad m) {
        return new ManiobraVista(m.getId(), m.getMagnitud(), m.getBandaMin(), m.getBandaMax(),
                m.getDuracionMaximaMin(), m.getDeclaradaEn(), m.getDeclaradaPor(), m.getEstado());
    }
}
