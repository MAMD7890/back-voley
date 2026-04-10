package galacticos_app_back.galacticos.service;

import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import galacticos_app_back.galacticos.config.TwilioConfig;
import galacticos_app_back.galacticos.dto.WhatsAppMessageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio para el envío de mensajes WhatsApp a través de Twilio.
 * 
 * Este servicio encapsula toda la lógica de comunicación con la API de Twilio,
 * proporcionando métodos de alto nivel para el envío de recordatorios de pago
 * de membresía para estudiantes de la escuela de voleibol.
 * 
 * @author Galacticos App
 * @version 2.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TwilioWhatsAppService {

    private final TwilioConfig twilioConfig;

    /**
     * Envía un mensaje de WhatsApp a un número específico.
     * 
     * @param numeroDestino número de teléfono destino (será formateado automáticamente)
     * @param mensaje contenido del mensaje a enviar
     * @return resultado del envío con información del estado
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
            log.error("❌ Número de teléfono inválido: {}", numeroDestino);
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

            log.info("✅ Mensaje enviado exitosamente. SID: {}, Estado: {}", 
                    message.getSid(), message.getStatus());

            return WhatsAppMessageResult.builder()
                    .exito(true)
                    .messageSid(message.getSid())
                    .estado(message.getStatus().toString())
                    .mensaje("Mensaje enviado correctamente")
                    .build();

        } catch (ApiException e) {
            log.error("❌ Error de API Twilio: Código {}, Mensaje: {}", e.getCode(), e.getMessage());
            return WhatsAppMessageResult.builder()
                    .exito(false)
                    .error("Error Twilio API: " + e.getMessage())
                    .codigoError(e.getCode())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error inesperado al enviar WhatsApp: {}", e.getMessage(), e);
            return WhatsAppMessageResult.builder()
                    .exito(false)
                    .error("Error inesperado: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Envía un recordatorio de pago de membresía personalizado.
     * 
     * @param numeroDestino número de teléfono destino
     * @param nombreEstudiante nombre del estudiante
     * @param fechaVencimiento fecha de vencimiento formateada
     * @param diasRestantes días hasta/desde el vencimiento (negativo = faltan, positivo = pasados)
     * @return resultado del envío
     */
    public WhatsAppMessageResult enviarRecordatorioPago(
            String numeroDestino, 
            String nombreEstudiante, 
            String fechaVencimiento,
            int diasRestantes) {
        
        String mensaje = construirMensajeRecordatorioPago(nombreEstudiante, fechaVencimiento, diasRestantes);
        return enviarMensaje(numeroDestino, mensaje);
    }

    /**
     * Construye el mensaje de recordatorio de pago de membresía según los días restantes.
     * Mensajes personalizados para la Escuela de Voleibol Galácticos.
     */
    private String construirMensajeRecordatorioPago(String nombre, String fechaVencimiento, int dias) {
        String nombreFormateado = capitalizarNombre(nombre);
        
        return switch (dias) {
            case -5 -> String.format(
                "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
                "━━━━━━━━━━━━━━━━━━━━━\n" +
                "📅 *Recordatorio de Pago*\n\n" +
                "Hola *%s* 👋\n\n" +
                "Te recordamos que tu membresía vence en *5 días* (el %s).\n\n" +
                "💰 Realiza tu pago a tiempo para continuar disfrutando de:\n" +
                "   ✅ Entrenamientos regulares\n" +
                "   ✅ Acceso a todas las instalaciones\n" +
                "   ✅ Participación en torneos\n\n" +
                "📲 Puedes pagar en línea o en nuestras oficinas.\n\n" +
                "¡Gracias por ser parte de la familia Galácticos! 🌟",
                nombreFormateado, fechaVencimiento
            );
            
            case -3 -> String.format(
                "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
                "━━━━━━━━━━━━━━━━━━━━━\n" +
                "⏰ *Recordatorio Importante*\n\n" +
                "Hola *%s* 👋\n\n" +
                "Tu membresía vence en *3 días* (el %s).\n\n" +
                "⚠️ No olvides renovar para seguir entrenando con nosotros.\n\n" +
                "💳 *Métodos de pago disponibles:*\n" +
                "   • Pago en línea (tarjeta/PSE)\n" +
                "   • Efectivo en recepción\n" +
                "   • Transferencia bancaria\n\n" +
                "¿Tienes dudas? Responde a este mensaje.\n\n" +
                "🏐 ¡Te esperamos en la cancha!",
                nombreFormateado, fechaVencimiento
            );
            
            case 0 -> String.format(
                "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
                "━━━━━━━━━━━━━━━━━━━━━\n" +
                "🚨 *¡ATENCIÓN! Vencimiento HOY*\n\n" +
                "Hola *%s* 👋\n\n" +
                "⚠️ *Tu membresía vence HOY %s*\n\n" +
                "Para continuar entrenando sin interrupciones, te invitamos a realizar tu pago lo antes posible.\n\n" +
                "💡 *Recuerda:* Si no renuevas hoy, mañana no podrás asistir a clases.\n\n" +
                "📞 ¿Necesitas ayuda? Contáctanos.\n\n" +
                "¡Gracias por entrenar con Galácticos! 🌟",
                nombreFormateado, fechaVencimiento
            );
            
            case 3 -> String.format(
                "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
                "━━━━━━━━━━━━━━━━━━━━━\n" +
                "🔔 *Membresía Vencida*\n\n" +
                "Hola *%s* 👋\n\n" +
                "Tu membresía venció hace *3 días* (desde el %s).\n\n" +
                "😔 Te extrañamos en los entrenamientos.\n\n" +
                "💪 *Renueva ahora y continúa mejorando:*\n" +
                "   • Tus habilidades técnicas\n" +
                "   • Tu condición física\n" +
                "   • Tu trabajo en equipo\n\n" +
                "📲 Realiza tu pago y vuelve a entrenar mañana mismo.\n\n" +
                "¿Tienes alguna dificultad? Escríbenos, podemos ayudarte. 🤝",
                nombreFormateado, fechaVencimiento
            );
            
            case 5 -> String.format(
                "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
                "━━━━━━━━━━━━━━━━━━━━━\n" +
                "🚨 *URGENTE - Membresía Vencida*\n\n" +
                "Hola *%s* 👋\n\n" +
                "Tu membresía lleva *5 días vencida* (desde el %s).\n\n" +
                "⚠️ *Tu lugar en el equipo está en riesgo.*\n\n" +
                "📞 *Por favor comunícate con el profe encargado* para establecer un *compromiso de pago* y poder continuar entrenando.\n\n" +
                "🤝 Queremos ayudarte a seguir siendo parte del equipo. No dejes pasar más tiempo.\n\n" +
                "💪 ¡Te esperamos de vuelta en la cancha! 🏐",
                nombreFormateado, fechaVencimiento
            );
            
            default -> String.format(
                "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
                "━━━━━━━━━━━━━━━━━━━━━\n" +
                "📋 *Notificación de Membresía*\n\n" +
                "Hola *%s* 👋\n\n" +
                "Te recordamos que tu membresía tiene fecha de vencimiento: *%s*.\n\n" +
                "Para más información sobre tu estado de cuenta, contáctanos.\n\n" +
                "¡Gracias por ser parte de Galácticos! 🌟",
                nombreFormateado, fechaVencimiento
            );
        };
    }

    /**
     * Capitaliza el nombre del estudiante (primera letra de cada palabra en mayúscula).
     */
    private String capitalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "Estudiante";
        }
        
        String[] palabras = nombre.toLowerCase().trim().split("\\s+");
        StringBuilder resultado = new StringBuilder();
        
        for (String palabra : palabras) {
            if (!palabra.isEmpty()) {
                resultado.append(Character.toUpperCase(palabra.charAt(0)))
                        .append(palabra.substring(1))
                        .append(" ");
            }
        }
        
        return resultado.toString().trim();
    }

    /**
     * Verifica si el servicio de Twilio está habilitado y configurado.
     * 
     * @return true si el servicio está operativo
     */
    public boolean isServicioDisponible() {
        return twilioConfig.isEnabled() && 
               twilioConfig.getAccountSid() != null && 
               !twilioConfig.getAccountSid().isBlank();
    }

    /**
     * Envía un mensaje de prueba para verificar la configuración.
     * 
     * @param numeroDestino número de teléfono para la prueba
     * @return resultado del envío
     */
    public WhatsAppMessageResult enviarMensajePrueba(String numeroDestino) {
        String mensajePrueba = 
            "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
            "━━━━━━━━━━━━━━━━━━━━━\n" +
            "✅ *Prueba de Conexión Exitosa*\n\n" +
            "¡Hola! Este es un mensaje de prueba.\n\n" +
            "El sistema de notificaciones de WhatsApp está funcionando correctamente.\n\n" +
            "📅 Fecha: " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n" +
            "⏰ Hora: " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + "\n\n" +
            "¡Gracias por usar nuestro sistema! 🌟";
        
        return enviarMensaje(numeroDestino, mensajePrueba);
    }

    /**
     * Envía un mensaje de bienvenida cuando un estudiante se registra.
     * 
     * @param numeroDestino número de teléfono del estudiante
     * @param nombreEstudiante nombre del estudiante
     * @param nombreEquipo nombre del equipo asignado
     * @return resultado del envío
     */
    public WhatsAppMessageResult enviarMensajeBienvenida(String numeroDestino, String nombreEstudiante, String nombreEquipo) {
        String nombreFormateado = capitalizarNombre(nombreEstudiante);
        
        String mensaje = String.format(
            "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
            "━━━━━━━━━━━━━━━━━━━━━\n" +
            "🎉 *¡Bienvenido/a a la Familia!*\n\n" +
            "Hola *%s* 👋\n\n" +
            "¡Nos alegra mucho que te unas a nosotros!\n\n" +
            "📋 *Tu información:*\n" +
            "   🏆 Equipo: %s\n\n" +
            "📱 Por este medio recibirás:\n" +
            "   • Recordatorios de pago\n" +
            "   • Información de entrenamientos\n" +
            "   • Novedades del equipo\n\n" +
            "¿Tienes preguntas? ¡Estamos para ayudarte!\n\n" +
            "¡Nos vemos en la cancha! 🌟",
            nombreFormateado, nombreEquipo
        );
        
        return enviarMensaje(numeroDestino, mensaje);
    }

    /**
     * Envía confirmación de pago recibido.
     * 
     * @param numeroDestino número de teléfono del estudiante
     * @param nombreEstudiante nombre del estudiante
     * @param mesPagado mes que se pagó
     * @param monto monto pagado
     * @param nuevaFechaVencimiento nueva fecha de vencimiento
     * @return resultado del envío
     */
    public WhatsAppMessageResult enviarConfirmacionPago(
            String numeroDestino, 
            String nombreEstudiante, 
            String mesPagado,
            String monto,
            String nuevaFechaVencimiento) {
        
        String nombreFormateado = capitalizarNombre(nombreEstudiante);
        
        String mensaje = String.format(
            "🏐 *ESCUELA DE VOLEIBOL GALÁCTICOS*\n" +
            "━━━━━━━━━━━━━━━━━━━━━\n" +
            "✅ *Pago Recibido*\n\n" +
            "Hola *%s* 👋\n\n" +
            "¡Gracias por tu pago! Tu membresía está al día.\n\n" +
            "📋 *Detalles:*\n" +
            "   💰 Monto: $%s\n" +
            "   📅 Período: %s\n" +
            "   📆 Próximo vencimiento: %s\n\n" +
            "¡Sigue entrenando y dando lo mejor! 💪\n\n" +
            "🏐 ¡Nos vemos en la cancha!",
            nombreFormateado, monto, mesPagado, nuevaFechaVencimiento
        );
        
        return enviarMensaje(numeroDestino, mensaje);
    }
}
