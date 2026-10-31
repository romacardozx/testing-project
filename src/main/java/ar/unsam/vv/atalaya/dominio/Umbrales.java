package ar.unsam.vv.atalaya.dominio;

import java.math.BigDecimal;

/** Umbrales de nivel alto y crítico, en % de probabilidad. */
public record Umbrales(BigDecimal alto, BigDecimal critico) {

    public static final Umbrales POR_DEFECTO = new Umbrales(BigDecimal.valueOf(40), BigDecimal.valueOf(75));
}
