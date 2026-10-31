package ar.unsam.vv.atalaya.aplicacion;

import ar.unsam.vv.atalaya.dominio.EstadoLectura;
import ar.unsam.vv.atalaya.dominio.MotivoNoConfiable;
import ar.unsam.vv.atalaya.dominio.RecuperacionCanal;
import ar.unsam.vv.atalaya.dominio.ValidadorRango;
import ar.unsam.vv.atalaya.dominio.ValidadorRedundancia;
import ar.unsam.vv.atalaya.persistencia.EstadoCanalEntidad;
import ar.unsam.vv.atalaya.persistencia.EstadoCanalRepositorio;
import ar.unsam.vv.atalaya.persistencia.LecturaEntidad;
import ar.unsam.vv.atalaya.persistencia.LecturaRepositorio;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-01/02 y RF-03: clasifica cada lectura y actualiza el estado del canal.
 *
 * <p>Orden de clasificación: DUPLICADA, FUERA_DE_ORDEN, DESCARTADA_RANGO, ACEPTADA. Una lectura vieja
 * no marca el canal aunque esté fuera de rango.
 */
@Service
public class ServicioLecturas {

    private final LecturaRepositorio lecturas;
    private final EstadoCanalRepositorio estados;
    private final SenalesMagnitud senales;
    private final ValidadorRango validadorRango;
    private final ValidadorRedundancia validadorRedundancia;
    private final Clock reloj;

    public ServicioLecturas(LecturaRepositorio lecturas, EstadoCanalRepositorio estados, SenalesMagnitud senales,
                            ValidadorRango validadorRango, ValidadorRedundancia validadorRedundancia, Clock reloj) {
        this.lecturas = lecturas;
        this.estados = estados;
        this.senales = senales;
        this.validadorRango = validadorRango;
        this.validadorRedundancia = validadorRedundancia;
        this.reloj = reloj;
    }

    @Transactional
    public EstadoLectura registrar(NuevaLectura lectura) {
        if (lecturas.existsByMagnitudAndCanalAndSecuencia(lectura.magnitud(), lectura.canal(), lectura.secuencia())) {
            return EstadoLectura.DUPLICADA;
        }
        Instant ahora = reloj.instant();
        EstadoCanalEntidad canal = estados.findByMagnitudAndCanal(lectura.magnitud(), lectura.canal())
                .orElseGet(() -> new EstadoCanalEntidad(lectura.magnitud(), lectura.canal()));

        EstadoLectura estado = clasificar(lectura, canal);
        lecturas.save(new LecturaEntidad(lectura.magnitud(), lectura.canal(), lectura.valor(),
                lectura.secuencia(), lectura.timestampOrigen(), ahora, estado));
        if (estado != EstadoLectura.FUERA_DE_ORDEN) {
            actualizarCanal(lectura, canal, estado, ahora);
        }
        return estado;
    }

    private EstadoLectura clasificar(NuevaLectura lectura, EstadoCanalEntidad canal) {
        if (canal.secuenciaVigente() != null && lectura.secuencia() < canal.secuenciaVigente()) {
            return EstadoLectura.FUERA_DE_ORDEN;
        }
        if (!validadorRango.dentroDeRango(lectura.magnitud(), lectura.valor())) {
            return EstadoLectura.DESCARTADA_RANGO;
        }
        return EstadoLectura.ACEPTADA;
    }

    /**
     * Si el canal venía sin actualizar más de 10 s, primero queda marcado por SIN_ACTUALIZACION; después
     * la lectura nueva se evalúa con las reglas 1 a 3 y cuenta para la recuperación (A3).
     */
    private void actualizarCanal(NuevaLectura lectura, EstadoCanalEntidad canal, EstadoLectura estado, Instant ahora) {
        RecuperacionCanal recuperacion = canal.recuperacion();
        if (canal.senal().ultimaRecepcion() != null) {
            var previo = validadorRedundancia.evaluar(lectura.magnitud(), canal.senal(),
                    senales.de(lectura.magnitud()), ahora);
            if (previo.filter(MotivoNoConfiable.SIN_ACTUALIZACION::equals).isPresent()) {
                recuperacion = validadorRedundancia.registrarLectura(recuperacion, previo);
            }
        }

        if (estado == EstadoLectura.ACEPTADA) {
            canal.registrarAceptada(lectura.valor(), lectura.secuencia(), ahora);
        } else {
            canal.registrarFueraDeRango(ahora);
        }
        estados.save(canal);

        var actual = validadorRedundancia.evaluar(lectura.magnitud(), canal.senal(),
                senales.de(lectura.magnitud()), ahora);
        canal.aplicar(validadorRedundancia.registrarLectura(recuperacion, actual));
    }
}
