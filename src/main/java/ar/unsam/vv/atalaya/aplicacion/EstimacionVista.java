package ar.unsam.vv.atalaya.aplicacion;

import ar.unsam.vv.atalaya.dominio.Factor;
import com.fasterxml.jackson.annotation.JsonInclude;
import ar.unsam.vv.atalaya.dominio.NivelRiesgo;
import ar.unsam.vv.atalaya.dominio.TipoEstimacion;
import java.math.BigDecimal;
import java.util.List;

/**
 * Estimación tal como se informa.
 *
 * @param alertaId alerta que generó, o null si no generó ninguna (NORMAL o repetición)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EstimacionVista(TipoEstimacion tipo, BigDecimal probabilidad, NivelRiesgo nivel,
                              List<Factor> factores, Long alertaId) {
}
