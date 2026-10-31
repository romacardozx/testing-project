package ar.unsam.vv.atalaya.web.api;

import ar.unsam.vv.atalaya.aplicacion.EstimacionVista;
import ar.unsam.vv.atalaya.aplicacion.ServicioEstimaciones;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/v1/estimaciones/actual: última estimación de cada tipo (RF-06). */
@RestController
@RequestMapping("/api/v1/estimaciones")
public class EstimacionesController {

    private final ServicioEstimaciones servicio;

    public EstimacionesController(ServicioEstimaciones servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/actual")
    public List<EstimacionVista> actual() {
        return servicio.actuales();
    }
}
