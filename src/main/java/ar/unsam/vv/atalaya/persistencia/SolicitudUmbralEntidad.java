package ar.unsam.vv.atalaya.persistencia;

import ar.unsam.vv.atalaya.dominio.EstadoSolicitud;
import ar.unsam.vv.atalaya.dominio.Umbrales;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Solicitud de cambio de umbrales: la pide el supervisor de SO y la aprueba el jefe de turno (RF-12). */
@Entity
@Table(name = "solicitud_umbral")
public class SolicitudUmbralEntidad {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, precision = 7, scale = 3)
    private BigDecimal alto;

    @Column(nullable = false, precision = 7, scale = 3)
    private BigDecimal critico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitud estado;

    @Column(nullable = false)
    private String solicitadaPor;

    @Column(nullable = false)
    private Instant solicitadaEn;

    private String aprobadaPor;

    private Instant aprobadaEn;

    protected SolicitudUmbralEntidad() {
    }

    public SolicitudUmbralEntidad(Umbrales umbrales, String solicitadaPor, Instant solicitadaEn) {
        this.alto = umbrales.alto();
        this.critico = umbrales.critico();
        this.estado = EstadoSolicitud.PENDIENTE;
        this.solicitadaPor = solicitadaPor;
        this.solicitadaEn = solicitadaEn;
    }

    public void aplicar(String usuario, Instant ahora) {
        this.estado = EstadoSolicitud.APLICADA;
        this.aprobadaPor = usuario;
        this.aprobadaEn = ahora;
    }

    public Umbrales umbrales() {
        return new Umbrales(alto, critico);
    }

    public Long getId() {
        return id;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public String getSolicitadaPor() {
        return solicitadaPor;
    }

    public String getAprobadaPor() {
        return aprobadaPor;
    }
}
