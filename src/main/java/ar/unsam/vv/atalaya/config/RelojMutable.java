package ar.unsam.vv.atalaya.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Reloj que solo avanza cuando se lo pide. Se usa en tests y en el perfil qa para probar
 * plazos (10 s, 45 s, 3 min, 30 min) sin esperar en tiempo real.
 */
public class RelojMutable extends Clock {

    /** Instante inicial de todas las pruebas (precondición "reloj en T0"). */
    public static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private volatile Instant ahora;
    private final ZoneId zona;

    public RelojMutable() {
        this(T0, ZoneOffset.UTC);
    }

    public RelojMutable(Instant inicio, ZoneId zona) {
        this.ahora = inicio;
        this.zona = zona;
    }

    public synchronized void avanzar(Duration duracion) {
        if (duracion.isNegative()) {
            throw new IllegalArgumentException("El reloj no retrocede");
        }
        ahora = ahora.plus(duracion);
    }

    public synchronized void reiniciar() {
        ahora = T0;
    }

    @Override
    public Instant instant() {
        return ahora;
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId otraZona) {
        return new RelojMutable(ahora, otraZona);
    }
}
