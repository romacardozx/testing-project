package ar.unsam.vv.atalaya.aplicacion;

import ar.unsam.vv.atalaya.dominio.EstadoSolicitud;
import ar.unsam.vv.atalaya.persistencia.SolicitudUmbralEntidad;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/** Solicitud de cambio de umbrales tal como se informa. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SolicitudUmbralVista(Long id, BigDecimal alto, BigDecimal critico, EstadoSolicitud estado,
                                   String solicitadaPor, String aprobadaPor) {

    static SolicitudUmbralVista de(SolicitudUmbralEntidad s) {
        return new SolicitudUmbralVista(s.getId(), s.umbrales().alto(), s.umbrales().critico(), s.getEstado(),
                s.getSolicitadaPor(), s.getAprobadaPor());
    }
}
