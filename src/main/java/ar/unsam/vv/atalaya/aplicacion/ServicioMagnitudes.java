package ar.unsam.vv.atalaya.aplicacion;

import ar.unsam.vv.atalaya.dominio.EstadoMagnitud;
import ar.unsam.vv.atalaya.dominio.Magnitud;
import ar.unsam.vv.atalaya.dominio.ValidadorRedundancia;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-03: estado consolidado de una magnitud en el instante actual del reloj. */
@Service
public class ServicioMagnitudes {

    private final SenalesMagnitud senales;
    private final ValidadorRedundancia validador;
    private final Clock reloj;

    public ServicioMagnitudes(SenalesMagnitud senales, ValidadorRedundancia validador, Clock reloj) {
        this.senales = senales;
        this.validador = validador;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public EstadoMagnitud consultar(Magnitud magnitud) {
        return validador.consolidar(magnitud, senales.de(magnitud), senales.recuperaciones(magnitud), reloj.instant());
    }
}
