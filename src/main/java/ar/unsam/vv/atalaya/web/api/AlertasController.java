package ar.unsam.vv.atalaya.web.api;

import ar.unsam.vv.atalaya.aplicacion.AlertaVista;
import ar.unsam.vv.atalaya.aplicacion.ServicioAlertas;
import ar.unsam.vv.atalaya.dominio.EstadoAlerta;
import ar.unsam.vv.atalaya.dominio.Rol;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta y reconocimiento de alertas (RF-07/08). */
@RestController
@RequestMapping("/api/v1/alertas")
public class AlertasController {

    private final ServicioAlertas servicio;

    public AlertasController(ServicioAlertas servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<AlertaVista> listar(@RequestParam(required = false) EstadoAlerta estado) {
        return servicio.listar(Optional.ofNullable(estado));
    }

    @PostMapping("/{id}/reconocimiento")
    public AlertaVista reconocer(@PathVariable long id, Authentication usuario) {
        return servicio.reconocer(id, usuario.getName(), rolDe(usuario));
    }

    private static Rol rolDe(Authentication usuario) {
        return usuario.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .map(Rol::valueOf)
                .findFirst()
                .orElseThrow();
    }
}
