package galacticos_app_back.galacticos.service;

import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import galacticos_app_back.galacticos.config.TwilioConfig;
import galacticos_app_back.galacticos.dto.WhatsAppMessageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio para el envío de mensajes WhatsApp a través de Twilio.
 *
 * Mensajes implementados (relativos a fechaFin de membresía):
 *   -5 días : Recordatorio preventivo temprano
 *   -2 días : Recordatorio urgente pre-vencimiento
 *    0 días : Último día, vence hoy
 *   +1 día  : Primer aviso de mora
 *   +2 días : Segundo aviso de mora
 *   +3 días : Aviso final con link de pago urgente
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TwilioWhatsAppService {

    private final TwilioConfig twilioConfig;

    /** Link de pago configurable por variable de entorno */
    @Value("${app.pago.link:https://galacticosvoleysm.com/pagar}")
    private String linkPago;

    // ─────────────────────────────────────────────────────
    //  MÉTODOS PÚBLICOS
    // ─────────────────────────────────────────────────────

    /**
     * Envía un mensaje WhatsApp genérico.
     */
    public WhatsAppMessageResult enviarMensaje(String numeroDestino, String mensaje) {
        if (!twilioConfig.isEnabled()) {
            log.warn("⚠️ Twilio deshabilitado. Mensaje simulado para: {}", numeroDestino);
            return WhatsAppMessageResult.builder()
                    .exito(true)
                    .messageSid("SIMULATED-" + System.currentTimeMillis())
                    .mensaje("Mensaje simulado (Twilio deshabilitado)")
                    .build();
        }

        String numeroFormateado = twilioConfig.formatearNumeroWhatsApp(numeroDestino);
        if (numeroFormateado == null) {
            log.error("❌ Número inválido: {}", numeroDestino);
            return WhatsAppMessageResult.builder()
                    .exito(false)
                    .error("Número de teléfono inválido o vacío")
                    .build();
        }

        try {
            log.info("📤 Enviando WhatsApp a {} desde {}", numeroFormateado, twilioConfig.getWhatsappFrom());

            Message message = Message.creator(
                    new PhoneNumber(numeroFormateado),
                    new PhoneNumber(twilioConfig.getWhatsappFrom()),
                    mensaje
            ).create();

            log.info("✅ Mensaje enviado. SID: {}, Estado: {}", message.getSid(), message.getStatus());

            return WhatsAppMessageResult.builder()
                    .exito(true)
                    .messageSid(message.getSid())
                    .estado(message.getStatus().toString())
                    .mensaje("Mensaje enviado correctamente")
                    .build();

        } catch (ApiException e) {
            log.error("❌ Error API Twilio [{}}]: {}", e.getCode(), e.getMessage());
            return WhatsAppMessageResult.builder()
                    .exito(false)
                    .error("Error Twilio API: " + e.getMessage())
                    .codigoError(e.getCode())
                    .build();
        } catch (Exception e) {
            log.error("❌ Error inesperado: {}", e.getMessage(), e);
            return WhatsAppMessageResult.builder()
                    .exito(false)
                    .error("Error inesperado: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Envía el recordatorio correspondiente según los días respecto al vencimiento.
     *
     * @param numeroDestino    teléfono del estudiante o tutor
     * @param nombreEstudiante nombre completo del estudiante
     * @param fechaVencimiento fecha de vencimiento formateada dd/MM/yyyy
     * @param diasDiferencia   días respecto a fechaFin (-5,-2,0,+1,+2,+3)
     */
    public WhatsAppMessageResult enviarRecordatorioPago(
            String numeroDestino,
            String nombreEstudiante,
            String fechaVencimiento,
            int diasDiferencia) {

        String mensaje = construirMensaje(nombreEstudiante, fechaVencimiento, diasDiferencia);
        return enviarMensaje(numeroDestino, mensaje);
    }

    /**
     * Envía mensaje de bienvenida cuando se registra un estudiante.
     */
    public WhatsAppMessageResult enviarMensajeBienvenida(
            String numeroDestino,
            String nombreEstudiante,
            String nombreEquipo) {

        String nombre = capitalizarNombre(nombreEstudiante);
        String mensaje = String.format(
            "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
            "━━━━━━━━━━━━━━━━━━━━━\n" +
            "🎉 *¡Bienvenido/a a la Familia!*\n\n" +
            "Hola *%s* 👋\n\n" +
            "¡Nos alegra mucho que te unas a nosotros!\n\n" +
            "📋 *Tu información:*\n" +
            "   🏆 Equipo: %s\n\n" +
            "📱 Por este medio recibirás:\n" +
            "   • Recordatorios de pago de membresía\n" +
            "   • Información importante del equipo\n\n" +
            "💳 Cuando necesites pagar tu membresía puedes hacerlo en:\n" +
            "%s\n\n" +
            "¡Nos vemos en la cancha! 🌟",
            nombre, nombreEquipo, linkPago
        );
        return enviarMensaje(numeroDestino, mensaje);
    }

    /**
     * Envía confirmación de pago recibido.
     */
    public WhatsAppMessageResult enviarConfirmacionPago(
            String numeroDestino,
            String nombreEstudiante,
            String mesPagado,
            String monto,
            String nuevaFechaVencimiento) {

        String nombre = capitalizarNombre(nombreEstudiante);
        String mensaje = String.format(
            "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
            "━━━━━━━━━━━━━━━━━━━━━\n" +
            "✅ *¡Pago Recibido!*\n\n" +
            "Hola *%s* 👋\n\n" +
            "¡Gracias! Tu membresía está al día. ✅\n\n" +
            "📋 *Detalles del pago:*\n" +
            "   💰 Monto: $%s\n" +
            "   📅 Período: %s\n" +
            "   📆 Próximo vencimiento: %s\n\n" +
            "¡Sigue entrenando y dando lo mejor! 💪\n" +
            "🏐 ¡Nos vemos en la cancha!",
            nombre, monto, mesPagado, nuevaFechaVencimiento
        );
        return enviarMensaje(numeroDestino, mensaje);
    }

    /**
     * Envía un mensaje de prueba para verificar la configuración.
     */
    public WhatsAppMessageResult enviarMensajePrueba(String numeroDestino) {
        String mensaje =
            "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
            "━━━━━━━━━━━━━━━━━━━━━\n" +
            "✅ *Prueba de Conexión Exitosa*\n\n" +
            "¡Hola! Este es un mensaje de prueba.\n\n" +
            "El sistema de notificaciones de WhatsApp está funcionando correctamente.\n\n" +
            "📅 Fecha: " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n" +
            "⏰ Hora: " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + "\n\n" +
            "¡Gracias por usar nuestro sistema! 🌟";
        return enviarMensaje(numeroDestino, mensaje);
    }

    public boolean isServicioDisponible() {
        return twilioConfig.isEnabled() &&
               twilioConfig.getAccountSid() != null &&
               !twilioConfig.getAccountSid().isBlank();
    }

    // ─────────────────────────────────────────────────────
    //  CONSTRUCCIÓN DE MENSAJES
    // ─────────────────────────────────────────────────────

    private String construirMensaje(String nombre, String fechaVencimiento, int dias) {
        String n = capitalizarNombre(nombre);
        return switch (dias) {

            case -5 -> String.format(
                "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
                "━━━━━━━━━━━━━━━━━━━━━\n" +
                "📅 *Recordatorio de Membresía*\n\n" +
                "Hola *%s* 👋\n\n" +
                "Te avisamos con tiempo que tu membresía vence en *5 días*, el *%s*.\n\n" +
                "Renueva antes del vencimiento para continuar disfrutando de:\n" +
                "   ✅ Entrenamientos regulares\n" +
                "   ✅ Acceso a todas las instalaciones\n" +
                "   ✅ Participación en torneos\n\n" +
                "💳 *Paga fácil y rápido aquí:*\n" +
                "%s\n\n" +
                "¡Gracias por ser parte de la familia Galácticos! 🌟",
                n, fechaVencimiento, linkPago);

            case -2 -> String.format(
                "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
                "━━━━━━━━━━━━━━━━━━━━━\n" +
                "⏰ *¡Faltan solo 2 días!*\n\n" +
                "Hola *%s* 👋\n\n" +
                "Tu membresía vence el *%s*, es decir, en *2 días*.\n\n" +
                "⚠️ Si no renuevas a tiempo, perderás el acceso a los entrenamientos.\n\n" +
                "💳 *Renueva ahora en:*\n" +
                "%s\n\n" +
                "¿Tienes dudas sobre el pago? Responde a este mensaje y te ayudamos.\n\n" +
                "🏐 ¡Te esperamos en la cancha!",
                n, fechaVencimiento, linkPago);

            case 0 -> String.format(
                "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
                "━━━━━━━━━━━━━━━━━━━━━\n" +
                "🚨 *¡Tu membresía vence HOY!*\n\n" +
                "Hola *%s* 👋\n\n" +
                "⚠️ Hoy *%s* es el último día de tu membresía.\n\n" +
                "Para seguir entrenando *sin interrupción*, realiza tu pago hoy mismo:\n\n" +
                "💳 *Pagar ahora:*\n" +
                "%s\n\n" +
                "💡 Si no renuevas hoy, mañana tu estado pasará a *EN MORA*.\n\n" +
                "¿Necesitas ayuda? Contáctanos. ¡Gracias! 🌟",
                n, fechaVencimiento, linkPago);

            case 1 -> String.format(
                "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
                "━━━━━━━━━━━━━━━━━━━━━\n" +
                "🔴 *Membresía Vencida - 1 día en mora*\n\n" +
                "Hola *%s* 👋\n\n" +
                "Tu membresía venció ayer (*%s*) y actualmente tu cuenta está en *mora*.\n\n" +
                "Para regularizar tu situación y volver a los entrenamientos, realiza tu pago:\n\n" +
                "💳 *Pagar aquí:*\n" +
                "%s\n\n" +
                "😔 Te extrañamos en los entrenamientos. ¡Renueva y vuelve pronto! 💪",
                n, fechaVencimiento, linkPago);

            case 2 -> String.format(
                "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
                "━━━━━━━━━━━━━━━━━━━━━\n" +
                "🔴 *Membresía Vencida - 2 días en mora*\n\n" +
                "Hola *%s* 👋\n\n" +
                "Han pasado *2 días* desde que tu membresía venció el *%s*.\n\n" +
                "Recuerda que mientras estés en mora no puedes participar en los entrenamientos.\n\n" +
                "💳 *Regulariza tu pago ahora:*\n" +
                "%s\n\n" +
                "¿Tienes alguna dificultad económica? Escríbenos, podemos ayudarte. 🤝",
                n, fechaVencimiento, linkPago);

            case 3 -> String.format(
                "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
                "━━━━━━━━━━━━━━━━━━━━━\n" +
                "🚨 *URGENTE - 3 días en mora*\n\n" +
                "Hola *%s* 👋\n\n" +
                "Tu membresía lleva *3 días vencida* desde el *%s*.\n\n" +
                "⚠️ *Tu lugar en el equipo está en riesgo.*\n\n" +
                "Por favor comunícate con el *profesor encargado* para establecer un compromiso de pago y continuar entrenando.\n\n" +
                "💳 *También puedes pagar directamente en:*\n" +
                "%s\n\n" +
                "🤝 Queremos que sigas siendo parte del equipo. ¡No dejes pasar más tiempo! 🏐",
                n, fechaVencimiento, linkPago);

            default -> String.format(
                "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
                "━━━━━━━━━━━━━━━━━━━━━\n" +
                "📋 *Notificación de Membresía*\n\n" +
                "Hola *%s* 👋\n\n" +
                "Tu membresía tiene fecha de referencia: *%s*.\n\n" +
                "Para más información sobre tu estado, contáctanos o visita:\n" +
                "%s\n\n" +
                "¡Gracias por ser parte de Galácticos! 🌟",
                n, fechaVencimiento, linkPago);
        };
    }

    private String capitalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return "Estudiante";
        String[] palabras = nombre.toLowerCase().trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : palabras) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
