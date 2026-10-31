package ar.unsam.vv.atalaya.config;

import ar.unsam.vv.atalaya.aplicacion.ServicioAlertas;
import ar.unsam.vv.atalaya.aplicacion.ServicioManiobras;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Evalúa el escalamiento y la expiración de maniobras una vez por segundo con el reloj del sistema. En el perfil qa no corre: el
 * reloj solo avanza con POST /qa/reloj, que evalúa en el momento, y así las pruebas son repetibles.
 */
@Configuration
@EnableScheduling
@Profile("!qa")
public class TickEscalamiento {

    private final ServicioAlertas alertas;
    private final ServicioManiobras maniobras;

    public TickEscalamiento(ServicioAlertas alertas, ServicioManiobras maniobras) {
        this.alertas = alertas;
        this.maniobras = maniobras;
    }

    @Scheduled(fixedRate = 1000)
    public void tick() {
        alertas.evaluarEscalamientos();
        maniobras.evaluarExpiraciones();
    }
}
