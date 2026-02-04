package galacticos_app_back.galacticos.dto.wompi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WompiWebhookEvent {
    
    private String event;
    private WompiWebhookData data;
    private String environment;
    private WompiSignature signature;
    
    @JsonProperty("sent_at")
    private String sentAt;
    
    private Long timestamp;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WompiWebhookData {
        private WompiTransaction transaction;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WompiTransaction {
        private String id;
        private String status;
        private String reference;
        
        @JsonProperty("amount_in_cents")
        private Long amountInCents;
        
        private String currency;
        
        @JsonProperty("customer_email")
        private String customerEmail;
        
        @JsonProperty("payment_method_type")
        private String paymentMethodType;
        
        @JsonProperty("payment_method")
        private Map<String, Object> paymentMethod;
        
        @JsonProperty("created_at")
        private String createdAt;
        
        @JsonProperty("finalized_at")
        private String finalizedAt;
        
        @JsonProperty("status_message")
        private String statusMessage;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WompiSignature {
        private String checksum;
        private String properties;
    }
}
