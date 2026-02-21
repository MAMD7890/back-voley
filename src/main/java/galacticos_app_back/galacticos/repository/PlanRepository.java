package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {
    
    /**
     * Obtener todos los planes activos ordenados por visualización
     */
    List<Plan> findByActivoTrueOrderByOrdenVisualizacion();
    
    /**
     * Obtener un plan por nombre
     */
    Optional<Plan> findByNombre(String nombre);
    
    /**
     * Obtener planes por duración
     */
    List<Plan> findByDuracionMeses(Integer duracionMeses);
}
