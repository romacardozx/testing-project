package ar.unsam.vv.atalaya.persistencia;

import ar.unsam.vv.atalaya.dominio.Canal;
import ar.unsam.vv.atalaya.dominio.Magnitud;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoCanalRepositorio extends JpaRepository<EstadoCanalEntidad, Long> {

    Optional<EstadoCanalEntidad> findByMagnitudAndCanal(Magnitud magnitud, Canal canal);

    List<EstadoCanalEntidad> findByMagnitud(Magnitud magnitud);
}
