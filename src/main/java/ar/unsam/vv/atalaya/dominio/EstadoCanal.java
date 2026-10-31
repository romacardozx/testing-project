package ar.unsam.vv.atalaya.dominio;

/** Estado de un canal tal como se informa en GET /magnitudes/{magnitud}. */
public record EstadoCanal(Canal canal, EstadoConfianza estado, MotivoNoConfiable motivo) {
}
