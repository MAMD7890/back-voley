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
public class WompiTransactionRequest {
    
    private Integer idEstudiante;
    private BigDecimal amount; // Monto en pesos (se convertirá a centavos)
    private String currency; // COP por defecto
    private String customerEmail;
    private String customerName;
    private String customerPhone;
    private String reference; // Referencia única del pago
    private String redirectUrl; // URL de redirección después del pago
    private String description;
    private String mesPagado; // Mes que se está pagando
}
