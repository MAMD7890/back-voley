package galacticos_app_back.galacticos.dto;

import galacticos_app_back.galacticos.entity.Estudiante;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para cambiar el estado de pago de un estudiante manualmente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambioEstadoPagoDTO {
    
    /**
     * Nuevo estado de pago del estudiante
     * Valores válidos: PENDIENTE, AL_DIA, EN_MORA, COMPROMISO_PAGO
     */
    private Estudiante.EstadoPago nuevoEstado;
    
    /**
     * Observación o motivo del cambio (opcional)
     */
    private String observacion;
    
    /**
     * ID del mes pagado si aplica (formato: "ENERO_2026", "FEBRERO_2026", etc.)
     */
    private String mesPagado;
}
