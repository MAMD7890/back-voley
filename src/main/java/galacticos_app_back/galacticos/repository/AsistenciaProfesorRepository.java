package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.AsistenciaProfesor;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AsistenciaProfesorRepository extends JpaRepository<AsistenciaProfesor, Integer> {
	boolean existsByProfesorIdProfesorAndFecha(Integer idProfesor, java.time.LocalDate fecha);
	List<AsistenciaProfesor> findByProfesorIdProfesorAndFechaBetween(Integer idProfesor, java.time.LocalDate desde, java.time.LocalDate hasta);
	List<AsistenciaProfesor> findByProfesorIdProfesorOrderByFechaDesc(Integer idProfesor);
	List<AsistenciaProfesor> findByFecha(java.time.LocalDate fecha);
}
