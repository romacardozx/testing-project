package ar.unsam.vv.atalaya.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * RF-12: alto entre 30 y 50, crítico entre 65 y 100 (A8), ambos inclusivos, y alto menor que crítico.
 * Límites como campos de instancia para que PIT pueda mutar 30 → 31 (sección 10.2).
 */
public class ValidadorUmbrales {

    private final BigDecimal altoMinimo = BigDecimal.valueOf(30);
    private final BigDecimal altoMaximo = BigDecimal.valueOf(50);
    private final BigDecimal criticoMinimo = BigDecimal.valueOf(65);
    private final BigDecimal criticoMaximo = BigDecimal.valueOf(100);

    public void validar(Umbrales umbrales) {
        Objects.requireNonNull(umbrales.alto(), "alto");
        Objects.requireNonNull(umbrales.critico(), "critico");
        if (fuera(umbrales.alto(), altoMinimo, altoMaximo)) {
            throw new UmbralesInvalidosException("alto", "El umbral alto debe estar entre 30 y 50");
        }
        if (fuera(umbrales.critico(), criticoMinimo, criticoMaximo)) {
            throw new UmbralesInvalidosException("critico", "El umbral crítico debe estar entre 65 y 100");
        }
        if (umbrales.alto().compareTo(umbrales.critico()) >= 0) {
            throw new UmbralesInvalidosException("alto", "El umbral alto debe ser menor que el crítico");
        }
    }

    private static boolean fuera(BigDecimal valor, BigDecimal minimo, BigDecimal maximo) {
        return valor.compareTo(minimo) < 0 || valor.compareTo(maximo) > 0;
    }
}
