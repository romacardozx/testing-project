package ar.unsam.vv.atalaya.persistencia;

import ar.unsam.vv.atalaya.dominio.Factor;
import ar.unsam.vv.atalaya.dominio.NivelRiesgo;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Estimación inyectada (RF-06), con el nivel calculado y sus factores principales. */
@Entity
@Table(name = "estimacion")
public class EstimacionEntidad {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEstimacion tipo;

    @Column(nullable = false, precision = 7, scale = 3)
    private BigDecimal probabilidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelRiesgo nivel;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "estimacion_factor")
    @OrderColumn(name = "orden")
    private List<FactorEmbebido> factores = new ArrayList<>();

    @Column(nullable = false)
    private Instant recibidaEn;

    protected EstimacionEntidad() {
    }

    public EstimacionEntidad(TipoEstimacion tipo, BigDecimal probabilidad, NivelRiesgo nivel,
                             List<Factor> factores, Instant recibidaEn) {
        this.tipo = tipo;
        this.probabilidad = probabilidad;
        this.nivel = nivel;
        this.factores = new ArrayList<>(factores.stream().map(FactorEmbebido::new).toList());
        this.recibidaEn = recibidaEn;
    }

    public TipoEstimacion getTipo() {
        return tipo;
    }

    public BigDecimal getProbabilidad() {
        return probabilidad;
    }

    public NivelRiesgo getNivel() {
        return nivel;
    }

    public List<Factor> getFactores() {
        return factores.stream().map(FactorEmbebido::aFactor).toList();
    }
}
