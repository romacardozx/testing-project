package ar.unsam.vv.atalaya.dominio;

import java.time.Instant;

/** Lo que el escalamiento necesita saber de una alerta. */
public record SituacionAlerta(NivelRiesgo nivel, EstadoAlerta estado, Instant emitidaEn) {
}
