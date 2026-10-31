package ar.unsam.vv.atalaya.persistencia;

import ar.unsam.vv.atalaya.dominio.EstadoSolicitud;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudUmbralRepositorio extends JpaRepository<SolicitudUmbralEntidad, Long> {

    Optional<SolicitudUmbralEntidad> findFirstByEstadoOrderByAprobadaEnDescIdDesc(EstadoSolicitud estado);
}
