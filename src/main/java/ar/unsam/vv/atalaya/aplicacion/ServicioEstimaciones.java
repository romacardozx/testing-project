package ar.unsam.vv.atalaya.aplicacion;

import ar.unsam.vv.atalaya.dominio.ClasificadorNivel;
import ar.unsam.vv.atalaya.dominio.Factor;
import ar.unsam.vv.atalaya.dominio.NivelRiesgo;
import ar.unsam.vv.atalaya.dominio.TipoEstimacion;
import ar.unsam.vv.atalaya.persistencia.EstimacionEntidad;
import ar.unsam.vv.atalaya.persistencia.EstimacionRepositorio;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-06: clasifica y guarda estimaciones; dispara la generación de alertas de RF-07. */
@Service
public class ServicioEstimaciones {

    private final EstimacionRepositorio estimaciones;
    private final ClasificadorNivel clasificador;
    private final ServicioUmbrales umbrales;
    private final ServicioAlertas alertas;
    private final Clock reloj;

    public ServicioEstimaciones(EstimacionRepositorio estimaciones, ClasificadorNivel clasificador,
                                ServicioUmbrales umbrales, ServicioAlertas alertas, Clock reloj) {
        this.estimaciones = estimaciones;
        this.clasificador = clasificador;
        this.umbrales = umbrales;
        this.alertas = alertas;
        this.reloj = reloj;
    }

    /** Una probabilidad inválida lanza excepción antes de guardar: el nivel vigente no cambia (CP-14). */
    @Transactional
    public EstimacionVista registrar(NuevaEstimacion estimacion) {
        NivelRiesgo nivel = clasificador.clasificar(estimacion.probabilidad(), umbrales.vigentes());
        List<Factor> factores = clasificador.factoresPrincipales(nivel, estimacion.factores());
        estimaciones.save(new EstimacionEntidad(estimacion.tipo(), estimacion.probabilidad(), nivel,
                factores, reloj.instant()));
        Long alertaId = alertas.procesar(estimacion.tipo(), estimacion.probabilidad(), nivel)
                .map(AlertaVista::id)
                .orElse(null);
        return new EstimacionVista(estimacion.tipo(), estimacion.probabilidad(), nivel, factores, alertaId);
    }

    /** Última estimación de cada tipo que tenga alguna. */
    @Transactional(readOnly = true)
    public List<EstimacionVista> actuales() {
        return Arrays.stream(TipoEstimacion.values())
                .flatMap(tipo -> estimaciones.findFirstByTipoOrderByIdDesc(tipo).stream())
                .map(e -> new EstimacionVista(e.getTipo(), e.getProbabilidad(), e.getNivel(), e.getFactores(), null))
                .toList();
    }
}
