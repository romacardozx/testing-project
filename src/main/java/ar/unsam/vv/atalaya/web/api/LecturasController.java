package ar.unsam.vv.atalaya.web.api;

import ar.unsam.vv.atalaya.aplicacion.NuevaLectura;
import ar.unsam.vv.atalaya.aplicacion.ServicioLecturas;
import ar.unsam.vv.atalaya.dominio.Canal;
import ar.unsam.vv.atalaya.dominio.EstadoLectura;
import ar.unsam.vv.atalaya.dominio.Magnitud;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /api/v1/lecturas (RF-01/02). Mensajes mal formados → 400 sin tocar el estado. */
@RestController
@RequestMapping("/api/v1/lecturas")
public class LecturasController {

    private final ServicioLecturas servicio;

    public LecturasController(ServicioLecturas servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public LecturaRespuesta registrar(@Valid @RequestBody LecturaSolicitud solicitud) {
        var estado = servicio.registrar(new NuevaLectura(solicitud.magnitud(), solicitud.canal(),
                solicitud.valor(), solicitud.secuencia(), solicitud.timestampOrigen()));
        return new LecturaRespuesta(estado);
    }

    public record LecturaSolicitud(
            @NotNull(message = "La magnitud es obligatoria") Magnitud magnitud,
            @NotNull(message = "El canal es obligatorio") Canal canal,
            @NotNull(message = "El valor es obligatorio") BigDecimal valor,
            @NotNull(message = "La secuencia es obligatoria") Long secuencia,
            @NotNull(message = "El timestamp de origen es obligatorio") Instant timestampOrigen) {
    }

    public record LecturaRespuesta(EstadoLectura estado) {
    }
}
