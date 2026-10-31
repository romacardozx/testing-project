package ar.unsam.vv.atalaya.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * RF-08: escalamiento de alertas no reconocidas y reconocimiento. Desarrollado con TDD.
 *
 * <p>Los plazos son campos de instancia (no {@code static final}) para que PIT pueda mutar
 * 45 → 46 con INLINE_CONSTS (sección 10.2 del informe).
 */
public class ServicioEscalamiento {

    private final Duration plazoCritico = Duration.ofSeconds(45);
    private final Duration plazoAlto = Duration.ofSeconds(90);

    /** Devuelve el rol que se suma a los destinatarios si la alerta tiene que escalar ahora. */
    public Optional<Rol> evaluar(SituacionAlerta alerta, Instant ahora) {
        if (alerta.estado() != EstadoAlerta.EMITIDA) {                     // E1: solo escala una vez
            return Optional.empty();
        }
        Duration transcurrido = Duration.between(alerta.emitidaEn(), ahora);
        if (alerta.nivel() == NivelRiesgo.CRITICO) {                         // E2
            return transcurrido.compareTo(plazoCritico) >= 0                 // E3
                    ? Optional.of(Rol.SUPERVISOR_SO)
                    : Optional.empty();
        }
        if (alerta.nivel() == NivelRiesgo.ALTO) {                            // E4
            return transcurrido.compareTo(plazoAlto) >= 0                    // E5
                    ? Optional.of(Rol.JEFE_TURNO)
                    : Optional.empty();
        }
        return Optional.empty();
    }

    /** Estado de la alerta después de reconocerla. */
    public EstadoAlerta reconocer(EstadoAlerta actual) {
        if (actual == EstadoAlerta.RECONOCIDA) {                             // E6
            throw new AlertaYaReconocidaException();
        }
        return EstadoAlerta.RECONOCIDA;
    }
}
