package galacticos_app_back.galacticos.repository;

import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Membresia;
import galacticos_app_back.galacticos.entity.RecordatorioPago;
import galacticos_app_back.galacticos.entity.RecordatorioPago.TipoRecordatorio;
import galacticos_app_back.galacticos.entity.RecordatorioPago.EstadoEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecordatorioPagoRepository extends JpaRepository<RecordatorioPago, Integer> {
    
    /**
     * Busca recordatorios por estudiante
     */
    List<RecordatorioPago> findByEstudiante(Estudiante estudiante);
    
    /**
     * Busca recordatorios por estudiante ordenados por fecha
     */
    List<RecordatorioPago> findByEstudianteOrderByFechaEnvioDesc(Estudiante estudiante);
    
    /**
     * Busca recordatorios por membresía
     */
    List<RecordatorioPago> findByMembresia(Membresia membresia);
    
    /**
     * Verifica si ya existe un recordatorio específico para evitar duplicados.
     * Busca por membresía, tipo de recordatorio y fecha de vencimiento de referencia.
     */
    boolean existsByMembresiaAndTipoRecordatorioAndFechaVencimientoReferencia(
            Membresia membresia, 
            TipoRecordatorio tipoRecordatorio, 
            LocalDate fechaVencimientoReferencia
    );
    
    /**
     * Busca un recordatorio específico
     */
    Optional<RecordatorioPago> findByMembresiaAndTipoRecordatorioAndFechaVencimientoReferencia(
            Membresia membresia, 
            TipoRecordatorio tipoRecordatorio, 
            LocalDate fechaVencimientoReferencia
    );
    
    /**
     * Busca recordatorios fallidos para reintento
     */
    List<RecordatorioPago> findByEstadoEnvioAndIntentosLessThan(EstadoEnvio estadoEnvio, Integer maxIntentos);
    
    /**
     * Cuenta recordatorios enviados en un rango de fechas
     */
    @Query("SELECT COUNT(r) FROM RecordatorioPago r WHERE r.fechaEnvio BETWEEN :desde AND :hasta AND r.estadoEnvio = :estado")
    long countByFechaEnvioBetweenAndEstadoEnvio(
            @Param("desde") LocalDateTime desde, 
            @Param("hasta") LocalDateTime hasta, 
            @Param("estado") EstadoEnvio estado
    );
    
    /**
     * Busca recordatorios por tipo y estado
     */
    List<RecordatorioPago> findByTipoRecordatorioAndEstadoEnvio(
            TipoRecordatorio tipoRecordatorio, 
            EstadoEnvio estadoEnvio
    );
    
    /**
     * Obtiene estadísticas de envíos del día actual
     */
    @Query("SELECT r.estadoEnvio, COUNT(r) FROM RecordatorioPago r " +
           "WHERE DATE(r.fechaEnvio) = CURRENT_DATE " +
           "GROUP BY r.estadoEnvio")
    List<Object[]> obtenerEstadisticasDelDia();
    
    /**
     * Busca recordatorios pendientes de reintento (fallidos con menos de X intentos)
     */
    @Query("SELECT r FROM RecordatorioPago r " +
           "WHERE r.estadoEnvio = 'FALLIDO' AND r.intentos < :maxIntentos " +
           "ORDER BY r.fechaEnvio ASC")
    List<RecordatorioPago> findRecordatoriosParaReintentar(@Param("maxIntentos") Integer maxIntentos);
}
