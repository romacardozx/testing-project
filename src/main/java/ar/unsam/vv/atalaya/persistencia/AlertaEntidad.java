package ar.unsam.vv.atalaya.persistencia;

import ar.unsam.vv.atalaya.dominio.CanalAlerta;
import ar.unsam.vv.atalaya.dominio.ClaseAlerta;
import ar.unsam.vv.atalaya.dominio.DefinicionAlerta;
import ar.unsam.vv.atalaya.dominio.EstadoAlerta;
import ar.unsam.vv.atalaya.dominio.NivelRiesgo;
import ar.unsam.vv.atalaya.dominio.Rol;
import ar.unsam.vv.atalaya.dominio.SituacionAlerta;
import ar.unsam.vv.atalaya.dominio.TipoEstimacion;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Alerta de RF-07/08. {@code @Version} da optimistic locking: de dos reconocimientos simultáneos
 * solo uno puede guardar (A5, CP-25).
 */
@Entity
@Table(name = "alerta")
public class AlertaEntidad {

    @Id
    @GeneratedValue
    private Long id;

    @Version
    private long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaseAlerta clase;

    /** Null en las INFORMATIVA: agrupan estimaciones de cualquier tipo. */
    @Enumerated(EnumType.STRING)
    private TipoEstimacion tipo;

    /** Null en las INFORMATIVA. */
    @Enumerated(EnumType.STRING)
    private NivelRiesgo nivel;

    @Column(precision = 7, scale = 3)
    private BigDecimal probabilidad;

    /** Solo INFORMATIVA: maniobra que la originó y cantidad de estimaciones agrupadas. */
    private Long maniobraId;

    private int agrupadas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAlerta estado;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "alerta_canal")
    @OrderColumn(name = "orden")
    @Enumerated(EnumType.STRING)
    private List<CanalAlerta> canales = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "alerta_destinatario")
    @OrderColumn(name = "orden")
    @Enumerated(EnumType.STRING)
    private List<Rol> destinatarios = new ArrayList<>();

    private boolean requiereReconocimiento;

    @Column(nullable = false)
    private Instant emitidaEn;

    private Instant escaladaEn;

    protected AlertaEntidad() {
    }

    public AlertaEntidad(TipoEstimacion tipo, NivelRiesgo nivel, BigDecimal probabilidad,
                         DefinicionAlerta definicion, Instant emitidaEn) {
        this.clase = ClaseAlerta.RIESGO;
        this.tipo = tipo;
        this.nivel = nivel;
        this.probabilidad = probabilidad;
        this.estado = EstadoAlerta.EMITIDA;
        this.canales = new ArrayList<>(definicion.canales());
        this.destinatarios = new ArrayList<>(definicion.destinatarios());
        this.requiereReconocimiento = definicion.requiereReconocimiento();
        this.emitidaEn = emitidaEn;
    }

    /** Alerta INFORMATIVA de una maniobra: visual, sin destinatarios ni reconocimiento (RF-09). */
    public static AlertaEntidad informativa(long maniobraId, Instant emitidaEn) {
        var alerta = new AlertaEntidad();
        alerta.clase = ClaseAlerta.INFORMATIVA;
        alerta.maniobraId = maniobraId;
        alerta.estado = EstadoAlerta.EMITIDA;
        alerta.canales = new ArrayList<>(List.of(CanalAlerta.VISUAL));
        alerta.requiereReconocimiento = false;
        alerta.emitidaEn = emitidaEn;
        return alerta;
    }

    public void agrupar() {
        this.agrupadas++;
    }

    public SituacionAlerta situacion() {
        return new SituacionAlerta(nivel, estado, emitidaEn);
    }

    public void escalar(Rol destinatario, Instant ahora) {
        this.estado = EstadoAlerta.ESCALADA;
        this.destinatarios.add(destinatario);
        this.escaladaEn = ahora;
    }

    public void cambiarEstado(EstadoAlerta nuevo) {
        this.estado = nuevo;
    }

    public Long getId() {
        return id;
    }

    public ClaseAlerta getClase() {
        return clase;
    }

    public Long getManiobraId() {
        return maniobraId;
    }

    public int getAgrupadas() {
        return agrupadas;
    }

    public TipoEstimacion getTipo() {
        return tipo;
    }

    public NivelRiesgo getNivel() {
        return nivel;
    }

    public BigDecimal getProbabilidad() {
        return probabilidad;
    }

    public EstadoAlerta getEstado() {
        return estado;
    }

    public List<CanalAlerta> getCanales() {
        return List.copyOf(canales);
    }

    public List<Rol> getDestinatarios() {
        return List.copyOf(destinatarios);
    }

    public boolean isRequiereReconocimiento() {
        return requiereReconocimiento;
    }

    public Instant getEmitidaEn() {
        return emitidaEn;
    }

    public Instant getEscaladaEn() {
        return escaladaEn;
    }
}
