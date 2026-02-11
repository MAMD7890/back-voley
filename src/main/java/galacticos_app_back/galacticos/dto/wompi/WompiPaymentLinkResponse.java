package galacticos_app_back.galacticos.dto.wompi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WompiPaymentLinkResponse {
    
    // Solo lo necesario para el cliente
    private boolean success;
    private String paymentLinkUrl;    // URL a la que el usuario debe ser redirigido
    private String reference;         // Referencia única del pago
    private String id;                // ID del link en Wompi
    private String message;           // Mensaje de estado
    
    // Campos adicionales opcionales
    private Long amountInCents;       // Para logging/verificación
    private String currency;          // Para logging/verificación
}
