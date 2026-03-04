package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.config.WompiConfig;
import galacticos_app_back.galacticos.dto.wompi.*;
import galacticos_app_back.galacticos.dto.auth.MessageResponse;
import galacticos_app_back.galacticos.service.WompiService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/wompi")
@RequiredArgsConstructor
@Slf4j
public class WompiController {
    
    private final WompiService wompiService;
    private final WompiConfig wompiConfig;
    
    /**
     * Obtiene la llave pública de Wompi para el frontend
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getPublicConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("publicKey", wompiConfig.getPublicKey());
        config.put("sandbox", wompiConfig.isSandbox());
        config.put("currency", "COP");
        return ResponseEntity.ok(config);
    }
    
    /**
     * Genera la firma de integridad para el widget de Wompi
     * Endpoint para validar integridad de transacciones en el widget
     * 
     * Request: POST /api/wompi/integrity-signature
     * Body: {"amount": 80000, "reference": "PAY-1-2025-02-ABC123", "currency": "COP"}
     * 
     * Response: {"reference": "...", "amountInCents": 8000000, "integritySignature": "sha256...", "publicKey": "pub_prod_..."}
     */
    @PostMapping("/integrity-signature")
    public ResponseEntity<?> generateIntegritySignature(
            @RequestBody IntegritySignatureRequest request) {
        try {
            log.info("Generando firma de integridad para referencia: {}, idEstudiante: {}", 
                    request.getReference(), request.getIdEstudiante());
            
            String currency = request.getCurrency() != null ? request.getCurrency() : "COP";
            
            // Pasar idEstudiante y mesPagado para crear el pago pendiente
            WompiIntegritySignature signature = wompiService.generateIntegritySignature(
                    request.getAmount(),
                    request.getReference(),
                    currency,
                    request.getIdEstudiante(),
                    request.getMesPagado());
            
            log.info("✅ Firma de integridad generada correctamente - Reference: {}", request.getReference());
            return ResponseEntity.ok(signature);
            
        } catch (Exception e) {
            log.error("❌ Error generando firma de integridad: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MessageResponse.error("Error generando firma: " + e.getMessage()));
        }
    }
    
    /**
     * Genera una referencia única para el pago
     */
    @GetMapping("/generate-reference")
    public ResponseEntity<Map<String, String>> generateReference(
            @RequestParam Integer idEstudiante,
            @RequestParam String mesPagado) {
        
        String reference = wompiService.generateReference(idEstudiante, mesPagado);
        Map<String, String> response = new HashMap<>();
        response.put("reference", reference);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Crea un link de pago de Wompi
     */
    @PostMapping("/payment-link")
    public ResponseEntity<WompiPaymentLinkResponse> createPaymentLink(
            @RequestBody WompiPaymentLinkRequest request) {
        
        log.info("Creando link de pago para estudiante: {}", request.getIdEstudiante());
        WompiPaymentLinkResponse response = wompiService.createPaymentLink(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Consulta el estado de una transacción por ID
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<WompiTransactionResponse> getTransactionStatus(
            @PathVariable String transactionId) {
        
        WompiTransactionResponse response = wompiService.getTransactionStatus(transactionId);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Consulta el estado de una transacción por referencia
     * Busca en la base de datos local usando la referencia del pago
     * 
     * GET /api/wompi/transaction/reference/PAY-123-2025-02-ABC123
     * 
     * Response (200 OK):
     * {
     *   "transactionId": "123456-abcd-efgh",
     *   "status": "APPROVED",
     *   "reference": "PAY-123-2025-02-ABC123",
     *   "amountInCents": 8000000,
     *   "currency": "COP",
     *   "paymentMethodType": "CARD",
     *   "success": true,
     *   "message": "Transacción encontrada"
     * }
     */
    @GetMapping("/transaction/reference/{reference}")
    public ResponseEntity<WompiTransactionResponse> getTransactionByReference(
            @PathVariable String reference) {
        
        log.info("Consultando transacción por referencia: {}", reference);
        WompiTransactionResponse response = wompiService.getTransactionByReference(reference);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Verifica y sincroniza el estado de un pago con Wompi
     * Útil si el webhook no llegó o para verificar manualmente
     */
    @PostMapping("/sync-payment/{transactionId}")
    public ResponseEntity<Map<String, Object>> syncPaymentStatus(
            @PathVariable String transactionId) {
        
        log.info("Sincronizando estado de pago - Transaction ID: {}", transactionId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Consultar estado en Wompi
            WompiTransactionResponse response = wompiService.getTransactionStatus(transactionId);
            
            if (response.isSuccess()) {
                // Procesar como si fuera un webhook
                boolean updated = wompiService.syncPaymentFromTransaction(transactionId, response);
                
                result.put("success", true);
                result.put("transactionId", transactionId);
                result.put("status", response.getStatus());
                result.put("paymentUpdated", updated);
                result.put("message", updated ? "Pago sincronizado correctamente" : "Pago no encontrado en base de datos");
                
                return ResponseEntity.ok(result);
            }
            
            result.put("success", false);
            result.put("message", response.getMessage());
            return ResponseEntity.badRequest().body(result);
            
        } catch (Exception e) {
            log.error("Error sincronizando pago: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * Endpoint para recibir webhooks de Wompi
     * Este endpoint debe ser público (sin autenticación)
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody WompiWebhookEvent event,
            @RequestHeader(value = "X-Event-Checksum", required = false) String checksum) {
        
        log.info("Webhook recibido de Wompi - Evento: {}", event.getEvent());
        
        boolean processed = wompiService.processWebhook(event, checksum);
        
        Map<String, String> response = new HashMap<>();
        if (processed) {
            response.put("status", "ok");
            response.put("message", "Webhook procesado correctamente");
            return ResponseEntity.ok(response);
        }
        
        response.put("status", "error");
        response.put("message", "Error procesando webhook");
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Obtiene el token de aceptación y métodos de pago disponibles
     */
    @GetMapping("/acceptance-token")
    public ResponseEntity<JsonNode> getAcceptanceToken() {
        JsonNode data = wompiService.getAcceptanceToken();
        if (data != null) {
            return ResponseEntity.ok(data);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Endpoint de redirección después del pago
     * Este debe ser público y redirigir al frontend
     */
    @GetMapping("/payment-result")
    public ResponseEntity<Map<String, Object>> paymentResult(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String env,
            @RequestParam(required = false) String reference) {
        
        log.info("Resultado de pago recibido - ID: {}, Env: {}, Reference: {}", id, env, reference);
        
        Map<String, Object> result = new HashMap<>();
        result.put("transactionId", id);
        result.put("reference", reference);
        result.put("environment", env);
        
        if (id != null) {
            WompiTransactionResponse transaction = wompiService.getTransactionStatus(id);
            result.put("status", transaction.getStatus());
            result.put("success", transaction.isSuccess());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Confirma un pago consultando a Wompi y actualizando el sistema
     * Recibe transactionId y/o reference para identificar el pago
     */
    @PostMapping("/confirmar-pago")
    public ResponseEntity<ConfirmacionPagoResponse> confirmarPago(
            @RequestBody ConfirmacionPagoRequest request) {
        
        log.info("Confirmando pago - TransactionId: {}, Reference: {}", 
                request.getTransactionId(), request.getReference());
        
        ConfirmacionPagoResponse response = wompiService.confirmarPago(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Confirma un pago usando solo la referencia
     * Acepta transactionId como query param para consultar Wompi directamente
     */
    @PostMapping("/confirmar-pago/referencia/{reference}")
    public ResponseEntity<ConfirmacionPagoResponse> confirmarPagoPorReferencia(
            @PathVariable String reference,
            @RequestParam(required = false) String transactionId) {
        
        log.info("Confirmando pago por referencia: {}, transactionId: {}", reference, transactionId);
        
        ConfirmacionPagoResponse response = wompiService.confirmarPagoPorReferencia(reference, transactionId);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Confirma un pago usando referencia y/o transactionId del body
     * Este endpoint es más flexible para el frontend
     */
    @PostMapping("/confirmar-pago/transaction")
    public ResponseEntity<ConfirmacionPagoResponse> confirmarPagoTransaction(
            @RequestBody ConfirmacionPagoRequest request) {
        
        log.info("Confirmando pago - TransactionId: {}, Reference: {}", 
                request.getTransactionId(), request.getReference());
        
        ConfirmacionPagoResponse response = wompiService.confirmarPago(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Sincroniza masivamente las fechas de todos los pagos de Wompi
     * Consulta la API de Wompi para cada transacción y actualiza la fecha/hora
     * con el timestamp real de la transacción convertido a zona horaria de Colombia.
     * 
     * ADVERTENCIA: Este endpoint puede tardar varios segundos si hay muchos pagos.
     * Usar solo cuando sea necesario corregir fechas históricas.
     * 
     * POST /api/wompi/sync-all-dates
     * 
     * Response:
     * {
     *   "totalPagos": 15,
     *   "actualizados": 10,
     *   "sinCambios": 3,
     *   "errores": 2,
     *   "detalles": [
     *     {
     *       "idPago": 123,
     *       "transactionId": "abc-123",
     *       "fechaAnterior": "2026-03-03",
     *       "fechaNueva": "2026-03-02",
     *       "estado": "ACTUALIZADO"
     *     }
     *   ]
     * }
     */
    @PostMapping("/sync-all-dates")
    public ResponseEntity<Map<String, Object>> sincronizarFechasPagosWompi() {
        log.info("🔄 Iniciando sincronización masiva de fechas de pagos Wompi");
        
        try {
            Map<String, Object> resultado = wompiService.sincronizarFechasPagosWompi();
            
            log.info("✅ Sincronización completada - Actualizados: {}, Errores: {}", 
                resultado.get("actualizados"), resultado.get("errores"));
            
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            log.error("❌ Error en sincronización masiva: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Sincroniza TODOS los pagos pendientes ONLINE consultando la API de Wompi.
     * Este endpoint es útil cuando los webhooks no llegaron o para corregir estados.
     * 
     * Proceso:
     * 1. Obtiene todos los pagos con estado PENDIENTE y método ONLINE
     * 2. Para cada pago con wompiTransactionId, consulta el estado en Wompi
     * 3. Si está APPROVED: actualiza pago a PAGADO y estudiante a AL_DIA
     * 4. Si está DECLINED/VOIDED/ERROR: actualiza pago a RECHAZADO
     * 
     * POST /api/wompi/sync-pending-payments
     * 
     * Response:
     * {
     *   "totalPagosPendientes": 10,
     *   "actualizadosAPagado": 5,
     *   "actualizadosARechazado": 1,
     *   "sinTransactionId": 3,
     *   "errores": 1,
     *   "detalles": [...]
     * }
     */
    @PostMapping("/sync-pending-payments")
    public ResponseEntity<Map<String, Object>> sincronizarPagosPendientes() {
        log.info("🔄 Iniciando sincronización de pagos pendientes");
        
        try {
            Map<String, Object> resultado = wompiService.sincronizarPagosPendientes();
            
            log.info("✅ Sincronización de pagos pendientes completada - Actualizados: {}, Errores: {}", 
                resultado.get("actualizadosAPagado"), resultado.get("errores"));
            
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            log.error("❌ Error sincronizando pagos pendientes: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Sincroniza pagos consultando las transacciones DIRECTAMENTE desde Wompi.
     * Este es el método más efectivo cuando los pagos quedaron sin wompiTransactionId.
     * 
     * Proceso:
     * 1. Consulta las últimas 100 transacciones de Wompi
     * 2. Para cada transacción APPROVED, busca si hay un pago pendiente con esa referencia
     * 3. Si encuentra match: vincula el transactionId y actualiza estados
     * 4. Si no hay match pero la referencia es PAY-{id}-...: crea el pago automáticamente
     * 
     * USAR ESTE ENDPOINT cuando los pagos no tienen transactionId
     * 
     * POST /api/wompi/sync-from-wompi
     * 
     * Response:
     * {
     *   "transaccionesConsultadas": 100,
     *   "vinculados": 5,
     *   "yaExistentes": 10,
     *   "sinMatchPendiente": 80,
     *   "errores": 0,
     *   "detalles": [...]
     * }
     */
    @PostMapping("/sync-from-wompi")
    public ResponseEntity<Map<String, Object>> sincronizarDesdeWompi() {
        log.info("🔄 Iniciando sincronización desde transacciones de Wompi");
        
        try {
            Map<String, Object> resultado = wompiService.sincronizarDesdeTransaccionesWompi();
            
            log.info("✅ Sincronización desde Wompi completada - Vinculados: {}, Errores: {}", 
                resultado.get("vinculados"), resultado.get("errores"));
            
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            log.error("❌ Error sincronizando desde Wompi: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Corrige los estados de estudiantes que tienen pagos PENDIENTES sin confirmar.
     * 
     * Los estudiantes que tienen pagos PENDIENTES sin transactionId de Wompi
     * (es decir, abrieron el widget pero NO completaron el pago) y están marcados
     * como AL_DIA incorrectamente, serán corregidos a estado PENDIENTE.
     * 
     * POST /api/wompi/corregir-estados
     * 
     * Query params:
     * - eliminarPagos: true/false - Si true, elimina los pagos pendientes sin confirmar
     * 
     * Response:
     * {
     *   "totalPagosPendientesSinConfirmar": 21,
     *   "estudiantesCorregidos": 5,
     *   "pagosEliminados": 0,
     *   "errores": 0,
     *   "detalles": [...]
     * }
     */
    @PostMapping("/corregir-estados")
    public ResponseEntity<Map<String, Object>> corregirEstadosEstudiantes(
            @RequestParam(defaultValue = "false") boolean eliminarPagos) {
        log.info("🔄 Iniciando corrección de estados de estudiantes - eliminarPagos: {}", eliminarPagos);
        
        try {
            Map<String, Object> resultado = wompiService.corregirEstadosEstudiantes(eliminarPagos);
            
            log.info("✅ Corrección completada - Estudiantes corregidos: {}, Pagos eliminados: {}", 
                resultado.get("estudiantesCorregidos"), resultado.get("pagosEliminados"));
            
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            log.error("❌ Error corrigiendo estados: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Sincroniza los estados de estudiantes con sus pagos confirmados.
     * Si un estudiante tiene pagos PAGADOS pero su estado no es AL_DIA,
     * lo actualiza a AL_DIA y activa su membresía.
     * 
     * Útil para corregir inconsistencias donde el pago se procesó correctamente
     * pero el estado del estudiante no se actualizó.
     * 
     * POST /api/wompi/sincronizar-estados
     * 
     * Response:
     * {
     *   "estudiantesActualizadosAAlDia": 3,
     *   "membresiasActivadas": 5,
     *   "yaAlDia": 42,
     *   "sinPagosRecientes": 10,
     *   "errores": 0,
     *   "detalles": [...]
     * }
     */
    @PostMapping("/sincronizar-estados")
    public ResponseEntity<Map<String, Object>> sincronizarEstadosConPagos() {
        log.info("🔄 Iniciando sincronización de estados con pagos confirmados");
        
        try {
            Map<String, Object> resultado = wompiService.sincronizarEstadosConPagos();
            
            log.info("✅ Sincronización completada - Actualizados a AL_DIA: {}, Membresías activadas: {}", 
                resultado.get("estudiantesActualizadosAAlDia"), resultado.get("membresiasActivadas"));
            
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            log.error("❌ Error sincronizando estados: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
