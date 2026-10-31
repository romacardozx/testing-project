package ar.unsam.vv.atalaya.web.api;

/** La magnitud pedida en la ruta no existe (→ 404). */
public class MagnitudInexistenteException extends RuntimeException {

    public MagnitudInexistenteException(String nombre) {
        super("No existe la magnitud " + nombre);
    }
}
