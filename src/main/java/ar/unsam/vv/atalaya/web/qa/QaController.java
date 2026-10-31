package ar.unsam.vv.atalaya.web.qa;

import ar.unsam.vv.atalaya.aplicacion.EstimacionVista;
import ar.unsam.vv.atalaya.aplicacion.NuevaEstimacion;
import ar.unsam.vv.atalaya.aplicacion.ServicioAlertas;
import ar.unsam.vv.atalaya.aplicacion.ServicioEstimaciones;
import ar.unsam.vv.atalaya.aplicacion.ServicioManiobras;
import ar.unsam.vv.atalaya.aplicacion.ServicioReset;
import ar.unsam.vv.atalaya.dominio.Factor;
import ar.unsam.vv.atalaya.dominio.TipoEstimacion;
import ar.unsam.vv.atalaya.config.RelojMutable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de preparación de pruebas. Solo existen con el perfil qa (CP-36).
 */
@RestController
@Profile("qa")
@RequestMapping("/api/v1/qa")
public class QaController {

    private final RelojMutable reloj;
    private final ServicioReset servicioReset;
    private final ServicioEstimaciones estimaciones;
    private final ServicioAlertas alertas;
    private final ServicioManiobras maniobras;

    public QaController(RelojMutable reloj, ServicioReset servicioReset, ServicioEstimaciones estimaciones,
                        ServicioAlertas alertas, ServicioManiobras maniobras) {
        this.reloj = reloj;
        this.servicioReset = servicioReset;
        this.estimaciones = estimaciones;
        this.alertas = alertas;
        this.maniobras = maniobras;
    }

    /** Inyecta una estimación como si viniera del modelo (RF-06). Genera alerta si corresponde. */
    @PostMapping("/estimaciones")
    public EstimacionVista inyectar(@Valid @RequestBody EstimacionSolicitud solicitud) {
        List<Factor> factores = solicitud.factores() == null ? List.of()
                : solicitud.factores().stream().map(f -> new Factor(f.nombre(), f.peso())).toList();
        return estimaciones.registrar(new NuevaEstimacion(solicitud.tipo(), solicitud.probabilidad(), factores));
    }

    /** Avanza el reloj (acepta décimas: CP-09) y evalúa escalamiento y expiración en el momento. */
    @PostMapping("/reloj")
    public EstadoReloj avanzar(@Valid @RequestBody AvanceReloj avance) {
        long millis = avance.segundos().movePointRight(3).longValueExact();
        reloj.avanzar(Duration.ofMillis(millis));
        alertas.evaluarEscalamientos();
        maniobras.evaluarExpiraciones();
        return new EstadoReloj(reloj.instant());
    }

    /** Limpia la base y vuelve el reloj a T0 (precondición "Base"). */
    @PostMapping("/reset")
    public EstadoReloj reset() {
        servicioReset.limpiar();
        reloj.reiniciar();
        return new EstadoReloj(reloj.instant());
    }

    public record EstimacionSolicitud(
            @NotNull(message = "El tipo es obligatorio") TipoEstimacion tipo,
            @NotNull(message = "La probabilidad es obligatoria") BigDecimal probabilidad,
            List<@Valid FactorSolicitud> factores) {
    }

    public record FactorSolicitud(@NotBlank String nombre, @NotNull BigDecimal peso) {
    }

    public record AvanceReloj(@NotNull @DecimalMin("0") BigDecimal segundos) {
    }

    public record EstadoReloj(Instant ahora) {
    }
}
