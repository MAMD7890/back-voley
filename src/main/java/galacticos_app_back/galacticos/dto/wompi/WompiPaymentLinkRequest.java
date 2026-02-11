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
public class WompiPaymentLinkRequest {
    
    private Integer idEstudiante;
    private BigDecimal amount;
    private String currency;
    private String name; // Nombre del link de pago
    private String description;
    private String customerEmail;
    private String customerName;
    private String customerPhone; // Teléfono del cliente (REQUERIDO por Wompi)
    private String redirectUrl;
    private String mesPagado;
    private Boolean singleUse; // Si es un link de un solo uso
    private Long expiresAt; // Timestamp de expiración
    private Boolean collectShipping; // Si recolectar datos de envío
    private String customerDocument;      // Nuevo: Número de documento
private String customerDocumentType;  // Nuevo: Tipo de documento (CC, CE, TI, PP)
}
