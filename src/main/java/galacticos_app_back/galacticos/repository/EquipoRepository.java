package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Integer> {
    List<Equipo> findByEstado(Boolean estado);
    List<Equipo> findBySedeIdSede(Integer idSede);
    List<Equipo> findByCategoriaIdCategoria(Integer idCategoria);
}
