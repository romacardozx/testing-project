package ar.unsam.vv.atalaya.aplicacion;

/** Operación que choca con el estado actual del recurso (→ 409). */
public class ConflictoException extends RuntimeException {

    private final String codigo;

    public ConflictoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
