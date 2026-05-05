package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.service.TareasInternasService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controlador para tareas internas invocadas por AWS Lambda.
 *
 * Estos endpoints NO requieren JWT pero están protegidos con una API Key
 * que debe enviarse en el header: X-Internal-Api-Key
 *
 * Endpoints:
 *   POST /api/internal/estados/actualizar   → actualiza estados a EN_MORA (Lambda medianoche)
 *   POST /api/internal/recordatorios/enviar → envía WhatsApp del día       (Lambda 10:00 AM)
 *   GET  /api/internal/health               → verifica que el servicio esté activo
 *
 * La API Key se configura en application.properties:
 *   app.internal.api-key=TU_CLAVE_SECRETA
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalTaskController {

    private final TareasInternasService tareasInternasService;

    @Value("${app.internal.api-key}")
    private String apiKey;

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    // ─────────────────────────────────────────────────────────────────
    //  TAREA 1: Actualizar estados a EN_MORA (invocada a medianoche)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Revisa membresías vencidas y actualiza estudiantes a EN_MORA.
     * Invocado por Lambda a medianoche (cron: 0 0 * * ? *)
     */
    @PostMapping("/estados/actualizar")
    public ResponseEntity<Map<String, Object>> actualizarEstados(HttpServletRequest request) {
        if (!esValida(request)) return respuestaNoAutorizada();

        log.info("🌙 [LAMBDA] Solicitud de actualización de estados desde IP: {}", request.getRemoteAddr());

        try {
            Map<String, Object> resultado = tareasInternasService.ejecutarActualizacionEstados();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("❌ Error en actualización de estados: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  TAREA 2: Enviar recordatorios WhatsApp (invocada a las 10:00 AM)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Procesa las membresías del día y envía los mensajes de WhatsApp.
     * Invocado por Lambda a las 10:00 AM (cron: 0 10 * * ? *)
     */
    @PostMapping("/recordatorios/enviar")
    public ResponseEntity<Map<String, Object>> enviarRecordatorios(HttpServletRequest request) {
        if (!esValida(request)) return respuestaNoAutorizada();

        log.info("📱 [LAMBDA] Solicitud de envío de recordatorios desde IP: {}", request.getRemoteAddr());

        try {
            Map<String, Object> resultado = tareasInternasService.ejecutarEnvioRecordatorios();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("❌ Error en envío de recordatorios: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  CORRECCIÓN PUNTUAL: sanear EN_MORA sin membresía real
    // ─────────────────────────────────────────────────────────────────

    /**
     * Busca estudiantes EN_MORA que nunca tuvieron membresía real y los pasa a PENDIENTE,
     * reiniciando su fecha de registro a hoy para que el ciclo de 5 días arranque desde cero.
     *
     * Se ejecuta una sola vez (o cuando se detecte data sucia).
     * POST /api/internal/estados/corregir-sin-membresia
     */
    @PostMapping("/estados/corregir-sin-membresia")
    public ResponseEntity<Map<String, Object>> corregirEnMoraSinMembresia(HttpServletRequest request) {
        if (!esValida(request)) return respuestaNoAutorizada();

        log.info("🔧 [LAMBDA] Corrección EN_MORA sin membresía desde IP: {}", request.getRemoteAddr());

        try {
            Map<String, Object> resultado = tareasInternasService.corregirEnMoraSinMembresia();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("❌ Error en corrección: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  CORRECCIÓN PUNTUAL: reconciliar fechas de membresía con pagos reales
    // ─────────────────────────────────────────────────────────────────

    /**
     * Revisa cada membresía activa y compara su duración con los pagos APROBADOS
     * encontrados en su rango. Si los pagos cubren más meses que los registrados,
     * extiende la fechaFin y pone al estudiante en AL_DIA.
     *
     * Caso típico: Wompi registró $150.000 (2 meses) pero la membresía solo muestra 1 mes.
     *
     * POST /api/internal/membresias/reconciliar
     */
    @PostMapping("/membresias/reconciliar")
    public ResponseEntity<Map<String, Object>> reconciliarMembresias(HttpServletRequest request) {
        if (!esValida(request)) return respuestaNoAutorizada();

        log.info("🔄 [LAMBDA] Reconciliación pagos-membresías desde IP: {}", request.getRemoteAddr());

        try {
            Map<String, Object> resultado = tareasInternasService.reconciliarPagosConMembresias();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("❌ Error en reconciliación: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  CORRECCIÓN PUNTUAL: eliminar pagos externos y recalcular membresías
    // ─────────────────────────────────────────────────────────────────

    /**
     * Busca pagos cuya referencia no empieza con "PAY-" (links externos de Wompi como
     * uniformes) que se registraron incorrectamente en membresías.
     * Recalcula fechaFin de cada membresía afectada usando solo pagos PAY- y elimina
     * los pagos externos de la BD.
     *
     * POST /api/internal/membresias/corregir-pagos-externos
     */
    @PostMapping("/membresias/corregir-pagos-externos")
    public ResponseEntity<Map<String, Object>> corregirPagosExternos(HttpServletRequest request) {
        if (!esValida(request)) return respuestaNoAutorizada();

        log.info("🔧 [LAMBDA] Corrección pagos externos desde IP: {}", request.getRemoteAddr());

        try {
            Map<String, Object> resultado = tareasInternasService.corregirPagosExternos();
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("❌ Error en corrección de pagos externos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  HEALTH CHECK
    // ─────────────────────────────────────────────────────────────────

    /**
     * Verifica que el servicio esté activo y responde correctamente.
     * Útil para que Lambda confirme conectividad antes de disparar tareas.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health(HttpServletRequest request) {
        if (!esValida(request)) return respuestaNoAutorizada();

        Map<String, Object> stats = tareasInternasService.obtenerEstadisticas();
        stats = new java.util.HashMap<>(stats);
        stats.put("status", "UP");
        stats.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(stats);
    }

    // ─────────────────────────────────────────────────────────────────
    //  VALIDACIÓN DE API KEY
    // ─────────────────────────────────────────────────────────────────

    private boolean esValida(HttpServletRequest request) {
        String key = request.getHeader(API_KEY_HEADER);
        if (key == null || !key.equals(apiKey)) {
            log.warn("🚫 Solicitud rechazada a {} - API Key inválida desde IP: {}",
                    request.getRequestURI(), request.getRemoteAddr());
            return false;
        }
        return true;
    }

    private ResponseEntity<Map<String, Object>> respuestaNoAutorizada() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
            "error", "API Key inválida o ausente",
            "header_requerido", API_KEY_HEADER
        ));
    }
}
