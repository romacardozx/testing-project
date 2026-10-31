package ar.unsam.vv.atalaya.dominio;

import java.time.Instant;

/** Última alerta emitida de un tipo de estimación, para decidir si se repite. */
public record UltimaAlerta(NivelRiesgo nivel, Instant emitidaEn) {
}
