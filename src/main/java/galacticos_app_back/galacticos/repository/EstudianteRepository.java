package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Integer> {
    List<Estudiante> findByEstado(Boolean estado);
    List<Estudiante> findBySedeIdSede(Integer idSede);
    List<Estudiante> findByNivelActual(Estudiante.NivelActual nivel);
    
    // Métodos para filtrar por estado de pago
    List<Estudiante> findByEstadoPago(Estudiante.EstadoPago estadoPago);
    List<Estudiante> findByEstadoAndEstadoPago(Boolean estado, Estudiante.EstadoPago estadoPago);
    List<Estudiante> findBySedeIdSedeAndEstadoPago(Integer idSede, Estudiante.EstadoPago estadoPago);
    
    // Buscar por documento
    Optional<Estudiante> findByNumeroDocumento(String numeroDocumento);
}
