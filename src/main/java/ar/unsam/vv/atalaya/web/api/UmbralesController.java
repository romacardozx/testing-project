package ar.unsam.vv.atalaya.web.api;

import ar.unsam.vv.atalaya.aplicacion.ServicioUmbrales;
import ar.unsam.vv.atalaya.aplicacion.SolicitudUmbralVista;
import ar.unsam.vv.atalaya.dominio.Umbrales;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Umbrales vigentes, solicitud de cambio (supervisor de SO) y aprobación (jefe de turno) (RF-12). */
@RestController
@RequestMapping("/api/v1/config/umbrales")
public class UmbralesController {

    private final ServicioUmbrales servicio;

    public UmbralesController(ServicioUmbrales servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Umbrales vigentes() {
        return servicio.vigentes();
    }

    @PutMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudUmbralVista solicitar(@Valid @RequestBody UmbralesSolicitud solicitud, Authentication usuario) {
        return servicio.solicitar(new Umbrales(solicitud.alto(), solicitud.critico()), usuario.getName());
    }

    @PostMapping("/{id}/aprobacion")
    public SolicitudUmbralVista aprobar(@PathVariable long id, Authentication usuario) {
        return servicio.aprobar(id, usuario.getName());
    }

    public record UmbralesSolicitud(
            @NotNull(message = "El umbral alto es obligatorio") BigDecimal alto,
            @NotNull(message = "El umbral crítico es obligatorio") BigDecimal critico) {
    }
}
