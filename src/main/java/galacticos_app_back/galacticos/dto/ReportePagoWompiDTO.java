package galacticos_app_back.galacticos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO para reportes de pagos realizados mediante Wompi
 * Incluye información del pago y del estudiante asociado
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportePagoWompiDTO {
    
    // Información del pago
    private Integer idPago;
    private String referenciaPago;
    private String wompiTransactionId;
    private BigDecimal monto;
    private String moneda;
    private LocalDate fechaPago;
    private LocalTime horaPago;
    private String mesPagado;
    private String metodoPago;
    private String estadoPago;
    
    // Información del estudiante
    private Integer idEstudiante;
    private String nombreEstudiante;
    private String apellidoEstudiante;
    private String emailEstudiante;
    private String telefonoEstudiante;
    private String documentoEstudiante;
    private String estadoPagoEstudiante;
    private String fotoEstudiante;
    
    // Información adicional
    private String nombreCompleto;
    private String colorEstadoPago;
    private String descripcionEstadoPago;
}
