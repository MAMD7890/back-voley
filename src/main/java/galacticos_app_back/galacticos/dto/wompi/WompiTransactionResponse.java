package galacticos_app_back.galacticos.dto.wompi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WompiTransactionResponse {
    
    private String transactionId;
    private String status;
    private String reference;
    private Long amountInCents;
    private String currency;
    private String paymentMethodType;
    private String paymentLinkUrl;
    private String redirectUrl;
    private String message;
    private boolean success;
}
