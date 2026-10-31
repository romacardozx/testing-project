package ar.unsam.vv.atalaya.web.api;

import ar.unsam.vv.atalaya.aplicacion.ServicioMagnitudes;
import ar.unsam.vv.atalaya.dominio.EstadoMagnitud;
import ar.unsam.vv.atalaya.dominio.Magnitud;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/v1/magnitudes/{magnitud} (RF-03). El nombre no distingue mayúsculas. */
@RestController
@RequestMapping("/api/v1/magnitudes")
public class MagnitudesController {

    private final ServicioMagnitudes servicio;

    public MagnitudesController(ServicioMagnitudes servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{magnitud}")
    public EstadoMagnitud consultar(@PathVariable String magnitud) {
        Magnitud m = Magnitud.desde(magnitud).orElseThrow(() -> new MagnitudInexistenteException(magnitud));
        return servicio.consultar(m);
    }
}
