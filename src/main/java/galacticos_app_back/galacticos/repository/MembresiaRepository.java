package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.Membresia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MembresiaRepository extends JpaRepository<Membresia, Integer> {
    List<Membresia> findByEstudianteIdEstudiante(Integer idEstudiante);
    List<Membresia> findByEquipoIdEquipo(Integer idEquipo);
    List<Membresia> findByEstado(Boolean estado);
    List<Membresia> findByFechaInicioBetween(java.time.LocalDate desde, java.time.LocalDate hasta);
    
    /**
     * Busca membresías activas cuya fecha de vencimiento (fechaFin) coincide con una fecha específica.
     * Útil para encontrar membresías que vencen exactamente en una fecha dada.
     */
    @Query("SELECT m FROM Membresia m WHERE m.estado = true AND m.fechaFin = :fecha AND m.estudiante.estado = true")
    List<Membresia> findMembresiasActivasPorFechaVencimiento(@Param("fecha") LocalDate fecha);
    
    /**
     * Busca membresías con estudiantes activos que tienen fecha de vencimiento en un rango de fechas.
     * Incluye membresías activas e inactivas (vencidas pero no renovadas).
     */
    @Query("SELECT m FROM Membresia m WHERE m.fechaFin BETWEEN :desde AND :hasta AND m.estudiante.estado = true")
    List<Membresia> findMembresiasPorRangoVencimiento(
            @Param("desde") LocalDate desde, 
            @Param("hasta") LocalDate hasta
    );
    
    /**
     * Busca membresías vencidas (fecha fin pasada) con estudiantes activos.
     * Útil para enviar recordatorios post-vencimiento.
     */
    @Query("SELECT m FROM Membresia m WHERE m.fechaFin < :fecha AND m.estudiante.estado = true")
    List<Membresia> findMembresiasVencidas(@Param("fecha") LocalDate fecha);
    
    /**
     * Busca todas las membresías que requieren notificación:
     * - Estudiante activo
     * - Fecha de vencimiento dentro del rango de notificación (-5 a +5 días)
     */
    @Query("SELECT m FROM Membresia m " +
           "WHERE m.estudiante.estado = true " +
           "AND m.fechaFin BETWEEN :fechaDesde AND :fechaHasta")
    List<Membresia> findMembresiasParaNotificacion(
            @Param("fechaDesde") LocalDate fechaDesde, 
            @Param("fechaHasta") LocalDate fechaHasta
    );
}
