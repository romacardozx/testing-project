package ar.unsam.vv.atalaya.web.qa;

import ar.unsam.vv.atalaya.config.RelojMutable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de preparación de pruebas. Solo existen con el perfil qa (CP-36).
 * La evaluación de escalamiento y expiración y la limpieza de la base se suman con RF-08 y RF-09.
 */
@RestController
@Profile("qa")
@RequestMapping("/api/v1/qa")
public class QaController {

    private final RelojMutable reloj;

    public QaController(RelojMutable reloj) {
        this.reloj = reloj;
    }

    /** Avanza el reloj; acepta décimas de segundo (CP-09: 10,0 s y 10,1 s). */
    @PostMapping("/reloj")
    public EstadoReloj avanzar(@Valid @RequestBody AvanceReloj avance) {
        long millis = avance.segundos().movePointRight(3).longValueExact();
        reloj.avanzar(Duration.ofMillis(millis));
        return new EstadoReloj(reloj.instant());
    }

    /** Vuelve el reloj a T0. */
    @PostMapping("/reset")
    public EstadoReloj reset() {
        reloj.reiniciar();
        return new EstadoReloj(reloj.instant());
    }

    public record AvanceReloj(@NotNull @DecimalMin("0") BigDecimal segundos) {
    }

    public record EstadoReloj(Instant ahora) {
    }
}
