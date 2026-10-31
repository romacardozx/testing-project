package ar.unsam.vv.atalaya.persistencia;

import ar.unsam.vv.atalaya.dominio.Canal;
import ar.unsam.vv.atalaya.dominio.Magnitud;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LecturaRepositorio extends JpaRepository<LecturaEntidad, Long> {

    boolean existsByMagnitudAndCanalAndSecuencia(Magnitud magnitud, Canal canal, long secuencia);
}
