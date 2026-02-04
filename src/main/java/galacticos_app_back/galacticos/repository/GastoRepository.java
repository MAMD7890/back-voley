package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.Gasto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GastoRepository extends JpaRepository<Gasto, Integer> {
    List<Gasto> findByFechaBetween(java.time.LocalDate desde, java.time.LocalDate hasta);
    List<Gasto> findBySedeIdSede(Integer idSede);
    List<Gasto> findByConcepto(String concepto);
    List<Gasto> findByDescripcionContainingIgnoreCase(String descripcion);
}
