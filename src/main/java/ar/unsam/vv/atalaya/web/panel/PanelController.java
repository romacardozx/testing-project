package ar.unsam.vv.atalaya.web.panel;

import ar.unsam.vv.atalaya.aplicacion.AlertaVista;
import ar.unsam.vv.atalaya.aplicacion.ServicioAlertas;
import ar.unsam.vv.atalaya.aplicacion.ServicioEstimaciones;
import ar.unsam.vv.atalaya.aplicacion.ServicioMagnitudes;
import ar.unsam.vv.atalaya.dominio.AlertaYaReconocidaException;
import ar.unsam.vv.atalaya.dominio.EstadoAlerta;
import ar.unsam.vv.atalaya.dominio.Magnitud;
import ar.unsam.vv.atalaya.dominio.Rol;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Panel web mínimo para la sala de control y las pruebas de Selenium (UI-01 a UI-03): alertas activas
 * con botón Reconocer, estimación vigente por tipo y estado de los canales con sus avisos.
 */
@Controller
public class PanelController {

    private static final Set<Rol> RECONOCEDORES = Set.of(Rol.OPERADOR_REACTOR, Rol.JEFE_TURNO, Rol.SUPERVISOR_SO);

    private final ServicioAlertas alertas;
    private final ServicioEstimaciones estimaciones;
    private final ServicioMagnitudes magnitudes;

    public PanelController(ServicioAlertas alertas, ServicioEstimaciones estimaciones, ServicioMagnitudes magnitudes) {
        this.alertas = alertas;
        this.estimaciones = estimaciones;
        this.magnitudes = magnitudes;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/", "/panel"})
    public String panel(Model modelo, Authentication usuario) {
        List<AlertaVista> activas = alertas.listar(Optional.empty()).stream()
                .filter(a -> a.estado() != EstadoAlerta.RECONOCIDA)
                .toList();
        modelo.addAttribute("usuario", usuario.getName());
        modelo.addAttribute("puedeReconocer", RECONOCEDORES.contains(rolDe(usuario)));
        modelo.addAttribute("alertas", activas);
        modelo.addAttribute("estimaciones", estimaciones.actuales());
        modelo.addAttribute("magnitudes", Arrays.stream(Magnitud.values()).map(magnitudes::consultar).toList());
        return "panel";
    }

    /** Reconocer en una sola interacción (UI-01 pide 2 o menos). */
    @PostMapping("/panel/alertas/{id}/reconocer")
    public String reconocer(@PathVariable long id, Authentication usuario) {
        try {
            alertas.reconocer(id, usuario.getName(), rolDe(usuario));
            return "redirect:/panel?reconocida=" + id;
        } catch (AlertaYaReconocidaException | ConcurrencyFailureException | DataIntegrityViolationException e) {
            return "redirect:/panel?yaReconocida=" + id;
        }
    }

    private static Rol rolDe(Authentication usuario) {
        return usuario.getAuthorities().stream()
                .map(a -> Rol.valueOf(a.getAuthority().replaceFirst("^ROLE_", "")))
                .findFirst()
                .orElseThrow();
    }
}
