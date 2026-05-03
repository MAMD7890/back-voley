package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.config.MetaWhatsAppConfig;
import galacticos_app_back.galacticos.dto.WhatsAppMessageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Servicio para envío de mensajes WhatsApp mediante Meta Business Cloud API.
 *
 * Todos los mensajes salientes usan plantillas (templates) pre-aprobadas por Meta.
 * Nombres de plantillas configurados:
 *   galacticos_recordatorio_5_dias  → {{name}} {{fecha}} {{link}}
 *   galacticos_recordatorio_2_dias  → {{name}} {{fecha}} {{link}}
 *   galacticos_vence_hoy            → {{name}} {{fecha}} {{link}}
 *   galacticos_mora_1               → {{name}} {{fecha}} {{link}}
 *   galacticos_mora_2               → {{name}} {{fecha}} {{link}}
 *   galacticos_mora_3               → {{name}} {{fecha}} {{link}}
 *   galacticos_bienvenida           → {{name}} {{equipo}} {{link}}
 *   confirmacionpago                → {{name}} {{monto}} {{periodo}} {{fecha}}
 *   galacticos_prueba               → sin parámetros
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetaWhatsAppService {

    private final MetaWhatsAppConfig config;
    private final RestTemplate restTemplate;

    @Value("${app.pago.link:https://galacticosvoleysm.com/pagar}")
    private String linkPago;

    private static final String LANG = "es_CO";

    // ─────────────────────────────────────────────────────
    //  MÉTODOS PÚBLICOS
    // ─────────────────────────────────────────────────────

    public WhatsAppMessageResult enviarRecordatorioPago(
            String numeroDestino,
            String nombreEstudiante,
            String fechaVencimiento,
            int diasDiferencia) {

        String templateName = switch (diasDiferencia) {
            case -5 -> "galacticos_recordatorio_5_dias";
            case -2 -> "galacticos_recordatorio_2_dias";
            case  0 -> "galacticos_vence_hoy";
            case  1 -> "galacticos_mora_1";
            case  2 -> "galacticos_mora_2";
            case  3 -> "galacticos_mora_3";
            default -> "galacticos_recordatorio_5_dias";
        };

        String nombre = capitalizarNombre(nombreEstudiante);
        return enviarTemplate(numeroDestino, templateName, List.of(
                param("name", nombre),
                param("fecha", fechaVencimiento),
                param("link", linkPago)
        ));
    }

    public WhatsAppMessageResult enviarMensajeBienvenida(
            String numeroDestino,
            String nombreEstudiante,
            String nombreEquipo) {

        return enviarTemplate(numeroDestino, "galacticos_bienvenida", List.of(
                param("name", capitalizarNombre(nombreEstudiante)),
                param("equipo", nombreEquipo),
                param("link", linkPago)
        ));
    }

    public WhatsAppMessageResult enviarConfirmacionPago(
            String numeroDestino,
            String nombreEstudiante,
            String mesPagado,
            String monto,
            String nuevaFechaVencimiento) {

        return enviarTemplate(numeroDestino, "confirmacionpago", List.of(
                param("name", capitalizarNombre(nombreEstudiante)),
                param("monto", monto),
                param("periodo", mesPagado),
                param("fecha", nuevaFechaVencimiento)
        ));
    }

    public WhatsAppMessageResult enviarMensajePrueba(String numeroDestino) {
        return enviarTemplate(numeroDestino, "galacticos_prueba", List.of());
    }

    public WhatsAppMessageResult enviarMensaje(String numeroDestino, String mensaje) {
        if (!config.isEnabled()) return simulado(numeroDestino);

        String numero = config.formatearNumero(numeroDestino);
        if (numero == null) return error("Número inválido: " + numeroDestino);

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", numero,
                "type", "text",
                "text", Map.of("body", mensaje)
        );

        return llamarApi(body);
    }

    public boolean isServicioDisponible() {
        return config.isEnabled()
                && config.getPhoneNumberId() != null
                && !config.getPhoneNumberId().isBlank()
                && config.getAccessToken() != null
                && !config.getAccessToken().isBlank();
    }

    // ─────────────────────────────────────────────────────
    //  ENVÍO DE TEMPLATE
    // ─────────────────────────────────────────────────────

    private WhatsAppMessageResult enviarTemplate(String numeroDestino, String templateName, List<Map<String, String>> parametros) {
        if (!config.isEnabled()) return simulado(numeroDestino);

        String numero = config.formatearNumero(numeroDestino);
        if (numero == null) return error("Número inválido: " + numeroDestino);

        Map<String, Object> template = parametros.isEmpty()
                ? Map.of("name", templateName, "language", Map.of("code", LANG))
                : Map.of(
                        "name", templateName,
                        "language", Map.of("code", LANG),
                        "components", List.of(Map.of(
                                "type", "body",
                                "parameters", parametros
                        ))
                  );

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", numero,
                "type", "template",
                "template", template
        );

        log.info("📤 Enviando template '{}' a {}", templateName, numero);
        return llamarApi(body);
    }

    private WhatsAppMessageResult llamarApi(Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getAccessToken());

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    config.getApiUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    new ParameterizedTypeReference<>() {}
            );

            String messageId = extraerMessageId(response.getBody());
            log.info("✅ Mensaje enviado. ID: {}", messageId);

            return WhatsAppMessageResult.builder()
                    .exito(true)
                    .messageSid(messageId)
                    .estado("sent")
                    .mensaje("Mensaje enviado correctamente")
                    .build();

        } catch (Exception e) {
            log.error("❌ Error enviando WhatsApp: {}", e.getMessage(), e);
            return WhatsAppMessageResult.builder()
                    .exito(false)
                    .error("Error: " + e.getMessage())
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────

    private Map<String, String> param(String name, String value) {
        return Map.of("type", "text", "parameter_name", name, "text", value != null ? value : "");
    }

    private String extraerMessageId(Map<String, Object> body) {
        if (body == null) return null;
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
            if (messages != null && !messages.isEmpty()) return messages.get(0).get("id");
        } catch (Exception ignored) {}
        return null;
    }

    private WhatsAppMessageResult simulado(String numero) {
        log.warn("⚠️ Meta WhatsApp deshabilitado. Mensaje simulado para: {}", numero);
        return WhatsAppMessageResult.builder()
                .exito(true)
                .messageSid("SIMULATED-" + System.currentTimeMillis())
                .mensaje("Mensaje simulado (Meta WhatsApp deshabilitado)")
                .build();
    }

    private WhatsAppMessageResult error(String msg) {
        log.error("❌ {}", msg);
        return WhatsAppMessageResult.builder().exito(false).error(msg).build();
    }

    private String capitalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return "Estudiante";
        String[] palabras = nombre.toLowerCase().trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : palabras) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
