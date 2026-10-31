package ar.unsam.vv.atalaya.persistencia;

import ar.unsam.vv.atalaya.dominio.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Quién reconoció una alerta y cuándo. Un solo reconocimiento por alerta (columna única, CP-25). */
@Entity
@Table(name = "reconocimiento")
public class ReconocimientoEntidad {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private Long alertaId;

    @Column(nullable = false)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private Instant instante;

    protected ReconocimientoEntidad() {
    }

    public ReconocimientoEntidad(Long alertaId, String usuario, Rol rol, Instant instante) {
        this.alertaId = alertaId;
        this.usuario = usuario;
        this.rol = rol;
        this.instante = instante;
    }
}
