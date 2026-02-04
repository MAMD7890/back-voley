package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "recordatorio_pago", 
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"id_membresia", "tipo_recordatorio", "fecha_vencimiento_referencia"}
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordatorioPago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idRecordatorio;
    
    @ManyToOne
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;
    
    @ManyToOne
    @JoinColumn(name = "id_membresia")
    private Membresia membresia;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_recordatorio", length = 30)
    private TipoRecordatorio tipoRecordatorio;
    
    @Column(name = "fecha_vencimiento_referencia")
    private LocalDate fechaVencimientoReferencia;
    
    @Column(columnDefinition = "DATETIME")
    private LocalDateTime fechaEnvio;
    
    @Column(length = 500)
    private String mensaje;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoEnvio estadoEnvio;
    
    @Column(length = 100)
    private String twilioMessageSid;
    
    @Column(columnDefinition = "TEXT")
    private String errorDetalle;
    
    private Integer intentos;
    
    /**
     * Tipos de recordatorio según los días respecto a la fecha de vencimiento:
     * - CINCO_DIAS_ANTES: 5 días antes del vencimiento
     * - TRES_DIAS_ANTES: 3 días antes del vencimiento
     * - DIA_VENCIMIENTO: El día exacto del vencimiento
     * - TRES_DIAS_DESPUES: 3 días después del vencimiento
     * - CINCO_DIAS_DESPUES: 5 días después del vencimiento
     */
    public enum TipoRecordatorio {
        CINCO_DIAS_ANTES(-5, "5 días antes"),
        TRES_DIAS_ANTES(-3, "3 días antes"),
        DIA_VENCIMIENTO(0, "Día del vencimiento"),
        TRES_DIAS_DESPUES(3, "3 días después"),
        CINCO_DIAS_DESPUES(5, "5 días después");
        
        private final int diasDiferencia;
        private final String descripcion;
        
        TipoRecordatorio(int diasDiferencia, String descripcion) {
            this.diasDiferencia = diasDiferencia;
            this.descripcion = descripcion;
        }
        
        public int getDiasDiferencia() {
            return diasDiferencia;
        }
        
        public String getDescripcion() {
            return descripcion;
        }
        
        /**
         * Obtiene el tipo de recordatorio según la diferencia de días
         * @param diasDiferencia días respecto al vencimiento (negativo = antes, positivo = después)
         * @return TipoRecordatorio correspondiente o null si no aplica
         */
        public static TipoRecordatorio fromDiasDiferencia(int diasDiferencia) {
            for (TipoRecordatorio tipo : values()) {
                if (tipo.diasDiferencia == diasDiferencia) {
                    return tipo;
                }
            }
            return null;
        }
    }
    
    public enum EstadoEnvio {
        ENVIADO,
        FALLIDO,
        PENDIENTE
    }
    
    // Mantener compatibilidad con código existente
    @Deprecated
    public enum EstadoRecordatorio {
        ENVIADO, PENDIENTE
    }
}
