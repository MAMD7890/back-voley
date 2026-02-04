package galacticos_app_back.galacticos.dto.wompi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO para la respuesta de confirmación de pago
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmacionPagoResponse {
    
    // Estado general de la operación
    private boolean success;
    private String message;
    
    // Información de la transacción Wompi
    private String transactionId;
    private String reference;
    private String statusWompi;  // APPROVED, DECLINED, PENDING, etc.
    private String paymentMethod;
    private String paymentMethodType;
    
    // Información del pago en el sistema
    private Integer idPago;
    private BigDecimal monto;
    private String moneda;
    private LocalDate fechaPago;
    private LocalTime horaPago;
    private String estadoPago;  // PAGADO, PENDIENTE, VENCIDO
    private String mesPagado;
    
    // Información del estudiante
    private Integer idEstudiante;
    private String nombreEstudiante;
    private String estadoEstudiante;  // AL_DIA, EN_MORA, PENDIENTE, COMPROMISO_PAGO
    private String colorEstadoEstudiante;
    
    // Detalles adicionales
    private boolean pagoActualizado;
    private boolean estudianteActualizado;
    private String detalleError;
}
