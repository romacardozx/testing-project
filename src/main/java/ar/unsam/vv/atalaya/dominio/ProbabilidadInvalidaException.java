package ar.unsam.vv.atalaya.dominio;

import java.math.BigDecimal;

/** La probabilidad está fuera de [0, 100] (D1 de ClasificadorNivel). */
public class ProbabilidadInvalidaException extends RuntimeException {

    public ProbabilidadInvalidaException(BigDecimal probabilidad) {
        super("La probabilidad debe estar entre 0 y 100: " + probabilidad);
    }
}
