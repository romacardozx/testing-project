package ar.unsam.vv.atalaya.dominio;

/** Reconocer una alerta que ya está RECONOCIDA (→ 409, CP-24). */
public class AlertaYaReconocidaException extends RuntimeException {

    public AlertaYaReconocidaException() {
        super("La alerta ya fue reconocida");
    }
}
