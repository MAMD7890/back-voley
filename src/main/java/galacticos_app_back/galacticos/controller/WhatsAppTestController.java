package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.WhatsAppMessageResult;
import galacticos_app_back.galacticos.service.TwilioWhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador de PRUEBAS para WhatsApp - SIN AUTENTICACIÓN.
 * 
 * ⚠️ IMPORTANTE: Este controlador es solo para desarrollo/pruebas.
 * Debe eliminarse o protegerse antes de pasar a producción.
 * 
 * @author Galacticos App
 */
@RestController
@RequestMapping("/api/test/whatsapp")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class WhatsAppTestController {

    private final TwilioWhatsAppService twilioWhatsAppService;

    /**
     * Endpoint de prueba para verificar el servicio.
     * GET /api/test/whatsapp/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        boolean disponible = twilioWhatsAppService.isServicioDisponible();
        
        return ResponseEntity.ok(Map.of(
            "servicio", "WhatsApp Twilio",
            "disponible", disponible,
            "mensaje", disponible ? "Servicio listo para enviar mensajes" : "Servicio no disponible"
        ));
    }

    /**
     * Envía un mensaje de prueba a un número específico.
     * POST /api/test/whatsapp/enviar
     * Body: { "telefono": "573001234567" }
     */
    @PostMapping("/enviar")
    public ResponseEntity<WhatsAppMessageResult> enviarPrueba(@RequestBody Map<String, String> request) {
        String telefono = request.get("telefono");
        
        if (telefono == null || telefono.isBlank()) {
            return ResponseEntity.badRequest().body(
                WhatsAppMessageResult.builder()
                    .exito(false)
                    .error("Debe proporcionar el campo 'telefono'")
                    .build()
            );
        }

        log.info("🧪 [TEST] Enviando mensaje de prueba a: {}", telefono);
        
        WhatsAppMessageResult resultado = twilioWhatsAppService.enviarMensajePrueba(telefono);
        
        if (resultado.isExito()) {
            log.info("✅ [TEST] Mensaje enviado exitosamente. SID: {}", resultado.getMessageSid());
        } else {
            log.error("❌ [TEST] Error al enviar: {}", resultado.getError());
        }
        
        return ResponseEntity.ok(resultado);
    }

    /**
     * Envía un recordatorio de prueba simulando diferentes días.
     * POST /api/test/whatsapp/recordatorio
     * Body: { "telefono": "573001234567", "nombre": "Juan Pérez", "dias": -5 }
     * dias: -5, -3, 0, 3, 5 (días antes/después del vencimiento)
     */
    @PostMapping("/recordatorio")
    public ResponseEntity<WhatsAppMessageResult> enviarRecordatorioPrueba(@RequestBody Map<String, Object> request) {
        String telefono = (String) request.get("telefono");
        String nombre = (String) request.getOrDefault("nombre", "Estudiante de Prueba");
        Integer dias = (Integer) request.getOrDefault("dias", 0);
        
        if (telefono == null || telefono.isBlank()) {
            return ResponseEntity.badRequest().body(
                WhatsAppMessageResult.builder()
                    .exito(false)
                    .error("Debe proporcionar el campo 'telefono'")
                    .build()
            );
        }

        // Calcular fecha de vencimiento basada en los días
        java.time.LocalDate fechaVencimiento = java.time.LocalDate.now().plusDays(-dias);
        String fechaFormateada = fechaVencimiento.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        log.info("🧪 [TEST] Enviando recordatorio de prueba - Teléfono: {}, Nombre: {}, Días: {}", 
                telefono, nombre, dias);
        
        WhatsAppMessageResult resultado = twilioWhatsAppService.enviarRecordatorioPago(
                telefono, 
                nombre, 
                fechaFormateada, 
                dias
        );
        
        if (resultado.isExito()) {
            log.info("✅ [TEST] Recordatorio enviado. SID: {}", resultado.getMessageSid());
        } else {
            log.error("❌ [TEST] Error al enviar recordatorio: {}", resultado.getError());
        }
        
        return ResponseEntity.ok(resultado);
    }
}
