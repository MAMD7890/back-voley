package galacticos_app_back.galacticos.dto;

import galacticos_app_back.galacticos.entity.RecordatorioPago.EstadoEnvio;
import galacticos_app_back.galacticos.entity.RecordatorioPago.TipoRecordatorio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para la transferencia de datos de recordatorios de pago.
 * Versión simplificada sin referencias circulares a entidades.
 * 
 * @author Galacticos App
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordatorioPagoDto {
    
    private Integer idRecordatorio;
    
    // Datos del estudiante (simplificados)
    private Integer idEstudiante;
    private String nombreEstudiante;
    private String telefonoEstudiante;
    
    // Datos de la membresía (simplificados)
    private Integer idMembresia;
    private String nombreEquipo;
    private LocalDate fechaVencimientoMembresia;
    
    // Datos del recordatorio
    private TipoRecordatorio tipoRecordatorio;
    private String descripcionTipo;
    private LocalDate fechaVencimientoReferencia;
    private LocalDateTime fechaEnvio;
    private String mensaje;
    private EstadoEnvio estadoEnvio;
    private String twilioMessageSid;
    private String errorDetalle;
    private Integer intentos;
}
