package galacticos_app_back.galacticos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para registrar un pago en efectivo manualmente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroPagoEfectivoDTO {
    
    /**
     * ID del estudiante que realiza el pago
     */
    private Integer idEstudiante;
    
    /**
     * Mes que se está pagando (formato: "ENERO_2026", "FEBRERO_2026", etc.)
     */
    private String mesPagado;
    
    /**
     * Valor del pago
     */
    private BigDecimal valor;
    
    /**
     * Observación o nota del pago (opcional)
     */
    private String observacion;
    
    /**
     * Referencia del pago en efectivo (número de recibo, etc.)
     */
    private String referenciaPago;
}
