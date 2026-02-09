package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.WhatsAppMessageResult;
import galacticos_app_back.galacticos.entity.RecordatorioPago;
import galacticos_app_back.galacticos.service.RecordatorioPagoService;
import galacticos_app_back.galacticos.service.RecordatorioSchedulerService;
import galacticos_app_back.galacticos.service.TwilioWhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la administración de recordatorios de pago.
 * 
 * Proporciona endpoints para:
 * - Consultar historial de recordatorios
 * - Ejecutar manualmente el proceso de envío
 * - Obtener estadísticas del sistema
 * - Probar envío de mensajes WhatsApp
 * 
 * Todos los endpoints requieren autenticación y rol de administrador.
 * 
 * @author Galacticos App
 * @version 1.1
 */
@RestController
@RequestMapping("/api/admin/recordatorios")
@RequiredArgsConstructor
@Slf4j
public class RecordatorioAdminController {

    private final RecordatorioPagoService recordatorioPagoService;
    private final RecordatorioSchedulerService recordatorioSchedulerService;
    private final TwilioWhatsAppService twilioWhatsAppService;

    /**
     * Obtiene todos los recordatorios del sistema.
     * GET /api/admin/recordatorios
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RecordatorioPago>> listarTodos() {
        log.info("📋 Consultando todos los recordatorios");
        List<RecordatorioPago> recordatorios = recordatorioPagoService.obtenerTodos();
        return ResponseEntity.ok(recordatorios);
    }

    /**
     * Obtiene un recordatorio por ID.
     * GET /api/admin/recordatorios/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecordatorioPago> obtenerPorId(@PathVariable Integer id) {
        return recordatorioPagoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtiene el historial de recordatorios de un estudiante.
     * GET /api/admin/recordatorios/estudiante/{idEstudiante}
     */
    @GetMapping("/estudiante/{idEstudiante}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RecordatorioPago>> obtenerPorEstudiante(@PathVariable Integer idEstudiante) {
        log.info("📋 Consultando recordatorios del estudiante ID: {}", idEstudiante);
        List<RecordatorioPago> recordatorios = recordatorioPagoService.obtenerHistorialPorEstudianteId(idEstudiante);
        return ResponseEntity.ok(recordatorios);
    }

    /**
     * Ejecuta manualmente el proceso de envío de recordatorios.
     * POST /api/admin/recordatorios/ejecutar
     * 
     * Útil para pruebas o para forzar el envío fuera del horario programado.
     */
    @PostMapping("/ejecutar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> ejecutarManualmente() {
        log.info("⚡ Ejecución manual de recordatorios solicitada por administrador");
        
        try {
            Map<String, Object> resultado = recordatorioSchedulerService.ejecutarManualmente();
            resultado.put("status", "success");
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("❌ Error en ejecución manual: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Envía un mensaje de prueba por WhatsApp para verificar la configuración.
     * POST /api/admin/recordatorios/probar-whatsapp
     * 
     * Body: { "telefono": "3242595111" }
     */
    @PostMapping("/probar-whatsapp")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> probarWhatsApp(@RequestBody Map<String, String> request) {
        String telefono = request.get("telefono");
        
        if (telefono == null || telefono.isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("mensaje", "El campo 'telefono' es requerido");
            return ResponseEntity.badRequest().body(error);
        }
        
        log.info("🧪 Enviando mensaje de prueba a: {}", telefono);
        
        WhatsAppMessageResult resultado = twilioWhatsAppService.enviarMensajePrueba(telefono);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", resultado.isExito() ? "success" : "error");
        response.put("telefono", telefono);
        response.put("resultado", resultado);
        
        if (resultado.isExito()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtiene estadísticas del sistema de recordatorios.
     * GET /api/admin/recordatorios/estadisticas
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        log.info("📊 Consultando estadísticas de recordatorios");
        
        Map<String, Object> estadisticas = recordatorioSchedulerService.obtenerEstadisticas();
        estadisticas.put("estadisticasPorEstado", recordatorioPagoService.obtenerEstadisticasPorEstado());
        estadisticas.put("totalRecordatorios", recordatorioPagoService.obtenerTodos().size());
        
        return ResponseEntity.ok(estadisticas);
    }

    /**
     * Elimina un recordatorio por ID.
     * DELETE /api/admin/recordatorios/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Integer id) {
        log.info("🗑️ Eliminando recordatorio ID: {}", id);
        
        if (recordatorioPagoService.obtenerPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        recordatorioPagoService.eliminar(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Recordatorio eliminado correctamente");
        response.put("id", id.toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de health check para el servicio de recordatorios.
     * GET /api/admin/recordatorios/health
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("servicio", "RecordatoriosWhatsApp");
        health.putAll(recordatorioSchedulerService.obtenerEstadisticas());
        
        return ResponseEntity.ok(health);
    }
}
