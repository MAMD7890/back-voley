package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.MembresiaCore;
import galacticos_app_back.galacticos.entity.MembresiaCore.EstadoMembresia;
import galacticos_app_back.galacticos.entity.MembresiaCore.TipoMembresia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MembresiaCoreRepository extends JpaRepository<MembresiaCore, Integer> {

    List<MembresiaCore> findByEstudianteIdEstudianteOrderByFechaInicioDesc(Integer idEstudiante);

    // Buscar membresía convertible (mora / pendiente sin pago)
    @Query("SELECT m FROM MembresiaCore m WHERE m.estudiante.idEstudiante = :idEstudiante " +
           "AND m.tipoMembresia IN :tipos " +
           "AND m.estadoMembresia IN :estados " +
           "AND m.pagoOrigen IS NULL " +
           "ORDER BY m.fechaCreacion DESC")
    List<MembresiaCore> findConvertibles(
            @Param("idEstudiante") Integer idEstudiante,
            @Param("tipos") List<TipoMembresia> tipos,
            @Param("estados") List<EstadoMembresia> estados);

    // Job 1 — membresías PAGADA cuya fechaInicio es hoy
    @Query("SELECT m FROM MembresiaCore m WHERE m.estadoMembresia = 'PAGADA' AND m.fechaInicio = :hoy")
    List<MembresiaCore> findPagadasQueInicianHoy(@Param("hoy") LocalDate hoy);

    // Job 1 — membresía anterior del mismo estudiante que terminó ayer o hoy mismo
    @Query("SELECT m FROM MembresiaCore m WHERE m.estudiante.idEstudiante = :idEstudiante " +
           "AND m.estadoMembresia = 'PAGADA' AND m.fechaFin <= :hoy " +
           "ORDER BY m.fechaFin DESC")
    List<MembresiaCore> findPagadasVencidasDeEstudiante(
            @Param("idEstudiante") Integer idEstudiante,
            @Param("hoy") LocalDate hoy);

    // Job 2 — membresías PAGADA con fechaFin < hoy
    @Query("SELECT m FROM MembresiaCore m WHERE m.estadoMembresia = 'PAGADA' AND m.fechaFin < :hoy")
    List<MembresiaCore> findPagadasVencidas(@Param("hoy") LocalDate hoy);

    // Job 2 — verificar si el estudiante tiene otra membresía activa o en mora
    @Query("SELECT COUNT(m) FROM MembresiaCore m WHERE m.estudiante.idEstudiante = :idEstudiante " +
           "AND m.estadoMembresia IN ('PAGADA', 'EN_MORA') " +
           "AND m.idMembresiaCore <> :excluirId")
    long countMembresiasActivasOEnMora(
            @Param("idEstudiante") Integer idEstudiante,
            @Param("excluirId") Integer excluirId);

    // Job 3 — PENDIENTE_PAGO cuya gracia venció o cuya fechaFin ya pasó (pasan a EN_MORA)
    @Query("SELECT m FROM MembresiaCore m WHERE " +
           "m.estadoMembresia = 'PENDIENTE_PAGO' " +
           "AND m.tipoMembresia = 'PENDIENTE_REGISTRO' " +
           "AND m.pagoOrigen IS NULL " +
           "AND (m.fechaLimiteGracia < :hoy OR m.fechaFin < :hoy)")
    List<MembresiaCore> findPendientesConGraciaVencida(@Param("hoy") LocalDate hoy);

    // Uso general: todas las membresías con gracia vencida sin pago (reporte)
    @Query("SELECT m FROM MembresiaCore m WHERE " +
           "m.estadoMembresia IN ('EN_MORA', 'PENDIENTE_PAGO') " +
           "AND m.tipoMembresia IN ('MORA', 'PENDIENTE_REGISTRO') " +
           "AND m.fechaLimiteGracia < :hoy " +
           "AND m.pagoOrigen IS NULL")
    List<MembresiaCore> findGraciasVencidas(@Param("hoy") LocalDate hoy);

    // Job 4 — acuerdos pendientes con fecha límite vencida y sin pago
    @Query("SELECT m FROM MembresiaCore m WHERE " +
           "m.tipoMembresia = 'ACUERDO_PAGO' " +
           "AND m.estadoMembresia = 'PENDIENTE_PAGO' " +
           "AND m.fechaLimiteCompromiso < :hoy " +
           "AND m.pagoOrigen IS NULL")
    List<MembresiaCore> findAcuerdosVencidos(@Param("hoy") LocalDate hoy);

    // Verificar si estudiante tiene membresía PAGADA vigente
    @Query("SELECT COUNT(m) FROM MembresiaCore m WHERE m.estudiante.idEstudiante = :idEstudiante " +
           "AND m.estadoMembresia = 'PAGADA' AND m.fechaFin >= :hoy")
    long countMembresiasVigentes(
            @Param("idEstudiante") Integer idEstudiante,
            @Param("hoy") LocalDate hoy);

    // Última membresía PAGADA vigente del estudiante
    @Query("SELECT m FROM MembresiaCore m WHERE m.estudiante.idEstudiante = :idEstudiante " +
           "AND m.estadoMembresia = 'PAGADA' AND m.fechaFin >= :hoy " +
           "ORDER BY m.fechaFin DESC")
    List<MembresiaCore> findVigentesDeEstudiante(
            @Param("idEstudiante") Integer idEstudiante,
            @Param("hoy") LocalDate hoy);

    Optional<MembresiaCore> findFirstByEstudianteIdEstudianteAndTipoMembresiaAndEstadoMembresia(
            Integer idEstudiante,
            TipoMembresia tipo,
            EstadoMembresia estado);

    // Para desactivar la activa anterior antes de asignar una nueva
    @Query("SELECT m FROM MembresiaCore m WHERE m.estudiante.idEstudiante = :idEstudiante AND m.esActiva = true ORDER BY m.idMembresiaCore DESC")
    List<MembresiaCore> findActivas(@Param("idEstudiante") Integer idEstudiante);

    // Membresía activa del estudiante — si hay duplicados por inconsistencia de datos, retorna la más reciente
    default Optional<MembresiaCore> findByEstudianteIdEstudianteAndEsActivaTrue(Integer idEstudiante) {
        List<MembresiaCore> lista = findActivas(idEstudiante);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    // Última FINALIZADA del estudiante (para restaurar tras Job4 y base de continuidad de mora)
    @Query("SELECT m FROM MembresiaCore m WHERE m.estudiante.idEstudiante = :idEstudiante " +
           "AND m.estadoMembresia = 'FINALIZADA' ORDER BY m.fechaFin DESC")
    List<MembresiaCore> findFinalizadasDeEstudiante(@Param("idEstudiante") Integer idEstudiante);

    // Reversión: membresías PAGADA activas creadas en un rango horario con pago anterior a una fecha
    @Query("SELECT m FROM MembresiaCore m WHERE m.motivoCambio = 'PAGO_CONFIRMADO' " +
           "AND m.estadoMembresia = 'PAGADA' " +
           "AND m.esActiva = true " +
           "AND m.fechaCreacion >= :inicio AND m.fechaCreacion < :fin " +
           "AND m.pagoOrigen IS NOT NULL " +
           "AND m.pagoOrigen.fechaPago < :fechaPago")
    List<MembresiaCore> findActivasCreadasEnRangoConPagoAnteriorA(
            @Param("inicio") java.time.LocalDateTime inicio,
            @Param("fin") java.time.LocalDateTime fin,
            @Param("fechaPago") LocalDate fechaPago);

    // Reversión: FINALIZADA desactivada más reciente antes de una membresía dada
    @Query("SELECT m FROM MembresiaCore m WHERE m.estudiante.idEstudiante = :idEstudiante " +
           "AND m.estadoMembresia = 'FINALIZADA' " +
           "AND m.esActiva = false " +
           "AND m.idMembresiaCore < :idReferencia " +
           "ORDER BY m.idMembresiaCore DESC")
    List<MembresiaCore> findFinalizadasDesactivadasAntesDeId(
            @Param("idEstudiante") Integer idEstudiante,
            @Param("idReferencia") Integer idReferencia);

    // Unicidad: verificar que un pago no esté ya vinculado a otra membresía
    boolean existsByPagoOrigenIdPago(Integer idPago);

    // Sincronizar estados: todas las membresías marcadas como activas
    List<MembresiaCore> findAllByEsActivaTrue();

    // Corrección duplicados: membresías que apuntan a un pago específico
    List<MembresiaCore> findByPagoOrigenIdPago(Integer idPago);

    // Membresías migradas de un estudiante (para recalcular tras limpiar duplicados)
    List<MembresiaCore> findByEstudianteIdEstudianteAndMotivoCambio(
            Integer idEstudiante, String motivoCambio);

    // Corrección fechas: membresías con pago vinculado para recalcular fechaFin
    @Query("SELECT m FROM MembresiaCore m WHERE m.pagoOrigen IS NOT NULL " +
           "AND m.estadoMembresia IN ('PAGADA', 'FINALIZADA') " +
           "AND m.fechaInicio IS NOT NULL AND m.fechaFin IS NOT NULL")
    List<MembresiaCore> findConPagoOrigenParaCorreccionFechas();

    // Corrección tipo: EFECTIVO con pago ONLINE o ONLINE con pago EFECTIVO
    @Query("SELECT m FROM MembresiaCore m WHERE m.pagoOrigen IS NOT NULL " +
           "AND m.tipoMembresia IN ('EFECTIVO', 'ONLINE') " +
           "AND ((m.tipoMembresia = 'EFECTIVO' AND m.pagoOrigen.metodoPago = 'ONLINE') " +
           "  OR (m.tipoMembresia = 'ONLINE'   AND m.pagoOrigen.metodoPago = 'EFECTIVO'))")
    List<MembresiaCore> findConTipoIncorrecto();

    // Elegibilidad asistencia: tiene al menos una membresía con fechaFin dentro de los últimos 2 meses
    @Query("SELECT COUNT(m) FROM MembresiaCore m WHERE m.estudiante.idEstudiante = :idEstudiante AND m.fechaFin >= :corte")
    long countMembresiasRecientes(@Param("idEstudiante") Integer idEstudiante, @Param("corte") LocalDate corte);

    // Job 1 extendido: PAGADA vigente hoy pero que nunca se activó (Job 1 la perdió en su día)
    @Query("SELECT m FROM MembresiaCore m WHERE m.estadoMembresia = 'PAGADA' " +
           "AND m.esActiva = false " +
           "AND m.fechaInicio <= :hoy " +
           "AND m.fechaFin > :hoy")
    List<MembresiaCore> findPagadasVigentesNoActivas(@Param("hoy") LocalDate hoy);
}
