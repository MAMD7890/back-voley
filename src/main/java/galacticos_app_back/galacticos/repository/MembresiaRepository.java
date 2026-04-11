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
    List<Membresia> findByFechaInicioBetween(LocalDate desde, LocalDate hasta);

    /**
     * Busca membresías activas cuya fecha de vencimiento coincide con una fecha exacta.
     */
    @Query("SELECT m FROM Membresia m WHERE m.estado = true AND m.fechaFin = :fecha AND m.estudiante.estado = true")
    List<Membresia> findMembresiasActivasPorFechaVencimiento(@Param("fecha") LocalDate fecha);

    /**
     * Busca membresías con fecha de vencimiento en un rango (para ventana de notificación).
     */
    @Query("SELECT m FROM Membresia m WHERE m.fechaFin BETWEEN :desde AND :hasta AND m.estudiante.estado = true")
    List<Membresia> findMembresiasPorRangoVencimiento(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /**
     * Busca membresías activas cuyo fechaFin ya pasó y el estudiante NO está en EN_MORA, COMPROMISO_PAGO ni DECLINADO.
     * Se usa para actualizar el estado a EN_MORA a medianoche.
     * Solo considera membresías con estado = true (activadas por pago real).
     * Las membresías ficticias creadas en importación Excel tienen estado = false y se excluyen.
     * Los DECLINADO se procesan por separado con ventana de gracia de 5 días.
     */
    @Query("SELECT m FROM Membresia m " +
           "WHERE m.estado = true " +
           "AND m.fechaFin < :hoy " +
           "AND m.estudiante.estado = true " +
           "AND m.estudiante.estadoPago NOT IN ('EN_MORA', 'COMPROMISO_PAGO', 'DECLINADO') " +
           "AND NOT EXISTS (" +
           "    SELECT m2 FROM Membresia m2 " +
           "    WHERE m2.estudiante = m.estudiante " +
           "    AND m2.estado = true " +
           "    AND m2.fechaFin >= :hoy" +
           ") " +
           "ORDER BY m.estudiante.idEstudiante ASC")
    List<Membresia> findMembresiasVencidasSinMora(@Param("hoy") LocalDate hoy);

    /**
     * Busca membresías activas de estudiantes DECLINADO cuyo fechaFin superó la ventana de gracia (5 días).
     * Pasados 5 días sin pago desde el vencimiento, se pasa a EN_MORA.
     */
    @Query("SELECT m FROM Membresia m " +
           "WHERE m.estado = true " +
           "AND m.fechaFin < :limite " +
           "AND m.estudiante.estado = true " +
           "AND m.estudiante.estadoPago = 'DECLINADO' " +
           "ORDER BY m.estudiante.idEstudiante ASC")
    List<Membresia> findMembresiasDeclinadasVencidas(@Param("limite") LocalDate limite);

    /**
     * Busca membresías en ventana de notificación pre-vencimiento (-5 a 0 días).
     * La membresía debe estar activa.
     */
    @Query("SELECT m FROM Membresia m " +
           "WHERE m.estado = true " +
           "AND m.fechaFin BETWEEN :hoy AND :limite " +
           "AND m.estudiante.estado = true")
    List<Membresia> findMembresiasProximasAVencer(
            @Param("hoy") LocalDate hoy,
            @Param("limite") LocalDate limite);

    /**
     * Busca membresías vencidas en los últimos N días (para recordatorios post-vencimiento).
     */
    @Query("SELECT m FROM Membresia m " +
           "WHERE m.fechaFin BETWEEN :desde AND :hasta " +
           "AND m.estudiante.estado = true")
    List<Membresia> findMembresiasParaNotificacion(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
