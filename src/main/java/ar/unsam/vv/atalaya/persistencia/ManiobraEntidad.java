package ar.unsam.vv.atalaya.persistencia;

import ar.unsam.vv.atalaya.dominio.EstadoManiobra;
import ar.unsam.vv.atalaya.dominio.Magnitud;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Maniobra declarada por el jefe de turno (RF-09). */
@Entity
@Table(name = "maniobra")
public class ManiobraEntidad {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Magnitud magnitud;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal bandaMin;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal bandaMax;

    private int duracionMaximaMin;

    @Column(nullable = false)
    private Instant declaradaEn;

    @Column(nullable = false)
    private String declaradaPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoManiobra estado;

    protected ManiobraEntidad() {
    }

    public ManiobraEntidad(Magnitud magnitud, BigDecimal bandaMin, BigDecimal bandaMax, int duracionMaximaMin,
                           Instant declaradaEn, String declaradaPor) {
        this.magnitud = magnitud;
        this.bandaMin = bandaMin;
        this.bandaMax = bandaMax;
        this.duracionMaximaMin = duracionMaximaMin;
        this.declaradaEn = declaradaEn;
        this.declaradaPor = declaradaPor;
        this.estado = EstadoManiobra.ACTIVA;
    }

    public void cambiarEstado(EstadoManiobra nuevo) {
        this.estado = nuevo;
    }

    public Long getId() {
        return id;
    }

    public Magnitud getMagnitud() {
        return magnitud;
    }

    public BigDecimal getBandaMin() {
        return bandaMin;
    }

    public BigDecimal getBandaMax() {
        return bandaMax;
    }

    public int getDuracionMaximaMin() {
        return duracionMaximaMin;
    }

    public Instant getDeclaradaEn() {
        return declaradaEn;
    }

    public String getDeclaradaPor() {
        return declaradaPor;
    }

    public EstadoManiobra getEstado() {
        return estado;
    }
}
