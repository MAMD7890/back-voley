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
    
    private String id;
    private String name;
    private String description;
    private Long amountInCents;
    private String currency;
    private String paymentLinkUrl;
    private Boolean singleUse;
    private Boolean active;
    private Long expiresAt;
    private String reference;
    private boolean success;
    private String message;
}
