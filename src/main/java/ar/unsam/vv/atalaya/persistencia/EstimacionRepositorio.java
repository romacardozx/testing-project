package ar.unsam.vv.atalaya.persistencia;

import ar.unsam.vv.atalaya.dominio.TipoEstimacion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstimacionRepositorio extends JpaRepository<EstimacionEntidad, Long> {

    Optional<EstimacionEntidad> findFirstByTipoOrderByIdDesc(TipoEstimacion tipo);
}
