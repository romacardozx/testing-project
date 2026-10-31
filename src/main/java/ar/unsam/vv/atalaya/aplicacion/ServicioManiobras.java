package ar.unsam.vv.atalaya.aplicacion;

import ar.unsam.vv.atalaya.dominio.EstadoManiobra;
import ar.unsam.vv.atalaya.dominio.Magnitud;
import ar.unsam.vv.atalaya.dominio.ReglaManiobra;
import ar.unsam.vv.atalaya.persistencia.ManiobraEntidad;
import ar.unsam.vv.atalaya.persistencia.ManiobraRepositorio;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-09: declaración, finalización y expiración de maniobras. Hay como mucho una ACTIVA. */
@Service
public class ServicioManiobras {

    private final ManiobraRepositorio maniobras;
    private final ReglaManiobra regla;
    private final Clock reloj;

    public ServicioManiobras(ManiobraRepositorio maniobras, ReglaManiobra regla, Clock reloj) {
        this.maniobras = maniobras;
        this.regla = regla;
        this.reloj = reloj;
    }

    @Transactional
    public ManiobraVista declarar(Magnitud magnitud, BigDecimal bandaMin, BigDecimal bandaMax,
                                  int duracionMaximaMin, String usuario) {
        if (activa().isPresent()) {
            throw new ConflictoException("MANIOBRA_ACTIVA", "Ya hay una maniobra activa");
        }
        var maniobra = maniobras.save(new ManiobraEntidad(magnitud, bandaMin, bandaMax, duracionMaximaMin,
                reloj.instant(), usuario));
        return ManiobraVista.de(maniobra);
    }

    @Transactional
    public ManiobraVista finalizar(long id) {
        evaluarExpiraciones();
        var maniobra = maniobras.findById(id).orElseThrow(() ->
                new RecursoInexistenteException("MANIOBRA_INEXISTENTE", "No existe la maniobra " + id));
        if (maniobra.getEstado() != EstadoManiobra.ACTIVA) {
            throw new ConflictoException("MANIOBRA_NO_ACTIVA", "La maniobra está " + maniobra.getEstado());
        }
        maniobra.cambiarEstado(EstadoManiobra.FINALIZADA);
        return ManiobraVista.de(maniobra);
    }

    /** Maniobra activa en este instante; si ya venció la marca EXPIRADA (A6). */
    @Transactional
    public Optional<ManiobraEntidad> activa() {
        evaluarExpiraciones();
        return maniobras.findByEstado(EstadoManiobra.ACTIVA).stream().findFirst();
    }

    /** Corre en cada tick, al avanzar el reloj de QA y antes de usar la maniobra activa. */
    @Transactional
    public void evaluarExpiraciones() {
        var ahora = reloj.instant();
        maniobras.findByEstado(EstadoManiobra.ACTIVA).stream()
                .filter(m -> regla.expiro(m.getDeclaradaEn(), m.getDuracionMaximaMin(), ahora))
                .forEach(m -> m.cambiarEstado(EstadoManiobra.EXPIRADA));
    }
}
