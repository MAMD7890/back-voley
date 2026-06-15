package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.SesionAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionAsistenciaRepository extends JpaRepository<SesionAsistencia, Integer> {

    Optional<SesionAsistencia> findBySedeIdSedeAndEquipoIdEquipoAndFecha(
            Integer idSede, Integer idEquipo, LocalDate fecha);

    Optional<SesionAsistencia> findBySedeIdSedeAndEquipoIsNullAndFecha(
            Integer idSede, LocalDate fecha);

    List<SesionAsistencia> findBySedeIdSedeAndFechaBetweenOrderByFechaDesc(
            Integer idSede, LocalDate desde, LocalDate hasta);

    @Query("SELECT s FROM SesionAsistencia s WHERE s.equipo IS NULL ORDER BY s.sede.idSede ASC, s.fecha ASC, s.idSesion ASC")
    List<SesionAsistencia> findAllSinEquipoOrdenadas();

    @Query("SELECT COUNT(DISTINCT s.fecha) FROM SesionAsistencia s " +
           "WHERE s.sede.idSede = :idSede " +
           "AND (:desde IS NULL OR s.fecha >= :desde) " +
           "AND (:hasta IS NULL OR s.fecha <= :hasta)")
    long countSesionesBySede(@Param("idSede") Integer idSede,
                              @Param("desde") LocalDate desde,
                              @Param("hasta") LocalDate hasta);
}
