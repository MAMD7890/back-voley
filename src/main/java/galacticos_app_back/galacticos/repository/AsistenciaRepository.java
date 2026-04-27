package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.Asistencia;
import galacticos_app_back.galacticos.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {
    boolean existsByEstudianteAndFecha(Estudiante estudiante, LocalDate fecha);
    Optional<Asistencia> findByEstudianteAndFecha(Estudiante estudiante, LocalDate fecha);
    List<Asistencia> findByEstudianteIdEstudianteOrderByFechaDesc(Integer idEstudiante);
    List<Asistencia> findByFecha(LocalDate fecha);
}
