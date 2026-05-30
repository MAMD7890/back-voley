package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer>, JpaSpecificationExecutor<Pago> {
    List<Pago> findByEstudianteIdEstudiante(Integer idEstudiante);
    List<Pago> findByEstadoPago(Pago.EstadoPago estado);
    List<Pago> findByFechaPagoBetween(LocalDate desde, LocalDate hasta);
    
    // Métodos para integración con Wompi
    Optional<Pago> findFirstByReferenciaPagoOrderByIdPagoDesc(String referenciaPago);
    Optional<Pago> findByWompiTransactionId(String wompiTransactionId);
    
    // Buscar pagos pendientes por monto (para matching de webhooks)
    @Query("SELECT p FROM Pago p WHERE p.estadoPago = :estado AND p.valor = :valor ORDER BY p.idPago DESC")
    List<Pago> findByEstadoPagoAndValor(@Param("estado") Pago.EstadoPago estado, @Param("valor") BigDecimal valor);
    
    // Verificar si existe pago para un mes específico de un estudiante
    @Query("SELECT p FROM Pago p WHERE p.estudiante.idEstudiante = :idEstudiante AND p.mesPagado = :mesPagado AND p.estadoPago = 'PAGADO'")
    Optional<Pago> findPagoAprobadoByEstudianteAndMes(@Param("idEstudiante") Integer idEstudiante, @Param("mesPagado") String mesPagado);
    
    // Obtener el último pago de un estudiante
    @Query("SELECT p FROM Pago p WHERE p.estudiante.idEstudiante = :idEstudiante ORDER BY p.fechaPago DESC")
    List<Pago> findUltimoPagoByEstudiante(@Param("idEstudiante") Integer idEstudiante);

    // Pagos PAGADOS de un estudiante ordenados por fecha ASC (para migración)
    @Query("SELECT p FROM Pago p WHERE p.estudiante.idEstudiante = :idEstudiante " +
           "AND p.estadoPago = 'PAGADO' ORDER BY p.fechaPago ASC, p.idPago ASC")
    List<Pago> findPagadosByEstudianteOrderByFechaAsc(@Param("idEstudiante") Integer idEstudiante);
    
    // Obtener pagos del mes actual
    @Query("SELECT p FROM Pago p WHERE p.estudiante.idEstudiante = :idEstudiante AND p.mesPagado = :mesActual")
    List<Pago> findPagosMesActual(@Param("idEstudiante") Integer idEstudiante, @Param("mesActual") String mesActual);
    
    // Filtrar por método de pago (ONLINE/EFECTIVO)
    List<Pago> findByMetodoPago(Pago.MetodoPago metodoPago);
    
    // Pagos online ordenados por fecha descendente
    @Query("SELECT p FROM Pago p WHERE p.metodoPago = 'ONLINE' ORDER BY p.fechaPago DESC, p.horaPago DESC")
    List<Pago> findAllPagosOnline();
    
    // Contar pagos por estado
    long countByEstadoPago(Pago.EstadoPago estadoPago);
    
    // Contar pagos por método
    long countByMetodoPago(Pago.MetodoPago metodoPago);
    
    // Buscar todos los pagos con wompiTransactionId (para sincronización masiva)
    @Query("SELECT p FROM Pago p WHERE p.wompiTransactionId IS NOT NULL ORDER BY p.idPago DESC")
    List<Pago> findAllWithWompiTransactionId();
    
    // Buscar pagos pendientes ONLINE (para sincronización con Wompi)
    @Query("SELECT p FROM Pago p WHERE p.estadoPago = 'PENDIENTE' AND p.metodoPago = 'ONLINE' ORDER BY p.idPago DESC")
    List<Pago> findPagosPendientesOnline();

    /**
     * Busca pagos APROBADOS de un estudiante dentro de un rango de fechas
     * con valor mayor o igual al mínimo esperado.
     * Usado para corroborar que una membresía tiene respaldo real de pago.
     */
    @Query("SELECT p FROM Pago p " +
           "WHERE p.estudiante.idEstudiante = :idEstudiante " +
           "AND p.estadoPago = 'PAGADO' " +
           "AND p.valor >= :valorMinimo " +
           "AND p.fechaPago BETWEEN :desde AND :hasta " +
           "ORDER BY p.fechaPago DESC")
    List<Pago> findPagosAprobadosEnRango(
            @Param("idEstudiante") Integer idEstudiante,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("valorMinimo") BigDecimal valorMinimo);

    /**
     * Busca pagos PAGADOS generados por esta app (referencia empieza con PAY-)
     * de un estudiante dentro de un rango de fechas.
     * Excluye pagos de links externos de Wompi (uniformes, etc.).
     */
    @Query("SELECT p FROM Pago p " +
           "WHERE p.estudiante.idEstudiante = :idEstudiante " +
           "AND p.estadoPago = 'PAGADO' " +
           "AND p.referenciaPago LIKE 'PAY-%' " +
           "AND p.fechaPago BETWEEN :desde AND :hasta " +
           "ORDER BY p.fechaPago ASC")
    List<Pago> findPagosAppEnRango(
            @Param("idEstudiante") Integer idEstudiante,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /**
     * Busca todos los pagos PAGADOS que no pertenecen a esta app
     * (referenciaPago nula o que no empieza con PAY-).
     * Estos son pagos de links externos de Wompi (uniformes, otros) que
     * pudieron haberse registrado incorrectamente como membresías.
     */
    @Query("SELECT p FROM Pago p " +
           "WHERE p.estadoPago = 'PAGADO' " +
           "AND (p.referenciaPago IS NULL OR p.referenciaPago NOT LIKE 'PAY-%')")
    List<Pago> findPagosExternos();

    // Corrección de duplicados: referencias PAY- que aparecen más de una vez
    @Query("SELECT p.referenciaPago FROM Pago p " +
           "WHERE p.referenciaPago LIKE 'PAY-%' " +
           "GROUP BY p.referenciaPago HAVING COUNT(p) > 1")
    List<String> findReferenciasConDuplicados();

    // Todos los pagos de una referencia, del más antiguo al más reciente
    List<Pago> findByReferenciaPagoOrderByIdPagoAsc(String referenciaPago);

    // Pagos ONLINE PAGADOS sin membresía asociada (huérfanos por bug de webhook)
    @Query("SELECT p FROM Pago p " +
           "WHERE p.estadoPago = 'PAGADO' " +
           "AND p.metodoPago = 'ONLINE' " +
           "AND NOT EXISTS (SELECT m FROM MembresiaCore m WHERE m.pagoOrigen = p) " +
           "ORDER BY p.fechaPago DESC, p.idPago DESC")
    List<Pago> findPagadosOnlineSinMembresia();

    // Pagos PAGADOS (ONLINE o EFECTIVO) de un estudiante sin membresía vinculada
    @Query("SELECT p FROM Pago p " +
           "WHERE p.estudiante.idEstudiante = :idEstudiante " +
           "AND p.estadoPago = 'PAGADO' " +
           "AND NOT EXISTS (SELECT m FROM MembresiaCore m WHERE m.pagoOrigen = p) " +
           "ORDER BY p.fechaPago DESC, p.idPago DESC")
    List<Pago> findPagadosSinMembresiaByEstudiante(@Param("idEstudiante") Integer idEstudiante);
}
