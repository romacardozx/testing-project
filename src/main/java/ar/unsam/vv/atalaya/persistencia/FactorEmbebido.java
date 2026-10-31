package ar.unsam.vv.atalaya.persistencia;

import ar.unsam.vv.atalaya.dominio.Factor;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** Factor guardado dentro de una estimación. */
@Embeddable
public class FactorEmbebido {

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal peso;

    protected FactorEmbebido() {
    }

    public FactorEmbebido(Factor factor) {
        this.nombre = factor.nombre();
        this.peso = factor.peso();
    }

    public Factor aFactor() {
        return new Factor(nombre, peso);
    }
}
