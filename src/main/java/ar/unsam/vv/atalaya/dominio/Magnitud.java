package ar.unsam.vv.atalaya.dominio;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/** Magnitudes de planta con su rango físico (RF-01/02). Los extremos son inclusivos. */
public enum Magnitud {
    TEMPERATURA_PRIMARIO(20, 350, "°C"),
    PRESION_PRIMARIO(0, 180, "bar"),
    NIVEL_PRESURIZADOR(0, 100, "%"),
    POTENCIA_TERMICA(0, 120, "% nominal"),
    TASA_DOSIS(0, 10000, "µSv/h");

    private final BigDecimal minimo;
    private final BigDecimal maximo;
    private final String unidad;

    Magnitud(int minimo, int maximo, String unidad) {
        this.minimo = BigDecimal.valueOf(minimo);
        this.maximo = BigDecimal.valueOf(maximo);
        this.unidad = unidad;
    }

    public BigDecimal minimo() {
        return minimo;
    }

    public BigDecimal maximo() {
        return maximo;
    }

    public String unidad() {
        return unidad;
    }

    /** Ancho del rango físico; base del 2 % de discrepancia (A2). */
    public BigDecimal rangoFisico() {
        return maximo.subtract(minimo);
    }

    /** Busca por nombre sin distinguir mayúsculas (temperatura_primario = TEMPERATURA_PRIMARIO). */
    public static Optional<Magnitud> desde(String nombre) {
        return Arrays.stream(values())
                .filter(m -> m.name().equalsIgnoreCase(nombre))
                .findFirst();
    }
}
