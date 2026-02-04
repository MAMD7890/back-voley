package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.ProfesorEquipo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfesorEquipoRepository extends JpaRepository<ProfesorEquipo, Integer> {
	List<ProfesorEquipo> findByEquipoIdEquipo(Integer idEquipo);
	List<ProfesorEquipo> findByEquipoSedeIdSede(Integer idSede);
	List<ProfesorEquipo> findByProfesorIdProfesor(Integer idProfesor);
}
