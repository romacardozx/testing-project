package ar.unsam.vv.atalaya.dominio;

/**
 * Memoria de un canal entre lecturas, para la recuperación (A3).
 *
 * @param noConfiable    true si el canal quedó marcado como no confiable
 * @param motivo         motivo con el que quedó marcado (null si es confiable)
 * @param buenasSeguidas lecturas consecutivas que cumplen las reglas desde que quedó marcado
 */
public record RecuperacionCanal(boolean noConfiable, MotivoNoConfiable motivo, int buenasSeguidas) {

    public static final RecuperacionCanal CONFIABLE = new RecuperacionCanal(false, null, 0);

    public static RecuperacionCanal marcado(MotivoNoConfiable motivo, int buenasSeguidas) {
        return new RecuperacionCanal(true, motivo, buenasSeguidas);
    }
}
