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

    // Membresía marcada explícitamente como activa (solo una por estudiante)
    Optional<MembresiaCore> findByEstudianteIdEstudianteAndEsActivaTrue(Integer idEstudiante);

    // Para desactivar la activa anterior antes de asignar una nueva
    @Query("SELECT m FROM MembresiaCore m WHERE m.estudiante.idEstudiante = :idEstudiante AND m.esActiva = true")
    List<MembresiaCore> findActivas(@Param("idEstudiante") Integer idEstudiante);

    // Última FINALIZADA del estudiante (para restaurar tras Job4 y base de continuidad de mora)
    @Query("SELECT m FROM MembresiaCore m WHERE m.estudiante.idEstudiante = :idEstudiante " +
           "AND m.estadoMembresia = 'FINALIZADA' ORDER BY m.fechaFin DESC")
    List<MembresiaCore> findFinalizadasDeEstudiante(@Param("idEstudiante") Integer idEstudiante);

    // Unicidad: verificar que un pago no esté ya vinculado a otra membresía
    boolean existsByPagoOrigenIdPago(Integer idPago);

    // Sincronizar estados: todas las membresías marcadas como activas
    List<MembresiaCore> findAllByEsActivaTrue();
}
