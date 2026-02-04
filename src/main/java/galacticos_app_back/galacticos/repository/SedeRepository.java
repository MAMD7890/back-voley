package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.Sede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SedeRepository extends JpaRepository<Sede, Integer> {
    List<Sede> findByEstado(Boolean estado);
}
