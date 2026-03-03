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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WompiService {
    
    // Zona horaria de Colombia para registro correcto de fechas de pago
    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");
    private static final ZoneId ZONA_UTC = ZoneId.of("UTC");
    
    private final WompiConfig wompiConfig;
    private final RestTemplate restTemplate;
    private final PagoRepository pagoRepository;
    private final EstudianteRepository estudianteRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Genera la firma de integridad para el widget de Wompi
     * Y crea el pago pendiente en la base de datos si se proporciona idEstudiante
     */
    public WompiIntegritySignature generateIntegritySignature(BigDecimal amount, String reference, String currency) {
        return generateIntegritySignature(amount, reference, currency, null, null);
    }
    
    /**
     * Genera la firma de integridad para el widget de Wompi
     * Y crea el pago pendiente en la base de datos
     */
    public WompiIntegritySignature generateIntegritySignature(BigDecimal amount, String reference, String currency, 
                                                               Integer idEstudiante, String mesPagado) {
        Long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
        
        // Concatenar: referencia + monto en centavos + moneda + secreto de integridad
        String dataToSign = reference + amountInCents + currency + wompiConfig.getIntegritySecret();
        String signature = sha256(dataToSign);
        
        // ✅ CREAR PAGO PENDIENTE EN LA BASE DE DATOS
        if (idEstudiante != null) {
            createPendingPayment(idEstudiante, amount, reference, mesPagado);
        } else {
            // Intentar extraer idEstudiante de la referencia (formato: PAY-{id}-{mes}-{random})
            try {
                String[] parts = reference.split("-");
                if (parts.length >= 3 && "PAY".equals(parts[0])) {
                    Integer extractedId = Integer.parseInt(parts[1]);
                    String extractedMes = parts[2];
                    createPendingPayment(extractedId, amount, reference, extractedMes);
                }
            } catch (Exception e) {
                log.warn("No se pudo extraer idEstudiante de la referencia: {}", reference);
            }
        }
        
        return WompiIntegritySignature.builder()
                .reference(reference)
                .amountInCents(amountInCents)
                .currency(currency)
                .integritySignature(signature)
                .publicKey(wompiConfig.getPublicKey())
                .build();
    }
    
    /**
     * Crea un pago pendiente en la base de datos
     */
    private void createPendingPayment(Integer idEstudiante, BigDecimal amount, String reference, String mesPagado) {
        try {
            // Verificar si ya existe un pago con esta referencia
            Optional<Pago> existingPago = pagoRepository.findByReferenciaPago(reference);
            if (existingPago.isPresent()) {
                log.info("Pago ya existe con referencia: {}", reference);
                return;
            }
            
            // Buscar el estudiante
            Optional<Estudiante> estudianteOpt = estudianteRepository.findById(idEstudiante);
            if (!estudianteOpt.isPresent()) {
                log.warn("Estudiante no encontrado: {}", idEstudiante);
                return;
            }
            
            // Crear el pago pendiente
            Pago pago = new Pago();
            pago.setEstudiante(estudianteOpt.get());
            pago.setValor(amount);
            pago.setReferenciaPago(reference);
            pago.setMesPagado(mesPagado);
            pago.setMetodoPago(Pago.MetodoPago.ONLINE);
            pago.setEstadoPago(Pago.EstadoPago.PENDIENTE);
            pago.setFechaPago(LocalDate.now(ZONA_COLOMBIA));
            pago.setHoraPago(LocalTime.now(ZONA_COLOMBIA));
            
            pagoRepository.save(pago);
            log.info("✅ Pago pendiente creado - Referencia: {}, Estudiante: {}, Monto: {}", 
                    reference, idEstudiante, amount);
                    
        } catch (Exception e) {
            log.error("Error creando pago pendiente: {}", e.getMessage(), e);
        }
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
    /**
     * Obtiene el estado de una transacción por referencia (buscando en BD local)
     * Útil cuando se necesita verificar el estado usando solo la referencia del pago
     */
    public WompiTransactionResponse getTransactionByReference(String reference) {
        try {
            log.info("Buscando transacción por referencia: {}", reference);
            
            // Buscar el pago en la base de datos local
            Optional<Pago> pagoOpt = pagoRepository.findByReferenciaPago(reference);
            
            if (!pagoOpt.isPresent()) {
                log.warn("No se encontró pago con referencia: {}", reference);
                return WompiTransactionResponse.builder()
                        .success(false)
                        .message("Pago no encontrado")
                        .build();
            }
            
            Pago pago = pagoOpt.get();
            
            // Mapear el estado del pago a estado de Wompi
            String status = mapEstadoPagoToWompiStatus(pago.getEstadoPago());
            
            WompiTransactionResponse response = WompiTransactionResponse.builder()
                    .transactionId(pago.getWompiTransactionId())
                    .status(status)
                    .reference(pago.getReferenciaPago())
                    .amountInCents(pago.getValor().multiply(BigDecimal.valueOf(100)).longValue())
                    .currency("COP")
                    .paymentMethodType(pago.getMetodoPago() != null ? pago.getMetodoPago().name() : null)
                    .success(true)
                    .message("Transacción encontrada")
                    .build();
            
            log.info("✅ Transacción encontrada - Reference: {}, Status: {}", reference, status);
            return response;
            
        } catch (Exception e) {
            log.error("Error consultando transacción por referencia: {}", e.getMessage(), e);
            return WompiTransactionResponse.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Mapea el estado del pago local al estado de Wompi
     */
    private String mapEstadoPagoToWompiStatus(Pago.EstadoPago estadoPago) {
        switch (estadoPago) {
            case PAGADO:
                return "APPROVED";
            case PENDIENTE:
                return "PENDING";
            case RECHAZADO:
                return "DECLINED";
            case VENCIDO:
                return "VOIDED";
            default:
                return "PENDING";
        }
    }
    
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
                log.info("Data fields - id: {}, status: {}, reference: {}, finalized_at: {}", 
                    data.get("id"), data.get("status"), data.get("reference"), data.get("finalized_at"));
                log.info("========================================================");
                
                return WompiTransactionResponse.builder()
                        .transactionId(data.get("id").asText())
                        .status(data.get("status").asText())
                        .reference(data.has("reference") ? data.get("reference").asText() : null)
                        .amountInCents(data.get("amount_in_cents").asLong())
                        .currency(data.get("currency").asText())
                        .paymentMethodType(data.has("payment_method_type") ? data.get("payment_method_type").asText() : null)
                        .createdAt(data.has("created_at") ? data.get("created_at").asText() : null)
                        .finalizedAt(data.has("finalized_at") ? data.get("finalized_at").asText() : null)
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
            
            // Convertir fecha de Wompi (UTC) a Colombia
            LocalDateTime fechaColombia = convertirFechaWompiAColombia(transaction.getFinalizedAt());
            
            log.info("Sincronizando pago - ID: {}, Estado: {}, Referencia: {}, Fecha Wompi: {}", 
                transactionId, status, reference, fechaColombia);
            
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
                    pago.setFechaPago(fechaColombia.toLocalDate());
                    pago.setHoraPago(fechaColombia.toLocalTime());
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
                    pago.setEstadoPago(Pago.EstadoPago.RECHAZADO);
                    pago.setFechaPago(fechaColombia.toLocalDate());
                    pago.setHoraPago(fechaColombia.toLocalTime());
                    pago.setWompiTransactionId(transactionId);
                    if (reference != null) {
                        pago.setReferenciaPago(reference);
                    }
                    
                    // Actualizar estado del estudiante a EN_MORA
                    if (pago.getEstudiante() != null) {
                        Estudiante estudiante = pago.getEstudiante();
                        if (estudiante.getEstadoPago() != Estudiante.EstadoPago.AL_DIA) {
                            estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                            estudianteRepository.save(estudiante);
                            log.info("❌ Estudiante {} marcado como EN_MORA por rechazo", estudiante.getIdEstudiante());
                        }
                    }
                    
                    pagoRepository.save(pago);
                    log.info("❌ Pago rechazado sincronizado - ID: {}, Estado: {}", transactionId, status);
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
     * Sincroniza masivamente las fechas de todos los pagos de Wompi
     * Consulta la API de Wompi para cada pago y actualiza la fecha/hora correcta
     * @return Map con estadísticas de la sincronización
     */
    public Map<String, Object> sincronizarFechasPagosWompi() {
        Map<String, Object> resultado = new HashMap<>();
        List<Map<String, Object>> detalles = new ArrayList<>();
        
        int totalPagos = 0;
        int actualizados = 0;
        int errores = 0;
        int sinCambios = 0;
        
        try {
            List<Pago> pagosWompi = pagoRepository.findAllWithWompiTransactionId();
            totalPagos = pagosWompi.size();
            
            log.info("🔄 Iniciando sincronización masiva de {} pagos de Wompi", totalPagos);
            
            for (Pago pago : pagosWompi) {
                Map<String, Object> detallePago = new HashMap<>();
                detallePago.put("idPago", pago.getIdPago());
                detallePago.put("transactionId", pago.getWompiTransactionId());
                detallePago.put("fechaAnterior", pago.getFechaPago() != null ? pago.getFechaPago().toString() : null);
                detallePago.put("horaAnterior", pago.getHoraPago() != null ? pago.getHoraPago().toString() : null);
                
                try {
                    // Consultar la transacción en Wompi
                    WompiTransactionResponse wompiResponse = getTransactionStatus(pago.getWompiTransactionId());
                    
                    if (wompiResponse.isSuccess() && wompiResponse.getFinalizedAt() != null) {
                        // Convertir fecha de Wompi a Colombia
                        LocalDateTime fechaColombia = convertirFechaWompiAColombia(wompiResponse.getFinalizedAt());
                        
                        LocalDate nuevaFecha = fechaColombia.toLocalDate();
                        java.time.LocalTime nuevaHora = fechaColombia.toLocalTime();
                        
                        // Verificar si hay cambios
                        boolean fechaCambiada = !nuevaFecha.equals(pago.getFechaPago());
                        boolean horaCambiada = pago.getHoraPago() == null || !nuevaHora.equals(pago.getHoraPago());
                        
                        if (fechaCambiada || horaCambiada) {
                            pago.setFechaPago(nuevaFecha);
                            pago.setHoraPago(nuevaHora);
                            pagoRepository.save(pago);
                            
                            detallePago.put("fechaNueva", nuevaFecha.toString());
                            detallePago.put("horaNueva", nuevaHora.toString());
                            detallePago.put("estado", "ACTUALIZADO");
                            detallePago.put("wompiTimestamp", wompiResponse.getFinalizedAt());
                            actualizados++;
                            
                            log.info("✅ Pago {} actualizado: {} {} -> {} {}", 
                                pago.getIdPago(), 
                                detallePago.get("fechaAnterior"), detallePago.get("horaAnterior"),
                                nuevaFecha, nuevaHora);
                        } else {
                            detallePago.put("estado", "SIN_CAMBIOS");
                            sinCambios++;
                        }
                    } else {
                        detallePago.put("estado", "ERROR_WOMPI");
                        detallePago.put("mensaje", wompiResponse.getMessage());
                        errores++;
                        log.warn("⚠️ No se pudo obtener fecha de Wompi para pago {}: {}", 
                            pago.getIdPago(), wompiResponse.getMessage());
                    }
                } catch (Exception e) {
                    detallePago.put("estado", "ERROR");
                    detallePago.put("mensaje", e.getMessage());
                    errores++;
                    log.error("❌ Error sincronizando pago {}: {}", pago.getIdPago(), e.getMessage());
                }
                
                detalles.add(detallePago);
                
                // Pequeña pausa para no saturar la API de Wompi
                Thread.sleep(100);
            }
            
            log.info("✅ Sincronización completada: {} total, {} actualizados, {} sin cambios, {} errores",
                totalPagos, actualizados, sinCambios, errores);
                
        } catch (Exception e) {
            log.error("❌ Error en sincronización masiva: {}", e.getMessage(), e);
            resultado.put("error", e.getMessage());
        }
        
        resultado.put("totalPagos", totalPagos);
        resultado.put("actualizados", actualizados);
        resultado.put("sinCambios", sinCambios);
        resultado.put("errores", errores);
        resultado.put("detalles", detalles);
        
        return resultado;
    }
    
    /**
     * Sincroniza todos los pagos PENDIENTES online consultando la API de Wompi.
     * Este método es útil cuando los webhooks no llegaron o los pagos quedaron sin actualizar.
     * 
     * Para cada pago pendiente:
     * 1. Si tiene wompiTransactionId, consulta el estado en Wompi
     * 2. Si el pago está APPROVED en Wompi, actualiza el estado del pago y del estudiante
     * 
     * @return Map con estadísticas de la sincronización
     */
    public Map<String, Object> sincronizarPagosPendientes() {
        Map<String, Object> resultado = new HashMap<>();
        List<Map<String, Object>> detalles = new ArrayList<>();
        
        int totalPagos = 0;
        int actualizados = 0;
        int sinTransactionId = 0;
        int yaAprobados = 0;
        int rechazados = 0;
        int errores = 0;
        
        try {
            List<Pago> pagosPendientes = pagoRepository.findPagosPendientesOnline();
            totalPagos = pagosPendientes.size();
            
            log.info("🔄 Iniciando sincronización de {} pagos pendientes ONLINE", totalPagos);
            
            for (Pago pago : pagosPendientes) {
                Map<String, Object> detallePago = new HashMap<>();
                detallePago.put("idPago", pago.getIdPago());
                detallePago.put("referencia", pago.getReferenciaPago());
                detallePago.put("transactionId", pago.getWompiTransactionId());
                detallePago.put("estudiante", pago.getEstudiante() != null ? 
                    pago.getEstudiante().getIdEstudiante() + " - " + pago.getEstudiante().getNombreCompleto() : "N/A");
                
                try {
                    // Si tiene wompiTransactionId, consultar directamente
                    if (pago.getWompiTransactionId() != null && !pago.getWompiTransactionId().isEmpty()) {
                        WompiTransactionResponse wompiResponse = getTransactionStatus(pago.getWompiTransactionId());
                        
                        if (wompiResponse.isSuccess()) {
                            String statusWompi = wompiResponse.getStatus();
                            detallePago.put("statusWompi", statusWompi);
                            
                            if ("APPROVED".equals(statusWompi)) {
                                // Convertir fecha de Wompi a Colombia
                                LocalDateTime fechaColombia = convertirFechaWompiAColombia(wompiResponse.getFinalizedAt());
                                
                                // Actualizar el pago
                                pago.setEstadoPago(Pago.EstadoPago.PAGADO);
                                pago.setFechaPago(fechaColombia.toLocalDate());
                                pago.setHoraPago(fechaColombia.toLocalTime());
                                pagoRepository.save(pago);
                                
                                // Actualizar el estudiante
                                if (pago.getEstudiante() != null) {
                                    Estudiante estudiante = pago.getEstudiante();
                                    estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
                                    estudianteRepository.save(estudiante);
                                    detallePago.put("estudianteActualizado", true);
                                }
                                
                                detallePago.put("estado", "ACTUALIZADO_A_PAGADO");
                                actualizados++;
                                log.info("✅ Pago {} actualizado a PAGADO - Estudiante: {}", 
                                    pago.getIdPago(), pago.getEstudiante() != null ? pago.getEstudiante().getNombreCompleto() : "N/A");
                                
                            } else if ("DECLINED".equals(statusWompi) || "VOIDED".equals(statusWompi) || "ERROR".equals(statusWompi)) {
                                pago.setEstadoPago(Pago.EstadoPago.RECHAZADO);
                                pagoRepository.save(pago);
                                detallePago.put("estado", "ACTUALIZADO_A_RECHAZADO");
                                rechazados++;
                                log.info("❌ Pago {} actualizado a RECHAZADO - Status Wompi: {}", pago.getIdPago(), statusWompi);
                                
                            } else {
                                detallePago.put("estado", "PENDIENTE_EN_WOMPI");
                                log.info("⏳ Pago {} sigue pendiente en Wompi", pago.getIdPago());
                            }
                        } else {
                            detallePago.put("estado", "ERROR_CONSULTA_WOMPI");
                            detallePago.put("mensaje", wompiResponse.getMessage());
                            errores++;
                        }
                    } else {
                        detallePago.put("estado", "SIN_TRANSACTION_ID");
                        sinTransactionId++;
                    }
                    
                } catch (Exception e) {
                    detallePago.put("estado", "ERROR");
                    detallePago.put("mensaje", e.getMessage());
                    errores++;
                    log.error("❌ Error procesando pago {}: {}", pago.getIdPago(), e.getMessage());
                }
                
                detalles.add(detallePago);
                
                // Pequeña pausa para no saturar la API de Wompi
                Thread.sleep(100);
            }
            
            log.info("✅ Sincronización de pagos pendientes completada: {} total, {} actualizados, {} rechazados, {} sin transactionId, {} errores",
                totalPagos, actualizados, rechazados, sinTransactionId, errores);
                
        } catch (Exception e) {
            log.error("❌ Error en sincronización de pagos pendientes: {}", e.getMessage(), e);
            resultado.put("error", e.getMessage());
        }
        
        resultado.put("totalPagosPendientes", totalPagos);
        resultado.put("actualizadosAPagado", actualizados);
        resultado.put("actualizadosARechazado", rechazados);
        resultado.put("sinTransactionId", sinTransactionId);
        resultado.put("errores", errores);
        resultado.put("detalles", detalles);
        
        return resultado;
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
                
                // Convertir fecha de Wompi (UTC) a Colombia
                LocalDateTime fechaColombia = convertirFechaWompiAColombia(transaction.getFinalizedAt());
                
                log.info("Transacción actualizada - ID: {}, Estado: {}, Referencia Wompi: {}, Fecha Wompi: {}", 
                        transactionId, status, wompiReference, fechaColombia);
                
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
                            pago.setFechaPago(fechaColombia.toLocalDate());
                            pago.setHoraPago(fechaColombia.toLocalTime());
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
                            pago.setEstadoPago(Pago.EstadoPago.RECHAZADO);
                            pago.setWompiTransactionId(transactionId);
                            pago.setFechaPago(fechaColombia.toLocalDate());
                            pago.setHoraPago(fechaColombia.toLocalTime());
                            
                            // Si el pago falla, el estudiante puede quedar en mora
                            if (pago.getEstudiante() != null) {
                                Estudiante estudiante = pago.getEstudiante();
                                if (estudiante.getEstadoPago() != Estudiante.EstadoPago.AL_DIA) {
                                    estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                                    estudianteRepository.save(estudiante);
                                    log.info("❌ Estudiante {} marcado como EN_MORA por rechazo de pago", 
                                        estudiante.getIdEstudiante());
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
                    
                    if ("APPROVED".equals(status)) {
                        // Intentar crear el pago automáticamente desde la referencia
                        Pago nuevoPago = createPaymentFromWebhook(
                            wompiReference, 
                            transactionId, 
                            transaction.getAmountInCents(),
                            transaction.getFinalizedAt(),
                            transaction.getCustomerEmail()
                        );
                        
                        if (nuevoPago != null) {
                            log.info("✅ Pago APROBADO creado automáticamente desde webhook - ID: {}, Estudiante: {}", 
                                transactionId, nuevoPago.getEstudiante() != null ? nuevoPago.getEstudiante().getIdEstudiante() : "N/A");
                            return true;
                        } else {
                            log.warn("❌ No se pudo crear el pago automáticamente. Referencia: {}, Email: {}", 
                                wompiReference, transaction.getCustomerEmail());
                        }
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
     * Convierte una fecha de Wompi (formato ISO 8601 UTC) a LocalDateTime en zona Colombia
     * Formato esperado: "2026-03-02T14:03:07.000Z" o "2026-03-02T14:03:07.000+00:00"
     * @param wompiTimestamp El timestamp de Wompi en UTC
     * @return LocalDateTime en zona horaria de Colombia, o fecha/hora actual de Colombia si hay error
     */
    private LocalDateTime convertirFechaWompiAColombia(String wompiTimestamp) {
        if (wompiTimestamp == null || wompiTimestamp.isEmpty()) {
            log.warn("Timestamp de Wompi nulo o vacío, usando fecha actual de Colombia");
            return LocalDateTime.now(ZONA_COLOMBIA);
        }
        
        try {
            // Parsear el timestamp ISO 8601 de Wompi
            Instant instant = Instant.parse(wompiTimestamp);
            
            // Convertir de UTC a zona de Colombia
            ZonedDateTime fechaUTC = instant.atZone(ZONA_UTC);
            ZonedDateTime fechaColombia = fechaUTC.withZoneSameInstant(ZONA_COLOMBIA);
            
            log.debug("Fecha Wompi UTC: {} -> Colombia: {}", wompiTimestamp, fechaColombia);
            return fechaColombia.toLocalDateTime();
            
        } catch (DateTimeParseException e) {
            log.warn("Error parseando fecha de Wompi '{}': {}. Usando fecha actual de Colombia", 
                    wompiTimestamp, e.getMessage());
            return LocalDateTime.now(ZONA_COLOMBIA);
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
                // Convertir fecha de Wompi (UTC) a Colombia
                LocalDateTime fechaColombia = convertirFechaWompiAColombia(wompiResponse.getFinalizedAt());
                
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
                        pago.setFechaPago(fechaColombia.toLocalDate());
                        pago.setHoraPago(fechaColombia.toLocalTime());
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
                    
                } else if ("DECLINED".equals(wompiResponse.getStatus()) || 
                          "VOIDED".equals(wompiResponse.getStatus()) || 
                          "ERROR".equals(wompiResponse.getStatus())) {
                    
                    // Manejar pagos rechazados
                    if (pagoOpt.isPresent()) {
                        Pago pago = pagoOpt.get();
                        boolean pagoActualizado = false;
                        boolean estudianteActualizado = false;
                        
                        if (pago.getEstadoPago() != Pago.EstadoPago.RECHAZADO) {
                            pago.setEstadoPago(Pago.EstadoPago.RECHAZADO);
                            pago.setFechaPago(fechaColombia.toLocalDate());
                            pago.setHoraPago(fechaColombia.toLocalTime());
                            pago.setWompiTransactionId(transactionId);
                            if (wompiResponse.getReference() != null) {
                                pago.setReferenciaPago(wompiResponse.getReference());
                            }
                            pagoRepository.save(pago);
                            pagoActualizado = true;
                            log.info("❌ Pago marcado como RECHAZADO - ID: {}", transactionId);
                        }
                        
                        // Actualizar estudiante a EN_MORA
                        if (pago.getEstudiante() != null) {
                            Estudiante estudiante = pago.getEstudiante();
                            if (estudiante.getEstadoPago() != Estudiante.EstadoPago.AL_DIA) {
                                estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                                estudianteRepository.save(estudiante);
                                estudianteActualizado = true;
                                log.info("❌ Estudiante {} marcado como EN_MORA", estudiante.getIdEstudiante());
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
                                .success(false)
                                .message("Pago rechazado por Wompi. Estado: " + wompiResponse.getStatus());
                    } else {
                        responseBuilder
                                .success(false)
                                .message("Pago rechazado pero no encontrado en el sistema. Estado: " + wompiResponse.getStatus());
                    }
                    
                } else if ("APPROVED".equals(wompiResponse.getStatus())) {
                    // ✅ AUTO-CREAR el pago si está aprobado en Wompi pero no existe en BD
                    log.info("Pago APPROVED en Wompi pero no existe en BD. Auto-creando para referencia: {}", reference);
                    
                    // Crear response temporal con el monto de Wompi para pasarlo al método
                    ConfirmacionPagoResponse tempResponse = ConfirmacionPagoResponse.builder()
                            .monto(BigDecimal.valueOf(wompiResponse.getAmountInCents() / 100.0))
                            .build();
                    
                    Pago nuevoPago = createPaymentFromReference(
                            wompiResponse.getReference() != null ? wompiResponse.getReference() : reference, 
                            transactionId, 
                            tempResponse,
                            wompiResponse.getFinalizedAt());
                    
                    if (nuevoPago != null) {
                        responseBuilder
                                .success(true)
                                .message("Pago creado y confirmado exitosamente")
                                .idPago(nuevoPago.getIdPago())
                                .estadoPago(nuevoPago.getEstadoPago().name())
                                .fechaPago(nuevoPago.getFechaPago())
                                .horaPago(nuevoPago.getHoraPago())
                                .mesPagado(nuevoPago.getMesPagado())
                                .pagoActualizado(true)
                                .estudianteActualizado(true);
                        
                        if (nuevoPago.getEstudiante() != null) {
                            responseBuilder
                                    .idEstudiante(nuevoPago.getEstudiante().getIdEstudiante())
                                    .nombreEstudiante(nuevoPago.getEstudiante().getNombreCompleto())
                                    .estadoEstudiante(nuevoPago.getEstudiante().getEstadoPago().name())
                                    .colorEstadoEstudiante(getColorEstadoPago(nuevoPago.getEstudiante().getEstadoPago()));
                        }
                    } else {
                        responseBuilder
                                .success(true)
                                .message("Pago aprobado en Wompi pero no se pudo crear en el sistema (verifique la referencia)")
                                .pagoActualizado(false);
                    }
                } else {
                    responseBuilder
                            .success(false)
                            .message("El pago tiene estado desconocido: " + wompiResponse.getStatus());
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
        return confirmarPagoPorReferencia(reference, null);
    }
    
    /**
     * Confirma un pago usando referencia y opcionalmente transactionId
     * Si el pago no existe en BD pero está aprobado en Wompi, lo crea automáticamente
     */
    public ConfirmacionPagoResponse confirmarPagoPorReferencia(String reference, String transactionId) {
        // Si tenemos transactionId, usar el método completo
        if (transactionId != null && !transactionId.isEmpty()) {
            ConfirmacionPagoRequest request = ConfirmacionPagoRequest.builder()
                    .reference(reference)
                    .transactionId(transactionId)
                    .build();
            
            ConfirmacionPagoResponse response = confirmarPago(request);
            
            // Si el pago está aprobado pero no existe en el sistema, crear el pago
            if (response.isSuccess() && response.getIdPago() == null && 
                "APPROVED".equals(response.getStatusWompi())) {
                
                log.info("Pago aprobado pero no existe en BD. Creando pago para referencia: {}", reference);
                Pago nuevoPago = createPaymentFromReference(reference, transactionId, response, null);
                
                if (nuevoPago != null) {
                    response = ConfirmacionPagoResponse.builder()
                            .success(true)
                            .message("Pago creado y confirmado exitosamente")
                            .transactionId(transactionId)
                            .reference(reference)
                            .statusWompi(response.getStatusWompi())
                            .paymentMethodType(response.getPaymentMethodType())
                            .monto(response.getMonto())
                            .moneda(response.getMoneda())
                            .idPago(nuevoPago.getIdPago())
                            .estadoPago(nuevoPago.getEstadoPago().name())
                            .fechaPago(nuevoPago.getFechaPago())
                            .horaPago(nuevoPago.getHoraPago())
                            .mesPagado(nuevoPago.getMesPagado())
                            .idEstudiante(nuevoPago.getEstudiante() != null ? nuevoPago.getEstudiante().getIdEstudiante() : null)
                            .nombreEstudiante(nuevoPago.getEstudiante() != null ? nuevoPago.getEstudiante().getNombreCompleto() : null)
                            .pagoActualizado(true)
                            .estudianteActualizado(true)
                            .build();
                }
            }
            
            return response;
        }
        
        // Solo referencia - buscar en BD
        ConfirmacionPagoRequest request = ConfirmacionPagoRequest.builder()
                .reference(reference)
                .build();
        return confirmarPago(request);
    }
    
    /**
     * Crea un pago a partir de la referencia cuando el pago está aprobado en Wompi pero no existe en BD
     * @param finalizedAt Timestamp de Wompi en formato ISO 8601 UTC (ej: "2026-03-02T14:03:07.000Z")
     */
    private Pago createPaymentFromReference(String reference, String transactionId, ConfirmacionPagoResponse wompiResponse, String finalizedAt) {
        try {
            // Convertir fecha de Wompi (UTC) a Colombia
            LocalDateTime fechaColombia = convertirFechaWompiAColombia(finalizedAt);
            
            // Extraer datos de la referencia (formato: PAY-{idEstudiante}-{mes}-{random})
            String[] parts = reference.split("-");
            if (parts.length < 4 || !"PAY".equals(parts[0])) {
                log.warn("Formato de referencia inválido: {}", reference);
                return null;
            }
            
            Integer idEstudiante = Integer.parseInt(parts[1]);
            String mesPagado = parts[2] + "-" + parts[3].substring(0, Math.min(2, parts[3].length()));
            
            // Ajustar formato del mes si es necesario (puede ser 2025-02 o similar)
            if (parts.length >= 4 && parts[2].length() == 4) {
                // El formato parece ser PAY-{id}-{año}-{mes}-{random}
                mesPagado = parts[2] + "-" + parts[3];
                // Si hay más partes, el random está después
            }
            
            // Buscar estudiante
            Optional<Estudiante> estudianteOpt = estudianteRepository.findById(idEstudiante);
            if (!estudianteOpt.isPresent()) {
                log.warn("Estudiante no encontrado: {}", idEstudiante);
                return null;
            }
            
            Estudiante estudiante = estudianteOpt.get();
            
            // Crear el pago
            Pago pago = new Pago();
            pago.setEstudiante(estudiante);
            pago.setValor(wompiResponse.getMonto() != null ? wompiResponse.getMonto() : BigDecimal.ZERO);
            pago.setReferenciaPago(reference);
            pago.setWompiTransactionId(transactionId);
            pago.setMesPagado(mesPagado);
            pago.setMetodoPago(Pago.MetodoPago.ONLINE);
            pago.setEstadoPago(Pago.EstadoPago.PAGADO);
            pago.setFechaPago(fechaColombia.toLocalDate());
            pago.setHoraPago(fechaColombia.toLocalTime());
            
            Pago pagoGuardado = pagoRepository.save(pago);
            
            // Actualizar estudiante
            estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
            estudianteRepository.save(estudiante);
            
            log.info("✅ Pago creado automáticamente - ID: {}, Estudiante: {}, Referencia: {}", 
                    pagoGuardado.getIdPago(), idEstudiante, reference);
            
            return pagoGuardado;
            
        } catch (Exception e) {
            log.error("Error creando pago desde referencia: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Crea un pago automáticamente desde el webhook de Wompi cuando no se encuentra el pago pendiente.
     * Prioridad de búsqueda del estudiante:
     * 1. Extraer ID del estudiante de la referencia (formato: PAY-{idEstudiante}-{mes}-{random})
     * 2. Buscar estudiante por correo electrónico (fallback)
     * 
     * IMPORTANTE: El correo del cliente en Wompi NO necesita coincidir con el correo registrado
     * del estudiante. El sistema siempre intentará vincular por la referencia del pago primero.
     * 
     * @param wompiReference Referencia de la transacción de Wompi
     * @param transactionId ID de la transacción de Wompi
     * @param amountInCents Monto en centavos
     * @param finalizedAt Timestamp de finalización de Wompi
     * @param customerEmail Email del cliente (usado solo como fallback)
     * @return Pago creado o null si no se pudo crear
     */
    private Pago createPaymentFromWebhook(String wompiReference, String transactionId, 
            Long amountInCents, String finalizedAt, String customerEmail) {
        try {
            LocalDateTime fechaColombia = convertirFechaWompiAColombia(finalizedAt);
            BigDecimal monto = amountInCents != null ? BigDecimal.valueOf(amountInCents / 100.0) : BigDecimal.ZERO;
            
            Estudiante estudiante = null;
            String mesPagado = null;
            
            // ESTRATEGIA 1: Extraer ID del estudiante de la referencia
            // Formato esperado: PAY-{idEstudiante}-{año}-{mes}-{random} o PAY-{idEstudiante}-{mes}-{random}
            if (wompiReference != null && wompiReference.startsWith("PAY-")) {
                try {
                    String[] parts = wompiReference.split("-");
                    if (parts.length >= 3) {
                        Integer idEstudiante = Integer.parseInt(parts[1]);
                        
                        // Extraer el mes del pago
                        if (parts.length >= 5 && parts[2].length() == 4) {
                            // Formato: PAY-{id}-{año}-{mes}-{random}
                            mesPagado = parts[2] + "-" + parts[3];
                        } else if (parts.length >= 4) {
                            // Formato: PAY-{id}-{mes}-{random}
                            mesPagado = parts[2];
                        }
                        
                        Optional<Estudiante> estudianteOpt = estudianteRepository.findById(idEstudiante);
                        if (estudianteOpt.isPresent()) {
                            estudiante = estudianteOpt.get();
                            log.info("✅ Estudiante encontrado por referencia: ID={}, Nombre={}", 
                                idEstudiante, estudiante.getNombreCompleto());
                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("No se pudo extraer ID del estudiante de la referencia: {}", wompiReference);
                }
            }
            
            // ESTRATEGIA 2: Buscar por correo electrónico (fallback)
            // Solo se usa si no se encontró el estudiante por referencia
            if (estudiante == null && customerEmail != null && !customerEmail.isEmpty()) {
                log.info("Buscando estudiante por email: {}", customerEmail);
                Optional<Estudiante> estudianteOpt = estudianteRepository.findByCorreoEstudiante(customerEmail);
                if (estudianteOpt.isPresent()) {
                    estudiante = estudianteOpt.get();
                    log.info("✅ Estudiante encontrado por email: ID={}, Nombre={}", 
                        estudiante.getIdEstudiante(), estudiante.getNombreCompleto());
                    
                    // Generar mes de pago si no se pudo extraer de la referencia
                    if (mesPagado == null) {
                        mesPagado = fechaColombia.getYear() + "-" + String.format("%02d", fechaColombia.getMonthValue());
                    }
                }
            }
            
            // Si no se encontró estudiante, no podemos crear el pago
            if (estudiante == null) {
                log.warn("❌ No se encontró estudiante para crear el pago. Referencia: {}, Email: {}", 
                    wompiReference, customerEmail);
                return null;
            }
            
            // Verificar si ya existe un pago con esta transacción
            Optional<Pago> existingPago = pagoRepository.findByWompiTransactionId(transactionId);
            if (existingPago.isPresent()) {
                log.info("Pago ya existe para transacción: {}", transactionId);
                return existingPago.get();
            }
            
            // Crear el pago
            Pago pago = new Pago();
            pago.setEstudiante(estudiante);
            pago.setValor(monto);
            pago.setReferenciaPago(wompiReference);
            pago.setWompiTransactionId(transactionId);
            pago.setMesPagado(mesPagado != null ? mesPagado : fechaColombia.getYear() + "-" + String.format("%02d", fechaColombia.getMonthValue()));
            pago.setMetodoPago(Pago.MetodoPago.ONLINE);
            pago.setEstadoPago(Pago.EstadoPago.PAGADO);
            pago.setFechaPago(fechaColombia.toLocalDate());
            pago.setHoraPago(fechaColombia.toLocalTime());
            
            Pago pagoGuardado = pagoRepository.save(pago);
            
            // Actualizar estado del estudiante a AL_DIA
            estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
            estudianteRepository.save(estudiante);
            
            log.info("✅ Pago creado automáticamente desde webhook - ID Pago: {}, Estudiante: {} ({}), Monto: {}, Mes: {}", 
                pagoGuardado.getIdPago(), 
                estudiante.getIdEstudiante(), 
                estudiante.getNombreCompleto(),
                monto,
                mesPagado);
            
            return pagoGuardado;
            
        } catch (Exception e) {
            log.error("Error creando pago desde webhook: {}", e.getMessage(), e);
            return null;
        }
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
