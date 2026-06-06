package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.AsistenciaV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaV2Repository extends JpaRepository<AsistenciaV2, Integer>,
        JpaSpecificationExecutor<AsistenciaV2> {

    Optional<AsistenciaV2> findBySesionIdSesionAndEstudianteIdEstudiante(
            Integer idSesion, Integer idEstudiante);

    List<AsistenciaV2> findBySesionIdSesionOrderByEstudianteNombreCompletoAsc(Integer idSesion);

    // Todas las asistencias de un estudiante ordenadas por fecha DESC
    @Query("SELECT a FROM AsistenciaV2 a WHERE a.estudiante.idEstudiante = :idEstudiante " +
           "ORDER BY a.sesion.fecha DESC")
    List<AsistenciaV2> findByEstudianteOrderByFechaDesc(@Param("idEstudiante") Integer idEstudiante);

    // Para el reporte con filtros opcionales
    @Query("SELECT a FROM AsistenciaV2 a " +
           "JOIN a.sesion s " +
           "JOIN a.estudiante e " +
           "WHERE (:desde IS NULL OR s.fecha >= :desde) " +
           "AND (:hasta IS NULL OR s.fecha <= :hasta) " +
           "AND (:idSede IS NULL OR e.sede.idSede = :idSede) " +
           "AND (:asistio IS NULL OR a.asistio = :asistio) " +
           "AND (:busqueda IS NULL OR " +
           "     LOWER(e.nombreCompleto) LIKE LOWER(CONCAT('%',:busqueda,'%')) OR " +
           "     e.numeroDocumento LIKE CONCAT('%',:busqueda,'%')) " +
           "ORDER BY s.fecha DESC, e.nombreCompleto ASC")
    List<AsistenciaV2> findWithFilters(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("idSede") Integer idSede,
            @Param("busqueda") String busqueda,
            @Param("asistio") Boolean asistio);
}
