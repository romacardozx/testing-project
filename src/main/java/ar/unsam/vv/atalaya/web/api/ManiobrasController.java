package ar.unsam.vv.atalaya.web.api;

import ar.unsam.vv.atalaya.aplicacion.ManiobraVista;
import ar.unsam.vv.atalaya.aplicacion.ServicioManiobras;
import ar.unsam.vv.atalaya.dominio.Magnitud;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Declarar (POST) y finalizar (DELETE) maniobras (RF-09). Solo el jefe de turno. */
@RestController
@RequestMapping("/api/v1/maniobras")
public class ManiobrasController {

    private static final int DURACION_POR_DEFECTO_MIN = 30;

    private final ServicioManiobras servicio;

    public ManiobrasController(ServicioManiobras servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ManiobraVista declarar(@Valid @RequestBody ManiobraSolicitud solicitud, Authentication usuario) {
        int duracion = solicitud.duracionMaximaMin() == null ? DURACION_POR_DEFECTO_MIN : solicitud.duracionMaximaMin();
        return servicio.declarar(solicitud.magnitud(), solicitud.bandaMin(), solicitud.bandaMax(), duracion,
                usuario.getName());
    }

    @DeleteMapping("/{id}")
    public ManiobraVista finalizar(@PathVariable long id) {
        return servicio.finalizar(id);
    }

    public record ManiobraSolicitud(
            @NotNull(message = "La magnitud es obligatoria") Magnitud magnitud,
            @NotNull(message = "bandaMin es obligatoria") BigDecimal bandaMin,
            @NotNull(message = "bandaMax es obligatoria") BigDecimal bandaMax,
            @Min(value = 1, message = "La duración mínima es 1 minuto") Integer duracionMaximaMin) {

        @AssertTrue(message = "bandaMin no puede ser mayor que bandaMax")
        public boolean isBandaOrdenada() {
            return bandaMin == null || bandaMax == null || bandaMin.compareTo(bandaMax) <= 0;
        }
    }
}
