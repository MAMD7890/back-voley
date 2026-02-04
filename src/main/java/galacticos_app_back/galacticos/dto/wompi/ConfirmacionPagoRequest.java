package galacticos_app_back.galacticos.dto.wompi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para solicitar la confirmación de un pago
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmacionPagoRequest {
    
    // Identificador de la transacción en Wompi
    private String transactionId;
    
    // Referencia del pago generada por el backend
    private String reference;
    
    // ID del estudiante (opcional, para validación adicional)
    private Integer idEstudiante;
    
    // Monto esperado (opcional, para validación)
    private BigDecimal monto;
    
    // Mes pagado (opcional)
    private String mesPagado;
}
