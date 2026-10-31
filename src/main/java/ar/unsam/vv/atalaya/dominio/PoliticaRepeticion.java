package ar.unsam.vv.atalaya.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * RF-07: decide si una estimación genera una alerta nueva para su tipo.
 *
 * <ul>
 *   <li>NORMAL nunca genera alerta.</li>
 *   <li>Sin alerta previa del tipo → se emite.</li>
 *   <li>Nivel mayor que la última → se emite siempre (A4).</li>
 *   <li>Mismo nivel o menor → solo si pasaron 180 s o más desde la última emisión.</li>
 * </ul>
 */
public class PoliticaRepeticion {

    private final Duration ventana = Duration.ofSeconds(180);

    public boolean debeEmitir(Optional<UltimaAlerta> ultima, NivelRiesgo nivel, Instant ahora) {
        if (nivel == NivelRiesgo.NORMAL) {
            return false;
        }
        if (ultima.isEmpty() || nivel.esMayorQue(ultima.get().nivel())) {
            return true;
        }
        return Duration.between(ultima.get().emitidaEn(), ahora).compareTo(ventana) >= 0;
    }
}
