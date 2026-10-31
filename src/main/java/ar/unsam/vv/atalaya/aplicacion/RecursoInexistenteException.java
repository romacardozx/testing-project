package ar.unsam.vv.atalaya.aplicacion;

/** El recurso pedido no existe (→ 404). */
public class RecursoInexistenteException extends RuntimeException {

    private final String codigo;

    public RecursoInexistenteException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
