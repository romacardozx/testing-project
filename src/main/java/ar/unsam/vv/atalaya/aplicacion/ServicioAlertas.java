package ar.unsam.vv.atalaya.aplicacion;

import ar.unsam.vv.atalaya.dominio.ClaseAlerta;
import ar.unsam.vv.atalaya.dominio.DefinicionAlerta;
import ar.unsam.vv.atalaya.dominio.EstadoAlerta;
import ar.unsam.vv.atalaya.dominio.NivelRiesgo;
import ar.unsam.vv.atalaya.dominio.PoliticaRepeticion;
import ar.unsam.vv.atalaya.dominio.ReglaManiobra;
import ar.unsam.vv.atalaya.dominio.Rol;
import ar.unsam.vv.atalaya.dominio.ServicioEscalamiento;
import ar.unsam.vv.atalaya.dominio.TipoEstimacion;
import ar.unsam.vv.atalaya.dominio.UltimaAlerta;
import ar.unsam.vv.atalaya.persistencia.AlertaEntidad;
import ar.unsam.vv.atalaya.persistencia.AlertaRepositorio;
import ar.unsam.vv.atalaya.persistencia.ManiobraEntidad;
import ar.unsam.vv.atalaya.persistencia.ReconocimientoEntidad;
import ar.unsam.vv.atalaya.persistencia.ReconocimientoRepositorio;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-07/08: emisión, escalamiento y reconocimiento de alertas. */
@Service
public class ServicioAlertas {

    private final AlertaRepositorio alertas;
    private final ReconocimientoRepositorio reconocimientos;
    private final PoliticaRepeticion politicaRepeticion;
    private final ServicioEscalamiento escalamiento;
    private final ServicioManiobras maniobras;
    private final ServicioMagnitudes magnitudes;
    private final ReglaManiobra reglaManiobra;
    private final Clock reloj;

    public ServicioAlertas(AlertaRepositorio alertas, ReconocimientoRepositorio reconocimientos,
                           PoliticaRepeticion politicaRepeticion, ServicioEscalamiento escalamiento,
                           ServicioManiobras maniobras, ServicioMagnitudes magnitudes, ReglaManiobra reglaManiobra,
                           Clock reloj) {
        this.alertas = alertas;
        this.reconocimientos = reconocimientos;
        this.politicaRepeticion = politicaRepeticion;
        this.escalamiento = escalamiento;
        this.maniobras = maniobras;
        this.magnitudes = magnitudes;
        this.reglaManiobra = reglaManiobra;
        this.reloj = reloj;
    }

    /**
     * Durante una maniobra activa, si la estimación queda dentro de la banda se agrupa en la alerta
     * INFORMATIVA de la maniobra (RF-09). Si no, se emite una alerta normal si el nivel lo requiere y
     * la política de repetición lo permite (RF-07).
     */
    @Transactional
    public Optional<AlertaVista> procesar(TipoEstimacion tipo, BigDecimal probabilidad, NivelRiesgo nivel) {
        Instant ahora = reloj.instant();
        Optional<ManiobraEntidad> maniobra = maniobras.activa();
        if (maniobra.isPresent() && reglaManiobra.agrupa(nivel, maniobra.get().getBandaMin(),
                maniobra.get().getBandaMax(), magnitudes.consultar(maniobra.get().getMagnitud()))) {
            return Optional.of(agrupar(maniobra.get(), ahora));
        }
        Optional<UltimaAlerta> ultima = alertas.findFirstByTipoOrderByEmitidaEnDescIdDesc(tipo)
                .map(a -> new UltimaAlerta(a.getNivel(), a.getEmitidaEn()));
        if (!politicaRepeticion.debeEmitir(ultima, nivel, ahora)) {
            return Optional.empty();
        }
        DefinicionAlerta definicion = DefinicionAlerta.para(nivel).orElseThrow();
        AlertaEntidad alerta = alertas.save(new AlertaEntidad(tipo, nivel, probabilidad, definicion, ahora));
        return Optional.of(AlertaVista.de(alerta));
    }

    private AlertaVista agrupar(ManiobraEntidad maniobra, Instant ahora) {
        AlertaEntidad informativa = alertas.findByManiobraId(maniobra.getId())
                .orElseGet(() -> alertas.save(AlertaEntidad.informativa(maniobra.getId(), ahora)));
        informativa.agrupar();
        return AlertaVista.de(informativa);
    }

    /** Escala las alertas EMITIDA cuyo plazo venció. Corre en cada tick y al avanzar el reloj de QA. */
    @Transactional
    public void evaluarEscalamientos() {
        Instant ahora = reloj.instant();
        for (AlertaEntidad alerta : alertas.findByEstadoAndClaseOrderByIdAsc(EstadoAlerta.EMITIDA, ClaseAlerta.RIESGO)) {
            escalamiento.evaluar(alerta.situacion(), ahora).ifPresent(rol -> alerta.escalar(rol, ahora));
        }
    }

    @Transactional(readOnly = true)
    public List<AlertaVista> listar(Optional<EstadoAlerta> estado) {
        List<AlertaEntidad> resultado = estado
                .map(alertas::findByEstadoOrderByIdAsc)
                .orElseGet(alertas::findAllByOrderByIdAsc);
        return resultado.stream().map(AlertaVista::de).toList();
    }

    /**
     * Reconoce una alerta. Con dos reconocimientos simultáneos, el segundo falla por la versión de la
     * alerta o por la columna única de reconocimiento, y la API responde 409 (A5).
     */
    @Transactional
    public AlertaVista reconocer(long alertaId, String usuario, Rol rol) {
        AlertaEntidad alerta = alertas.findById(alertaId).orElseThrow(() -> new AlertaInexistenteException(alertaId));
        alerta.cambiarEstado(escalamiento.reconocer(alerta.getEstado()));
        reconocimientos.save(new ReconocimientoEntidad(alertaId, usuario, rol, reloj.instant()));
        alertas.saveAndFlush(alerta);
        return AlertaVista.de(alerta);
    }
}
