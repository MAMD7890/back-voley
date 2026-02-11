package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.config.WompiConfig;
import galacticos_app_back.galacticos.dto.wompi.*;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Pago;
import galacticos_app_back.galacticos.repository.EstudianteRepository;
import galacticos_app_back.galacticos.repository.PagoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WompiService {
    
    private final WompiConfig wompiConfig;
    private final RestTemplate restTemplate;
    private final PagoRepository pagoRepository;
    private final EstudianteRepository estudianteRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Genera la firma de integridad para el widget de Wompi
     */
    public WompiIntegritySignature generateIntegritySignature(BigDecimal amount, String reference, String currency) {
        Long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
        
        // Concatenar: referencia + monto en centavos + moneda + secreto de integridad
        String dataToSign = reference + amountInCents + currency + wompiConfig.getIntegritySecret();
        String signature = sha256(dataToSign);
        
        return WompiIntegritySignature.builder()
                .reference(reference)
                .amountInCents(amountInCents)
                .currency(currency)
                .integritySignature(signature)
                .publicKey(wompiConfig.getPublicKey())
                .build();
    }
    
    /**
     * Genera una referencia única para el pago
     */
    public String generateReference(Integer idEstudiante, String mesPagado) {
        return "PAY-" + idEstudiante + "-" + mesPagado + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
/**
 * Crea un link de pago en Wompi
 */
public WompiPaymentLinkResponse createPaymentLink(WompiPaymentLinkRequest request) {
    try {
        String reference = generateReference(request.getIdEstudiante(), request.getMesPagado());
        Long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(wompiConfig.getPrivateKey());
        
        Map<String, Object> body = new HashMap<>();
        body.put("name", request.getName() != null ? request.getName() : "Pago Mensualidad");
        body.put("description", request.getDescription() != null ? request.getDescription() : "Pago de mensualidad escuela de voleibol");
        body.put("single_use", request.getSingleUse() != null ? request.getSingleUse() : true);
        body.put("collect_shipping", request.getCollectShipping() != null ? request.getCollectShipping() : false);
        body.put("currency", request.getCurrency() != null ? request.getCurrency() : "COP");
        body.put("amount_in_cents", amountInCents);
        
        if (request.getRedirectUrl() != null) {
            body.put("redirect_url", request.getRedirectUrl());
        }
        
        if (request.getExpiresAt() != null) {
            body.put("expires_at", request.getExpiresAt());
        }
        
        // Agregar el ID del estudiante y mes como metadata
        Map<String, String> customerData = new HashMap<>();
        customerData.put("email", request.getCustomerEmail());
        customerData.put("full_name", request.getCustomerName());
        if (request.getCustomerPhone() != null) {
            customerData.put("phone_number", request.getCustomerPhone());
        }
        // ✅ NUEVO: Agregar campos de documento requeridos por Wompi
        if (request.getCustomerDocument() != null) {
            customerData.put("legal_id", request.getCustomerDocument());
        }
        if (request.getCustomerDocumentType() != null) {
            customerData.put("legal_id_type", request.getCustomerDocumentType());
        }
        body.put("customer_data", customerData);
        
        // Agregar referencia para tracking
        body.put("sku", reference);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                wompiConfig.getApiUrl() + "/payment_links",
                HttpMethod.POST,
                entity,
                String.class
        );
        
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode data = jsonResponse.get("data");
            
            // ✅ NUEVO: Log detallado
            log.info("===== RESPUESTA COMPLETA DE WOMPI (createPaymentLink) =====");
            log.info("Body completo: {}", response.getBody());
            log.info("Data node: {}", data);
            log.info("Data fields - id: {}, active: {}, merchant_public_key: {}", 
                data.get("id"), data.get("active"), data.get("merchant_public_key"));
            log.info("=========================================================");
            
            // Crear registro de pago pendiente
            createPendingPayment(request.getIdEstudiante(), request.getAmount(), 
                    reference, request.getMesPagado());
            
            return WompiPaymentLinkResponse.builder()
                    .success(true)
                    .id(data.get("id").asText())
                    .paymentLinkUrl("https://checkout.wompi.co/l/" + data.get("id").asText())
                    .reference(reference)
                    .amountInCents(amountInCents)
                    .currency(request.getCurrency() != null ? request.getCurrency() : "COP")
                    .message("Link de pago creado exitosamente")
                    .build();
        }
        
        return WompiPaymentLinkResponse.builder()
                .success(false)
                .message("Error al crear el link de pago")
                .build();
                
    } catch (Exception e) {
        log.error("Error creando link de pago en Wompi: {}", e.getMessage(), e);
        return WompiPaymentLinkResponse.builder()
                .success(false)
                .message("Error: " + e.getMessage())
                .build();
    }
}
    /**
     * Consulta el estado de una transacción
     */
    public WompiTransactionResponse getTransactionStatus(String transactionId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(wompiConfig.getPrivateKey());
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    wompiConfig.getApiUrl() + "/transactions/" + transactionId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode data = jsonResponse.get("data");
                
                // ✅ NUEVO: Log detallado
                log.info("===== RESPUESTA COMPLETA DE WOMPI (getTransaction) =====");
                log.info("Body completo: {}", response.getBody());
                log.info("Data node: {}", data);
                log.info("Data fields - id: {}, status: {}, reference: {}", 
                    data.get("id"), data.get("status"), data.get("reference"));
                log.info("========================================================");
                
                return WompiTransactionResponse.builder()
                        .transactionId(data.get("id").asText())
                        .status(data.get("status").asText())
                        .reference(data.has("reference") ? data.get("reference").asText() : null)
                        .amountInCents(data.get("amount_in_cents").asLong())
                        .currency(data.get("currency").asText())
                        .paymentMethodType(data.has("payment_method_type") ? data.get("payment_method_type").asText() : null)
                        .success(true)
                        .message("Transacción consultada exitosamente")
                        .build();
            }
            
            return WompiTransactionResponse.builder()
                    .success(false)
                    .message("Error al consultar la transacción")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error consultando transacción en Wompi: {}", e.getMessage(), e);
            return WompiTransactionResponse.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Sincroniza el estado de un pago desde una transacción de Wompi
     * Usado cuando el webhook no llega o para verificación manual
     */
    public boolean syncPaymentFromTransaction(String transactionId, WompiTransactionResponse transaction) {
        try {
            String status = transaction.getStatus();
            String reference = transaction.getReference();
            Long amountInCents = transaction.getAmountInCents();
            
            log.info("Sincronizando pago - ID: {}, Estado: {}, Referencia: {}", 
                transactionId, status, reference);
            
            // Buscar el pago
            Optional<Pago> pagoOpt = pagoRepository.findByWompiTransactionId(transactionId);
            
            if (!pagoOpt.isPresent() && reference != null) {
                pagoOpt = pagoRepository.findByReferenciaPago(reference);
            }
            
            if (!pagoOpt.isPresent() && amountInCents != null) {
                List<Pago> pagosPendientes = pagoRepository.findByEstadoPagoAndValor(
                    Pago.EstadoPago.PENDIENTE, 
                    BigDecimal.valueOf(amountInCents / 100.0)
                );
                if (!pagosPendientes.isEmpty()) {
                    pagoOpt = Optional.of(pagosPendientes.get(0));
                }
            }
            
            if (pagoOpt.isPresent()) {
                Pago pago = pagoOpt.get();
                
                if ("APPROVED".equals(status)) {
                    pago.setEstadoPago(Pago.EstadoPago.PAGADO);
                    pago.setFechaPago(LocalDate.now());
                    pago.setHoraPago(LocalTime.now());
                    pago.setWompiTransactionId(transactionId);
                    if (reference != null) {
                        pago.setReferenciaPago(reference);
                    }
                    
                    // Actualizar estado del estudiante
                    if (pago.getEstudiante() != null) {
                        Estudiante estudiante = pago.getEstudiante();
                        estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
                        estudianteRepository.save(estudiante);
                        log.info("✅ Estudiante {} actualizado a AL_DIA", estudiante.getIdEstudiante());
                    }
                    
                    pagoRepository.save(pago);
                    log.info("✅ Pago sincronizado correctamente - ID: {}", transactionId);
                    return true;
                    
                } else if ("DECLINED".equals(status) || "VOIDED".equals(status) || "ERROR".equals(status)) {
                    pago.setWompiTransactionId(transactionId);
                    pagoRepository.save(pago);
                    log.info("❌ Pago rechazado sincronizado - ID: {}", transactionId);
                    return true;
                }
            }
            
            log.warn("No se encontró pago para sincronizar - ID: {}", transactionId);
            return false;
            
        } catch (Exception e) {
            log.error("Error sincronizando pago: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Procesa el webhook de Wompi
     */
    public boolean processWebhook(WompiWebhookEvent event, String receivedChecksum) {
        try {
            // Verificar la firma del webhook (opcional en sandbox)
            if (receivedChecksum != null && !verifyWebhookSignature(event, receivedChecksum)) {
                log.warn("Firma de webhook inválida");
                // En sandbox, continuar aunque la firma no sea válida
                if (!wompiConfig.isSandbox()) {
                    return false;
                }
            }
            
            String eventType = event.getEvent();
            log.info("Procesando evento Wompi: {}", eventType);
            
            if ("transaction.updated".equals(eventType)) {
                WompiWebhookEvent.WompiTransaction transaction = event.getData().getTransaction();
                String status = transaction.getStatus();
                String wompiReference = transaction.getReference();
                String transactionId = transaction.getId();
                
                log.info("Transacción actualizada - ID: {}, Estado: {}, Referencia Wompi: {}", 
                        transactionId, status, wompiReference);
                
                // Intentar buscar el pago por diferentes métodos
                Optional<Pago> pagoOpt = Optional.empty();
                
                // 1. Buscar por referencia exacta
                pagoOpt = pagoRepository.findByReferenciaPago(wompiReference);
                
                // 2. Si no se encuentra, buscar por transactionId de Wompi
                if (!pagoOpt.isPresent()) {
                    pagoOpt = pagoRepository.findByWompiTransactionId(transactionId);
                }
                
                // 3. Si aún no se encuentra, extraer el SKU de la referencia de Wompi
                // El formato de Wompi puede ser: test_TvDQZA_timestamp_random o incluir el SKU
                if (!pagoOpt.isPresent() && wompiReference != null) {
                    // Buscar pagos pendientes recientes que coincidan con el monto
                    Long amountInCents = transaction.getAmountInCents();
                    if (amountInCents != null) {
                        List<Pago> pagosPendientes = pagoRepository.findByEstadoPagoAndValor(
                            Pago.EstadoPago.PENDIENTE, 
                            BigDecimal.valueOf(amountInCents / 100.0)
                        );
                        if (!pagosPendientes.isEmpty()) {
                            // Tomar el más reciente
                            pagoOpt = Optional.of(pagosPendientes.get(0));
                            log.info("Pago encontrado por monto coincidente");
                        }
                    }
                }
                
                if (pagoOpt.isPresent()) {
                    Pago pago = pagoOpt.get();
                    
                    switch (status) {
                        case "APPROVED":
                            pago.setEstadoPago(Pago.EstadoPago.PAGADO);
                            pago.setFechaPago(LocalDate.now());
                            pago.setHoraPago(LocalTime.now());
                            pago.setWompiTransactionId(transactionId);
                            pago.setReferenciaPago(wompiReference); // Actualizar con la referencia real
                            
                            // Actualizar estado de pago del estudiante a AL_DIA
                            if (pago.getEstudiante() != null) {
                                Estudiante estudiante = pago.getEstudiante();
                                estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
                                estudianteRepository.save(estudiante);
                                log.info("✅ Estado de pago del estudiante {} actualizado a AL_DIA", 
                                    estudiante.getIdEstudiante());
                            }
                            
                            log.info("✅ Pago APROBADO - ID: {}, Referencia: {}", transactionId, wompiReference);
                            break;
                            
                        case "DECLINED":
                        case "VOIDED":
                        case "ERROR":
                            pago.setEstadoPago(Pago.EstadoPago.PENDIENTE);
                            pago.setWompiTransactionId(transactionId);
                            
                            // Si el pago falla, el estudiante puede quedar en mora
                            if (pago.getEstudiante() != null) {
                                Estudiante estudiante = pago.getEstudiante();
                                if (estudiante.getEstadoPago() != Estudiante.EstadoPago.AL_DIA) {
                                    estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                                    estudianteRepository.save(estudiante);
                                }
                            }
                            
                            log.info("❌ Pago RECHAZADO/ERROR - ID: {}, Estado: {}", transactionId, status);
                            break;
                            
                        case "PENDING":
                            log.info("⏳ Pago PENDIENTE - ID: {}", transactionId);
                            break;
                            
                        default:
                            log.info("Estado de transacción no manejado: {}", status);
                    }
                    
                    pagoRepository.save(pago);
                    return true;
                } else {
                    log.warn("⚠️ No se encontró pago para la transacción - ID: {}, Ref: {}", 
                        transactionId, wompiReference);
                    
                    // Crear un registro del pago aunque no se encuentre el pendiente
                    // Esto puede pasar si el webhook llega antes de que se guarde el pago
                    log.info("Intentando crear registro de pago desde webhook...");
                    
                    if ("APPROVED".equals(status) && transaction.getCustomerEmail() != null) {
                        // Buscar estudiante por email
                        // Por ahora solo logueamos
                        log.info("Email del cliente: {}", transaction.getCustomerEmail());
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error procesando webhook de Wompi: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Verifica la firma del webhook
     */
    private boolean verifyWebhookSignature(WompiWebhookEvent event, String receivedChecksum) {
        try {
            if (event.getSignature() == null || event.getSignature().getProperties() == null) {
                log.warn("No se recibió firma en el webhook");
                return true; // En producción, cambiar a false
            }
            
            // Construir string para verificar según las properties
            String properties = event.getSignature().getProperties();
            StringBuilder dataToSign = new StringBuilder();
            
            WompiWebhookEvent.WompiTransaction tx = event.getData().getTransaction();
            
            // Las properties indican qué campos se usaron para la firma
            for (String prop : properties.split("\\.")) {
                switch (prop) {
                    case "transaction":
                        // Continuar con las sub-propiedades
                        break;
                    case "id":
                        dataToSign.append(tx.getId());
                        break;
                    case "status":
                        dataToSign.append(tx.getStatus());
                        break;
                    case "amount_in_cents":
                        dataToSign.append(tx.getAmountInCents());
                        break;
                }
            }
            
            dataToSign.append(event.getTimestamp());
            dataToSign.append(wompiConfig.getEventsSecret());
            
            String calculatedChecksum = sha256(dataToSign.toString());
            
            boolean valid = calculatedChecksum.equals(receivedChecksum);
            if (!valid) {
                log.warn("Checksum no coincide - Calculado: {}, Recibido: {}", calculatedChecksum, receivedChecksum);
            }
            
            return valid;
            
        } catch (Exception e) {
            log.error("Error verificando firma del webhook: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Crea un pago pendiente en la base de datos
     */
    private void createPendingPayment(Integer idEstudiante, BigDecimal amount, String reference, String mesPagado) {
        Optional<Estudiante> estudianteOpt = estudianteRepository.findById(idEstudiante);
        
        if (estudianteOpt.isPresent()) {
            Pago pago = new Pago();
            pago.setEstudiante(estudianteOpt.get());
            pago.setValor(amount);
            pago.setReferenciaPago(reference);
            pago.setMesPagado(mesPagado);
            pago.setMetodoPago(Pago.MetodoPago.ONLINE);
            pago.setEstadoPago(Pago.EstadoPago.PENDIENTE);
            
            pagoRepository.save(pago);
            log.info("Pago pendiente creado - Estudiante: {}, Referencia: {}", idEstudiante, reference);
        }
    }
    
    /**
     * Genera hash SHA-256
     */
    private String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generando SHA-256", e);
        }
    }
    
    /**
     * Obtiene los métodos de pago aceptados
     */
    public JsonNode getAcceptanceToken() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    wompiConfig.getApiUrl() + "/merchants/" + wompiConfig.getPublicKey(),
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode data = jsonResponse.get("data");
                
                // ✅ NUEVO: Log detallado
                log.info("===== RESPUESTA COMPLETA DE WOMPI (getMerchantInfo) =====");
                log.info("Body completo: {}", response.getBody());
                log.info("Data node: {}", data);
                log.info("=========================================================");
                
                return data;
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error obteniendo acceptance token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Confirma un pago usando el transactionId o reference
     * Consulta a Wompi y actualiza el estado del pago y estudiante
     */
    public ConfirmacionPagoResponse confirmarPago(ConfirmacionPagoRequest request) {
        try {
            // Determinar qué usar para buscar
            String transactionId = request.getTransactionId();
            String reference = request.getReference();
            
            if (transactionId == null && reference == null) {
                return ConfirmacionPagoResponse.builder()
                        .success(false)
                        .message("Debe proporcionar transactionId o reference")
                        .build();
            }
            
            // Si tenemos transactionId, consultar directamente a Wompi
            WompiTransactionResponse wompiResponse = null;
            if (transactionId != null) {
                wompiResponse = getTransactionStatus(transactionId);
            }
            
            // Si no tenemos transactionId pero tenemos reference, buscar en BD
            Optional<Pago> pagoOpt = Optional.empty();
            
            if (transactionId != null) {
                pagoOpt = pagoRepository.findByWompiTransactionId(transactionId);
            }
            
            if (!pagoOpt.isPresent() && reference != null) {
                pagoOpt = pagoRepository.findByReferenciaPago(reference);
            }
            
            // Si aún no lo encontramos, buscar por estudiante y mes
            if (!pagoOpt.isPresent() && request.getIdEstudiante() != null && request.getMesPagado() != null) {
                List<Pago> pagos = pagoRepository.findByEstudianteIdEstudiante(request.getIdEstudiante());
                pagoOpt = pagos.stream()
                        .filter(p -> request.getMesPagado().equals(p.getMesPagado()))
                        .findFirst();
            }
            
            // Construir respuesta
            ConfirmacionPagoResponse.ConfirmacionPagoResponseBuilder responseBuilder = 
                    ConfirmacionPagoResponse.builder();
            
            if (wompiResponse != null && wompiResponse.isSuccess()) {
                responseBuilder
                        .transactionId(wompiResponse.getTransactionId())
                        .reference(wompiResponse.getReference())
                        .statusWompi(wompiResponse.getStatus())
                        .paymentMethodType(wompiResponse.getPaymentMethodType())
                        .monto(BigDecimal.valueOf(wompiResponse.getAmountInCents() / 100.0))
                        .moneda(wompiResponse.getCurrency());
                
                // Si está aprobado y encontramos el pago, actualizarlo
                if ("APPROVED".equals(wompiResponse.getStatus()) && pagoOpt.isPresent()) {
                    Pago pago = pagoOpt.get();
                    boolean pagoActualizado = false;
                    boolean estudianteActualizado = false;
                    
                    // Actualizar pago si no está ya pagado
                    if (pago.getEstadoPago() != Pago.EstadoPago.PAGADO) {
                        pago.setEstadoPago(Pago.EstadoPago.PAGADO);
                        pago.setFechaPago(LocalDate.now());
                        pago.setHoraPago(LocalTime.now());
                        pago.setWompiTransactionId(transactionId);
                        if (wompiResponse.getReference() != null) {
                            pago.setReferenciaPago(wompiResponse.getReference());
                        }
                        pagoRepository.save(pago);
                        pagoActualizado = true;
                        log.info("✅ Pago confirmado y actualizado - ID: {}", transactionId);
                    }
                    
                    // Actualizar estudiante
                    if (pago.getEstudiante() != null) {
                        Estudiante estudiante = pago.getEstudiante();
                        if (estudiante.getEstadoPago() != Estudiante.EstadoPago.AL_DIA) {
                            estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
                            estudianteRepository.save(estudiante);
                            estudianteActualizado = true;
                            log.info("✅ Estudiante {} actualizado a AL_DIA", estudiante.getIdEstudiante());
                        }
                        
                        responseBuilder
                                .idEstudiante(estudiante.getIdEstudiante())
                                .nombreEstudiante(estudiante.getNombreCompleto())
                                .estadoEstudiante(estudiante.getEstadoPago().name())
                                .colorEstadoEstudiante(getColorEstadoPago(estudiante.getEstadoPago()));
                    }
                    
                    responseBuilder
                            .idPago(pago.getIdPago())
                            .estadoPago(pago.getEstadoPago().name())
                            .fechaPago(pago.getFechaPago())
                            .horaPago(pago.getHoraPago())
                            .mesPagado(pago.getMesPagado())
                            .pagoActualizado(pagoActualizado)
                            .estudianteActualizado(estudianteActualizado)
                            .success(true)
                            .message("Pago confirmado exitosamente");
                    
                } else if ("APPROVED".equals(wompiResponse.getStatus())) {
                    responseBuilder
                            .success(true)
                            .message("Pago aprobado en Wompi pero no encontrado en el sistema")
                            .pagoActualizado(false);
                } else {
                    responseBuilder
                            .success(false)
                            .message("El pago no está aprobado. Estado: " + wompiResponse.getStatus());
                }
                
            } else if (pagoOpt.isPresent()) {
                // No pudimos consultar Wompi pero tenemos el pago local
                Pago pago = pagoOpt.get();
                responseBuilder
                        .idPago(pago.getIdPago())
                        .reference(pago.getReferenciaPago())
                        .transactionId(pago.getWompiTransactionId())
                        .monto(pago.getValor())
                        .estadoPago(pago.getEstadoPago().name())
                        .fechaPago(pago.getFechaPago())
                        .horaPago(pago.getHoraPago())
                        .mesPagado(pago.getMesPagado())
                        .success(pago.getEstadoPago() == Pago.EstadoPago.PAGADO)
                        .message(pago.getEstadoPago() == Pago.EstadoPago.PAGADO 
                                ? "Pago ya confirmado anteriormente" 
                                : "Pago pendiente de confirmación");
                
                if (pago.getEstudiante() != null) {
                    Estudiante estudiante = pago.getEstudiante();
                    responseBuilder
                            .idEstudiante(estudiante.getIdEstudiante())
                            .nombreEstudiante(estudiante.getNombreCompleto())
                            .estadoEstudiante(estudiante.getEstadoPago().name())
                            .colorEstadoEstudiante(getColorEstadoPago(estudiante.getEstadoPago()));
                }
                
            } else {
                responseBuilder
                        .success(false)
                        .message("No se encontró el pago en el sistema");
            }
            
            return responseBuilder.build();
            
        } catch (Exception e) {
            log.error("Error confirmando pago: {}", e.getMessage(), e);
            return ConfirmacionPagoResponse.builder()
                    .success(false)
                    .message("Error al confirmar pago: " + e.getMessage())
                    .detalleError(e.getMessage())
                    .build();
        }
    }
    
    /**
     * Confirma un pago usando solo la referencia
     */
    public ConfirmacionPagoResponse confirmarPagoPorReferencia(String reference) {
        ConfirmacionPagoRequest request = ConfirmacionPagoRequest.builder()
                .reference(reference)
                .build();
        return confirmarPago(request);
    }
    
    /**
     * Obtiene el color hexadecimal para el estado de pago del estudiante
     */
    private String getColorEstadoPago(Estudiante.EstadoPago estadoPago) {
        if (estadoPago == null) return "#6c757d";
        switch (estadoPago) {
            case AL_DIA: return "#28a745";
            case EN_MORA: return "#dc3545";
            case PENDIENTE: return "#ffc107";
            case COMPROMISO_PAGO: return "#17a2b8";
            default: return "#6c757d";
        }
    }
}
