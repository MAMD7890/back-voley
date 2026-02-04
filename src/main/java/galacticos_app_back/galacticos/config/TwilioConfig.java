package galacticos_app_back.galacticos.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Twilio para el envío de mensajes WhatsApp.
 * 
 * Las credenciales se obtienen de application.properties:
 * - twilio.account-sid: SID de la cuenta Twilio
 * - twilio.auth-token: Token de autenticación
 * - twilio.whatsapp-from: Número de WhatsApp de Twilio (formato: whatsapp:+14155238886)
 * - twilio.content-sid: SID de la plantilla de contenido para mensajes
 * 
 * @author Galacticos App
 * @version 1.1
 */
@Configuration
@Getter
@Slf4j
public class TwilioConfig {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.whatsapp-from}")
    private String whatsappFrom;

    @Value("${twilio.enabled:true}")
    private boolean enabled;

    @Value("${twilio.sandbox:true}")
    private boolean sandbox;

    @Value("${twilio.content-sid:}")
    private String contentSid;

    /**
     * Inicializa el cliente de Twilio con las credenciales configuradas.
     * Se ejecuta automáticamente después de la inyección de dependencias.
     */
    @PostConstruct
    public void initTwilio() {
        if (enabled) {
            try {
                Twilio.init(accountSid, authToken);
                log.info("✅ Twilio inicializado correctamente. Modo sandbox: {}", sandbox);
                log.info("📱 WhatsApp From: {}", whatsappFrom);
                if (contentSid != null && !contentSid.isBlank()) {
                    log.info("📋 Content Template SID: {}", contentSid);
                }
            } catch (Exception e) {
                log.error("❌ Error al inicializar Twilio: {}", e.getMessage());
                throw new RuntimeException("No se pudo inicializar Twilio", e);
            }
        } else {
            log.warn("⚠️ Twilio está DESHABILITADO. Los mensajes de WhatsApp no se enviarán.");
        }
    }

    /**
     * Formatea un número de teléfono al formato WhatsApp de Twilio.
     * 
     * @param telefono número de teléfono (puede incluir o no el prefijo +57)
     * @return número formateado como whatsapp:+57XXXXXXXXXX
     */
    public String formatearNumeroWhatsApp(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return null;
        }
        
        // Limpiar el número: solo dígitos
        String numeroLimpio = telefono.replaceAll("[^0-9]", "");
        
        // Si ya tiene código de país Colombia (57), solo agregar prefijo whatsapp
        if (numeroLimpio.startsWith("57") && numeroLimpio.length() == 12) {
            return "whatsapp:+" + numeroLimpio;
        }
        
        // Si es número colombiano sin código de país (10 dígitos)
        if (numeroLimpio.length() == 10) {
            return "whatsapp:+57" + numeroLimpio;
        }
        
        // Para otros casos, intentar agregar el prefijo
        return "whatsapp:+" + numeroLimpio;
    }

    /**
     * Verifica si hay una plantilla de contenido configurada.
     */
    public boolean tieneContentTemplate() {
        return contentSid != null && !contentSid.isBlank();
    }
}
