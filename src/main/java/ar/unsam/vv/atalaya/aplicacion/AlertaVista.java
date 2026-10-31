package ar.unsam.vv.atalaya.aplicacion;

import ar.unsam.vv.atalaya.dominio.CanalAlerta;
import ar.unsam.vv.atalaya.dominio.ClaseAlerta;
import ar.unsam.vv.atalaya.dominio.EstadoAlerta;
import ar.unsam.vv.atalaya.dominio.NivelRiesgo;
import ar.unsam.vv.atalaya.dominio.Rol;
import ar.unsam.vv.atalaya.dominio.TipoEstimacion;
import ar.unsam.vv.atalaya.persistencia.AlertaEntidad;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Alerta tal como se informa en la API y en el panel. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlertaVista(Long id, ClaseAlerta clase, TipoEstimacion tipo, NivelRiesgo nivel, BigDecimal probabilidad,
                          EstadoAlerta estado, List<CanalAlerta> canales, List<Rol> destinatarios,
                          boolean requiereReconocimiento, Instant emitidaEn, Instant escaladaEn,
                          Long maniobraId, Integer agrupadas) {

    static AlertaVista de(AlertaEntidad a) {
        Integer agrupadas = a.getClase() == ClaseAlerta.INFORMATIVA ? a.getAgrupadas() : null;
        return new AlertaVista(a.getId(), a.getClase(), a.getTipo(), a.getNivel(), a.getProbabilidad(), a.getEstado(),
                a.getCanales(), a.getDestinatarios(), a.isRequiereReconocimiento(),
                a.getEmitidaEn(), a.getEscaladaEn(), a.getManiobraId(), agrupadas);
    }
}
