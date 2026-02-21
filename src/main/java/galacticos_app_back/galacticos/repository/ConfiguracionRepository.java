package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ConfiguracionRepository extends JpaRepository<Configuracion, Integer> {
    
    /**
     * Obtener configuración por clave
     */
    Optional<Configuracion> findByClave(String clave);
}
