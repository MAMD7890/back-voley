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
     */
    @PostMapping("/confirmar-pago/referencia/{reference}")
    public ResponseEntity<ConfirmacionPagoResponse> confirmarPagoPorReferencia(
            @PathVariable String reference) {
        
        log.info("Confirmando pago por referencia: {}", reference);
        
        ConfirmacionPagoResponse response = wompiService.confirmarPagoPorReferencia(reference);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}
