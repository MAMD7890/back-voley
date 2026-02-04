package galacticos_app_back.galacticos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa el resultado de un envío de mensaje WhatsApp.
 * 
 * Contiene información sobre el éxito o fallo del envío,
 * así como detalles de la respuesta de Twilio.
 * 
 * @author Galacticos App
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppMessageResult {
    
    /**
     * Indica si el mensaje fue enviado exitosamente
     */
    private boolean exito;
    
    /**
     * SID del mensaje asignado por Twilio (identificador único)
     */
    private String messageSid;
    
    /**
     * Estado del mensaje según Twilio (queued, sent, delivered, etc.)
     */
    private String estado;
    
    /**
     * Mensaje descriptivo del resultado
     */
    private String mensaje;
    
    /**
     * Descripción del error en caso de fallo
     */
    private String error;
    
    /**
     * Código de error de la API de Twilio (si aplica)
     */
    private Integer codigoError;
}
