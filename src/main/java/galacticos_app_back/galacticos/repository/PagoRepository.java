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
}
