package ar.unsam.vv.atalaya.dominio;

/** Niveles de riesgo de RF-06, de menor a mayor: el orden se usa para saber si una alerta empeora (A4). */
public enum NivelRiesgo {
    NORMAL,
    ATENCION,
    ALTO,
    CRITICO;

    public boolean esMayorQue(NivelRiesgo otro) {
        return compareTo(otro) > 0;
    }
}
