package ar.unsam.vv.atalaya.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/** RF-01/02: un valor es válido si está dentro del rango físico, con ambos extremos incluidos. */
public class ValidadorRango {

    public boolean dentroDeRango(Magnitud magnitud, BigDecimal valor) {
        Objects.requireNonNull(valor, "valor");
        return valor.compareTo(magnitud.minimo()) >= 0
                && valor.compareTo(magnitud.maximo()) <= 0;
    }
}
