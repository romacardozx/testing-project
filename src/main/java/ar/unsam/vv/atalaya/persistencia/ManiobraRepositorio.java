package ar.unsam.vv.atalaya.persistencia;

import ar.unsam.vv.atalaya.dominio.EstadoManiobra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManiobraRepositorio extends JpaRepository<ManiobraEntidad, Long> {

    List<ManiobraEntidad> findByEstado(EstadoManiobra estado);
}
