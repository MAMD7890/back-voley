package galacticos_app_back.galacticos.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Slf4j
public class MetaWhatsAppConfig {

    @Value("${meta.whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${meta.whatsapp.access-token:}")
    private String accessToken;

    @Value("${meta.whatsapp.api-version:v19.0}")
    private String apiVersion;

    @Value("${meta.whatsapp.enabled:false}")
    private boolean enabled;

    public String getApiUrl() {
        return "https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/messages";
    }

    /**
     * Formatea el número al estándar E.164 que requiere Meta: 573001234567 (sin +)
     */
    public String formatearNumero(String telefono) {
        if (telefono == null || telefono.isBlank()) return null;
        String limpio = telefono.replaceAll("[^0-9]", "");
        if (limpio.startsWith("57") && limpio.length() == 12) return limpio;
        if (limpio.length() == 10) return "57" + limpio;
        return limpio;
    }
}
