package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.AsistenciaEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AsistenciaEstudianteRepository extends JpaRepository<AsistenciaEstudiante, Integer> {
    
    // Buscar por fecha
    List<AsistenciaEstudiante> findByFecha(LocalDate fecha);
    
    // Buscar por equipo
    List<AsistenciaEstudiante> findByEquipoIdEquipo(Integer idEquipo);
    
    // Buscar por estudiante
    List<AsistenciaEstudiante> findByEstudianteIdEstudiante(Integer idEstudiante);
    
    // Buscar por equipo y fecha
    List<AsistenciaEstudiante> findByEquipoIdEquipoAndFecha(Integer idEquipo, LocalDate fecha);
    
    // Buscar por estudiante y fecha
    List<AsistenciaEstudiante> findByEstudianteIdEstudianteAndFecha(Integer idEstudiante, LocalDate fecha);
    
    // Buscar por rango de fechas
    List<AsistenciaEstudiante> findByFechaBetween(LocalDate desde, LocalDate hasta);
    
    // Buscar por equipo y rango de fechas
    List<AsistenciaEstudiante> findByEquipoIdEquipoAndFechaBetween(Integer idEquipo, LocalDate desde, LocalDate hasta);
}
