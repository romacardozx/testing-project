package ar.unsam.vv.atalaya.dominio;

/** Umbrales fuera de los rangos permitidos (→ 422, CP-32). */
public class UmbralesInvalidosException extends RuntimeException {

    private final String campo;

    public UmbralesInvalidosException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
