package ar.unsam.vv.atalaya.aplicacion;

import ar.unsam.vv.atalaya.dominio.Factor;
import ar.unsam.vv.atalaya.dominio.TipoEstimacion;
import java.math.BigDecimal;
import java.util.List;

/** Estimación inyectada por el perfil qa (el modelo real no existe en este alcance). */
public record NuevaEstimacion(TipoEstimacion tipo, BigDecimal probabilidad, List<Factor> factores) {

    public NuevaEstimacion {
        factores = factores == null ? List.of() : List.copyOf(factores);
    }
}
