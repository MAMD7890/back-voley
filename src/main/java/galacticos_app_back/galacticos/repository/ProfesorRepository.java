package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.Profesor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfesorRepository extends JpaRepository<Profesor, Integer> {
    List<Profesor> findByEstado(Boolean estado);
    List<Profesor> findByNombreContainingIgnoreCaseAndEstado(String nombre, Boolean estado);
}
