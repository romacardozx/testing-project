package ar.unsam.vv.atalaya.persistencia;

import ar.unsam.vv.atalaya.dominio.Canal;
import ar.unsam.vv.atalaya.dominio.Magnitud;
import ar.unsam.vv.atalaya.dominio.MotivoNoConfiable;
import ar.unsam.vv.atalaya.dominio.RecuperacionCanal;
import ar.unsam.vv.atalaya.dominio.SenalCanal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;

/** Último estado conocido de un canal de una magnitud: señal vigente y memoria de recuperación. */
@Entity
@Table(name = "estado_canal", uniqueConstraints = @UniqueConstraint(
        name = "uk_estado_canal", columnNames = {"magnitud", "canal"}))
public class EstadoCanalEntidad {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Magnitud magnitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Canal canal;

    @Column(precision = 14, scale = 4)
    private BigDecimal valorVigente;

    private Long secuenciaVigente;

    private Instant ultimaRecepcion;

    private boolean ultimaFueraDeRango;

    private boolean noConfiable;

    @Enumerated(EnumType.STRING)
    private MotivoNoConfiable motivo;

    private int buenasSeguidas;

    protected EstadoCanalEntidad() {
    }

    public EstadoCanalEntidad(Magnitud magnitud, Canal canal) {
        this.magnitud = magnitud;
        this.canal = canal;
    }

    public SenalCanal senal() {
        return new SenalCanal(canal, valorVigente, ultimaRecepcion, ultimaFueraDeRango);
    }

    public RecuperacionCanal recuperacion() {
        return new RecuperacionCanal(noConfiable, motivo, buenasSeguidas);
    }

    public Long secuenciaVigente() {
        return secuenciaVigente;
    }

    public Canal canal() {
        return canal;
    }

    public void registrarAceptada(BigDecimal valor, long secuencia, Instant ahora) {
        this.valorVigente = valor;
        this.secuenciaVigente = secuencia;
        this.ultimaRecepcion = ahora;
        this.ultimaFueraDeRango = false;
    }

    public void registrarFueraDeRango(Instant ahora) {
        this.ultimaRecepcion = ahora;
        this.ultimaFueraDeRango = true;
    }

    public void aplicar(RecuperacionCanal recuperacion) {
        this.noConfiable = recuperacion.noConfiable();
        this.motivo = recuperacion.motivo();
        this.buenasSeguidas = recuperacion.buenasSeguidas();
    }
}
