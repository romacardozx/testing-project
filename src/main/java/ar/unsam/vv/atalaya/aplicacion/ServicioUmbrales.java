package ar.unsam.vv.atalaya.aplicacion;

import ar.unsam.vv.atalaya.dominio.EstadoSolicitud;
import ar.unsam.vv.atalaya.dominio.Umbrales;
import ar.unsam.vv.atalaya.dominio.ValidadorUmbrales;
import ar.unsam.vv.atalaya.persistencia.SolicitudUmbralEntidad;
import ar.unsam.vv.atalaya.persistencia.SolicitudUmbralRepositorio;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-12: umbrales vigentes y cambio con doble aprobación. Vigentes = última solicitud APLICADA o, si
 * no hay ninguna, los por defecto (40/75). Los roles los controla la seguridad; acá se controla que
 * quien aprueba no sea quien solicitó.
 */
@Service
public class ServicioUmbrales {

    private final SolicitudUmbralRepositorio solicitudes;
    private final ValidadorUmbrales validador;
    private final Clock reloj;

    public ServicioUmbrales(SolicitudUmbralRepositorio solicitudes, ValidadorUmbrales validador, Clock reloj) {
        this.solicitudes = solicitudes;
        this.validador = validador;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Umbrales vigentes() {
        return solicitudes.findFirstByEstadoOrderByAprobadaEnDescIdDesc(EstadoSolicitud.APLICADA)
                .map(SolicitudUmbralEntidad::umbrales)
                .orElse(Umbrales.POR_DEFECTO);
    }

    /** Valida y deja la solicitud PENDIENTE: los umbrales vigentes no cambian todavía. */
    @Transactional
    public SolicitudUmbralVista solicitar(Umbrales umbrales, String usuario) {
        validador.validar(umbrales);
        return SolicitudUmbralVista.de(solicitudes.save(new SolicitudUmbralEntidad(umbrales, usuario, reloj.instant())));
    }

    @Transactional
    public SolicitudUmbralVista aprobar(long id, String usuario) {
        var solicitud = solicitudes.findById(id).orElseThrow(() ->
                new RecursoInexistenteException("SOLICITUD_INEXISTENTE", "No existe la solicitud " + id));
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ConflictoException("SOLICITUD_NO_PENDIENTE", "La solicitud ya fue aplicada");
        }
        if (solicitud.getSolicitadaPor().equals(usuario)) {
            throw new ConflictoException("MISMO_USUARIO", "Quien aprueba no puede ser quien solicitó");
        }
        solicitud.aplicar(usuario, reloj.instant());
        return SolicitudUmbralVista.de(solicitud);
    }
}
