package ar.unsam.vv.atalaya.dominio;

import java.math.BigDecimal;

/** Factor que explica una estimación, con su peso. */
public record Factor(String nombre, BigDecimal peso) {
}
