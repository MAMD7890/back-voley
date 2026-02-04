package galacticos_app_back.galacticos.dto.wompi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WompiIntegritySignature {
    
    private String reference;
    private Long amountInCents;
    private String currency;
    private String integritySignature;
    private String publicKey;
}
