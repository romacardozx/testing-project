package ar.unsam.vv.atalaya.aplicacion;

/** No existe una alerta con ese id (→ 404). */
public class AlertaInexistenteException extends RuntimeException {

    public AlertaInexistenteException(long id) {
        super("No existe la alerta " + id);
    }
}
