package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.MembresiaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MembresiaHistorialRepository extends JpaRepository<MembresiaHistorial, Integer> {

    List<MembresiaHistorial> findByEstudianteIdEstudianteOrderByFechaCambioDesc(Integer idEstudiante);

    List<MembresiaHistorial> findByMembresiaIdMembresiaOrderByFechaCambioDesc(Integer idMembresia);
}
