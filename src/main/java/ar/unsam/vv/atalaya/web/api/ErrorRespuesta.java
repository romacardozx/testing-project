package ar.unsam.vv.atalaya.web.api;

/** Formato único de error de la API: { "codigo", "mensaje", "campo" }. */
public record ErrorRespuesta(String codigo, String mensaje, String campo) {
}
