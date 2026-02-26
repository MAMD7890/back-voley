package galacticos_app_back.galacticos.dto.wompi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegritySignatureRequest {
    
    private BigDecimal amount;      // En pesos (ej: 80000)
    private String reference;       // Referencia única del pago
    private String currency;        // Moneda (default: "COP")
    
    // Campos adicionales para crear el pago pendiente
    private Integer idEstudiante;   // ID del estudiante
    private String mesPagado;       // Mes que se está pagando (ej: "2026-02")
}
