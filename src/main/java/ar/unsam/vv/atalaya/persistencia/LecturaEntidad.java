package ar.unsam.vv.atalaya.persistencia;

import ar.unsam.vv.atalaya.dominio.Canal;
import ar.unsam.vv.atalaya.dominio.EstadoLectura;
import ar.unsam.vv.atalaya.dominio.Magnitud;
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

/**
 * Lectura recibida, con el estado con que se clasificó. Las DUPLICADA no se guardan: la restricción
 * única sobre magnitud + canal + secuencia lo garantiza también en la base (CP-04).
 */
@Entity
@Table(name = "lectura", uniqueConstraints = @UniqueConstraint(
        name = "uk_lectura_magnitud_canal_secuencia", columnNames = {"magnitud", "canal", "secuencia"}))
public class LecturaEntidad {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Magnitud magnitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Canal canal;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal valor;

    @Column(nullable = false)
    private long secuencia;

    @Column(nullable = false)
    private Instant timestampOrigen;

    @Column(nullable = false)
    private Instant recibidaEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoLectura estado;

    protected LecturaEntidad() {
    }

    public LecturaEntidad(Magnitud magnitud, Canal canal, BigDecimal valor, long secuencia,
                          Instant timestampOrigen, Instant recibidaEn, EstadoLectura estado) {
        this.magnitud = magnitud;
        this.canal = canal;
        this.valor = valor;
        this.secuencia = secuencia;
        this.timestampOrigen = timestampOrigen;
        this.recibidaEn = recibidaEn;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public EstadoLectura getEstado() {
        return estado;
    }
}
