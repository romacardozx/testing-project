package ar.unsam.vv.atalaya.persistencia;

import ar.unsam.vv.atalaya.dominio.ClaseAlerta;
import ar.unsam.vv.atalaya.dominio.EstadoAlerta;
import ar.unsam.vv.atalaya.dominio.TipoEstimacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaRepositorio extends JpaRepository<AlertaEntidad, Long> {

    Optional<AlertaEntidad> findFirstByTipoOrderByEmitidaEnDescIdDesc(TipoEstimacion tipo);

    List<AlertaEntidad> findByEstadoOrderByIdAsc(EstadoAlerta estado);

    List<AlertaEntidad> findByEstadoAndClaseOrderByIdAsc(EstadoAlerta estado, ClaseAlerta clase);

    Optional<AlertaEntidad> findByManiobraId(Long maniobraId);

    List<AlertaEntidad> findAllByOrderByIdAsc();
}
