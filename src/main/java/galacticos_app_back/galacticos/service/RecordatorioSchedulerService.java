package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.WhatsAppMessageResult;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Membresia;
import galacticos_app_back.galacticos.entity.RecordatorioPago;
import galacticos_app_back.galacticos.entity.RecordatorioPago.EstadoEnvio;
import galacticos_app_back.galacticos.entity.RecordatorioPago.TipoRecordatorio;
import galacticos_app_back.galacticos.repository.MembresiaRepository;
import galacticos_app_back.galacticos.repository.RecordatorioPagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio programado para el envío automático de recordatorios de pago por WhatsApp.
 * 
 * Este servicio se ejecuta diariamente y procesa las membresías que requieren
 * notificación según las reglas de negocio establecidas:
 * - 5 días antes del vencimiento
 * - 3 días antes del vencimiento
 * - El día del vencimiento
 * - 3 días después del vencimiento
 * - 5 días después del vencimiento
 * 
 * @author Galacticos App
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RecordatorioSchedulerService {

    private final MembresiaRepository membresiaRepository;
    private final RecordatorioPagoRepository recordatorioPagoRepository;
    private final TwilioWhatsAppService twilioWhatsAppService;

    @Value("${recordatorio.max-reintentos:3}")
    private int maxReintentos;

    @Value("${recordatorio.enabled:true}")
    private boolean recordatoriosEnabled;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Días en los que se envían recordatorios (relativos a fecha de vencimiento)
    private static final int[] DIAS_RECORDATORIO = {-5, -3, 0, 3, 5};

    /**
     * Tarea programada que se ejecuta diariamente a las 8:00 AM.
     * Procesa todas las membresías y envía los recordatorios correspondientes.
     * 
     * Cron: segundo minuto hora día-mes mes día-semana
     * "0 0 8 * * *" = todos los días a las 8:00:00 AM
     */
    @Scheduled(cron = "${recordatorio.cron:0 0 8 * * *}")
    @Transactional
    public void ejecutarRecordatoriosDiarios() {
        if (!recordatoriosEnabled) {
            log.info("⏸️ Sistema de recordatorios DESHABILITADO");
            return;
        }

        log.info("🚀 ====== INICIANDO PROCESO DE RECORDATORIOS DIARIOS ======");
        log.info("📅 Fecha de ejecución: {}", LocalDate.now().format(DATE_FORMATTER));

        LocalDate hoy = LocalDate.now();
        Map<String, Integer> estadisticas = new HashMap<>();
        estadisticas.put("procesadas", 0);
        estadisticas.put("enviadas", 0);
        estadisticas.put("omitidas", 0);
        estadisticas.put("fallidas", 0);

        // Procesar cada día de recordatorio
        for (int diasDiferencia : DIAS_RECORDATORIO) {
            procesarRecordatoriosPorDia(hoy, diasDiferencia, estadisticas);
        }

        // Procesar reintentos de mensajes fallidos
        procesarReintentos(estadisticas);

        log.info("📊 ====== RESUMEN DE EJECUCIÓN ======");
        log.info("   📋 Membresías procesadas: {}", estadisticas.get("procesadas"));
        log.info("   ✅ Mensajes enviados: {}", estadisticas.get("enviadas"));
        log.info("   ⏭️ Mensajes omitidos (ya enviados): {}", estadisticas.get("omitidas"));
        log.info("   ❌ Mensajes fallidos: {}", estadisticas.get("fallidas"));
        log.info("🏁 ====== FIN DEL PROCESO ======\n");
    }

    /**
     * Procesa los recordatorios para un día específico relativo al vencimiento.
     */
    private void procesarRecordatoriosPorDia(LocalDate hoy, int diasDiferencia, Map<String, Integer> estadisticas) {
        // Calcular la fecha de vencimiento que corresponde
        // Si diasDiferencia = -5, buscamos membresías que vencen en hoy + 5 días
        // Si diasDiferencia = 3, buscamos membresías que vencieron hace 3 días (hoy - 3)
        LocalDate fechaVencimientoBuscada = hoy.minusDays(diasDiferencia);
        
        TipoRecordatorio tipoRecordatorio = TipoRecordatorio.fromDiasDiferencia(diasDiferencia);
        
        if (tipoRecordatorio == null) {
            log.warn("⚠️ No se encontró tipo de recordatorio para {} días", diasDiferencia);
            return;
        }

        log.info("🔍 Procesando recordatorios: {} (Fecha vencimiento: {})", 
                tipoRecordatorio.getDescripcion(), 
                fechaVencimientoBuscada.format(DATE_FORMATTER));

        // Buscar membresías con esa fecha de vencimiento
        List<Membresia> membresias = membresiaRepository.findMembresiasActivasPorFechaVencimiento(fechaVencimientoBuscada);
        
        // También incluir membresías vencidas para recordatorios post-vencimiento
        if (diasDiferencia > 0) {
            membresias = membresiaRepository.findMembresiasParaNotificacion(fechaVencimientoBuscada, fechaVencimientoBuscada);
        }

        log.info("   📋 Membresías encontradas: {}", membresias.size());

        for (Membresia membresia : membresias) {
            estadisticas.merge("procesadas", 1, Integer::sum);
            procesarMembresia(membresia, tipoRecordatorio, estadisticas);
        }
    }

    /**
     * Procesa una membresía individual y envía el recordatorio si corresponde.
     */
    private void procesarMembresia(Membresia membresia, TipoRecordatorio tipoRecordatorio, Map<String, Integer> estadisticas) {
        Estudiante estudiante = membresia.getEstudiante();
        
        // Validar que el estudiante esté activo
        if (estudiante == null || !Boolean.TRUE.equals(estudiante.getEstado())) {
            log.debug("   ⏭️ Estudiante inactivo o nulo - Membresía ID: {}", membresia.getIdMembresia());
            estadisticas.merge("omitidas", 1, Integer::sum);
            return;
        }

        // Obtener número de WhatsApp (priorizar whatsapp del estudiante, luego tutor)
        String numeroWhatsApp = obtenerNumeroWhatsApp(estudiante);
        if (numeroWhatsApp == null || numeroWhatsApp.isBlank()) {
            log.warn("   ⚠️ Sin número WhatsApp - Estudiante: {} (ID: {})", 
                    estudiante.getNombreCompleto(), estudiante.getIdEstudiante());
            estadisticas.merge("omitidas", 1, Integer::sum);
            return;
        }

        // Verificar si ya se envió este recordatorio (evitar duplicados)
        if (yaSeEnvioRecordatorio(membresia, tipoRecordatorio)) {
            log.debug("   ⏭️ Recordatorio ya enviado - Estudiante: {}, Tipo: {}", 
                    estudiante.getNombreCompleto(), tipoRecordatorio);
            estadisticas.merge("omitidas", 1, Integer::sum);
            return;
        }

        // Enviar el recordatorio
        enviarRecordatorio(membresia, estudiante, tipoRecordatorio, numeroWhatsApp, estadisticas);
    }

    /**
     * Obtiene el número de WhatsApp disponible para enviar recordatorios de pago.
     * Prioridad: Teléfono tutor (responsable del pago) > WhatsApp estudiante > Celular estudiante
     * 
     * Para recordatorios de pago, el tutor es el principal responsable,
     * por lo que se prioriza su número de contacto.
     */
    private String obtenerNumeroWhatsApp(Estudiante estudiante) {
        // Prioridad 1: Teléfono del tutor (responsable del pago de la membresía)
        if (estudiante.getTelefonoTutor() != null && !estudiante.getTelefonoTutor().isBlank()) {
            return estudiante.getTelefonoTutor();
        }
        // Prioridad 2: WhatsApp del estudiante
        if (estudiante.getWhatsappEstudiante() != null && !estudiante.getWhatsappEstudiante().isBlank()) {
            return estudiante.getWhatsappEstudiante();
        }
        // Prioridad 3: Celular del estudiante
        if (estudiante.getCelularEstudiante() != null && !estudiante.getCelularEstudiante().isBlank()) {
            return estudiante.getCelularEstudiante();
        }
        return null;
    }

    /**
     * Verifica si ya existe un recordatorio enviado para esta membresía y tipo.
     */
    private boolean yaSeEnvioRecordatorio(Membresia membresia, TipoRecordatorio tipoRecordatorio) {
        return recordatorioPagoRepository.existsByMembresiaAndTipoRecordatorioAndFechaVencimientoReferencia(
                membresia, 
                tipoRecordatorio, 
                membresia.getFechaFin()
        );
    }

    /**
     * Envía el recordatorio por WhatsApp y registra el resultado.
     */
    private void enviarRecordatorio(
            Membresia membresia, 
            Estudiante estudiante, 
            TipoRecordatorio tipoRecordatorio,
            String numeroWhatsApp,
            Map<String, Integer> estadisticas) {
        
        String fechaVencimientoStr = membresia.getFechaFin().format(DATE_FORMATTER);
        
        log.info("   📤 Enviando {} a {} ({})", 
                tipoRecordatorio.getDescripcion(),
                estudiante.getNombreCompleto(),
                numeroWhatsApp);

        // Enviar mensaje por WhatsApp
        WhatsAppMessageResult resultado = twilioWhatsAppService.enviarRecordatorioPago(
                numeroWhatsApp,
                estudiante.getNombreCompleto(),
                fechaVencimientoStr,
                tipoRecordatorio.getDiasDiferencia()
        );

        // Registrar el recordatorio
        RecordatorioPago recordatorio = new RecordatorioPago();
        recordatorio.setEstudiante(estudiante);
        recordatorio.setMembresia(membresia);
        recordatorio.setTipoRecordatorio(tipoRecordatorio);
        recordatorio.setFechaVencimientoReferencia(membresia.getFechaFin());
        recordatorio.setFechaEnvio(LocalDateTime.now());
        recordatorio.setIntentos(1);

        if (resultado.isExito()) {
            recordatorio.setEstadoEnvio(EstadoEnvio.ENVIADO);
            recordatorio.setTwilioMessageSid(resultado.getMessageSid());
            recordatorio.setMensaje("Enviado exitosamente");
            estadisticas.merge("enviadas", 1, Integer::sum);
            log.info("   ✅ Mensaje enviado - SID: {}", resultado.getMessageSid());
        } else {
            recordatorio.setEstadoEnvio(EstadoEnvio.FALLIDO);
            recordatorio.setErrorDetalle(resultado.getError());
            estadisticas.merge("fallidas", 1, Integer::sum);
            log.error("   ❌ Error al enviar: {}", resultado.getError());
        }

        recordatorioPagoRepository.save(recordatorio);
    }

    /**
     * Procesa reintentos de mensajes que fallaron previamente.
     */
    private void procesarReintentos(Map<String, Integer> estadisticas) {
        log.info("🔄 Procesando reintentos de mensajes fallidos...");
        
        List<RecordatorioPago> recordatoriosFallidos = 
                recordatorioPagoRepository.findRecordatoriosParaReintentar(maxReintentos);
        
        log.info("   📋 Recordatorios pendientes de reintento: {}", recordatoriosFallidos.size());

        for (RecordatorioPago recordatorio : recordatoriosFallidos) {
            reintentarEnvio(recordatorio, estadisticas);
        }
    }

    /**
     * Reintenta el envío de un recordatorio fallido.
     */
    private void reintentarEnvio(RecordatorioPago recordatorio, Map<String, Integer> estadisticas) {
        Estudiante estudiante = recordatorio.getEstudiante();
        String numeroWhatsApp = obtenerNumeroWhatsApp(estudiante);

        if (numeroWhatsApp == null) {
            log.warn("   ⚠️ Sin número para reintento - Estudiante: {}", estudiante.getNombreCompleto());
            return;
        }

        String fechaVencimientoStr = recordatorio.getFechaVencimientoReferencia().format(DATE_FORMATTER);

        log.info("   🔄 Reintentando envío (intento {}/{}) a {}", 
                recordatorio.getIntentos() + 1, 
                maxReintentos,
                estudiante.getNombreCompleto());

        WhatsAppMessageResult resultado = twilioWhatsAppService.enviarRecordatorioPago(
                numeroWhatsApp,
                estudiante.getNombreCompleto(),
                fechaVencimientoStr,
                recordatorio.getTipoRecordatorio().getDiasDiferencia()
        );

        recordatorio.setIntentos(recordatorio.getIntentos() + 1);
        recordatorio.setFechaEnvio(LocalDateTime.now());

        if (resultado.isExito()) {
            recordatorio.setEstadoEnvio(EstadoEnvio.ENVIADO);
            recordatorio.setTwilioMessageSid(resultado.getMessageSid());
            recordatorio.setErrorDetalle(null);
            estadisticas.merge("enviadas", 1, Integer::sum);
            log.info("   ✅ Reintento exitoso - SID: {}", resultado.getMessageSid());
        } else {
            recordatorio.setErrorDetalle(resultado.getError());
            estadisticas.merge("fallidas", 1, Integer::sum);
            log.error("   ❌ Reintento fallido: {}", resultado.getError());
        }

        recordatorioPagoRepository.save(recordatorio);
    }

    /**
     * Método para ejecutar el proceso manualmente (útil para testing).
     * Puede ser invocado desde un endpoint de administración.
     */
    @Transactional
    public Map<String, Object> ejecutarManualmente() {
        log.info("⚡ Ejecución manual de recordatorios solicitada");
        
        Map<String, Integer> estadisticas = new HashMap<>();
        estadisticas.put("procesadas", 0);
        estadisticas.put("enviadas", 0);
        estadisticas.put("omitidas", 0);
        estadisticas.put("fallidas", 0);

        LocalDate hoy = LocalDate.now();
        
        for (int diasDiferencia : DIAS_RECORDATORIO) {
            procesarRecordatoriosPorDia(hoy, diasDiferencia, estadisticas);
        }

        procesarReintentos(estadisticas);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("fechaEjecucion", LocalDateTime.now());
        resultado.put("estadisticas", estadisticas);
        resultado.put("mensaje", "Proceso ejecutado correctamente");

        return resultado;
    }

    /**
     * Obtiene estadísticas del sistema de recordatorios.
     */
    public Map<String, Object> obtenerEstadisticas() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Object[]> estadisticasDelDia = recordatorioPagoRepository.obtenerEstadisticasDelDia();
        Map<String, Long> enviosHoy = new HashMap<>();
        
        for (Object[] row : estadisticasDelDia) {
            EstadoEnvio estado = (EstadoEnvio) row[0];
            Long cantidad = (Long) row[1];
            enviosHoy.put(estado.name(), cantidad);
        }
        
        stats.put("enviosHoy", enviosHoy);
        stats.put("pendientesReintento", 
                recordatorioPagoRepository.findRecordatoriosParaReintentar(maxReintentos).size());
        stats.put("servicioTwilioActivo", twilioWhatsAppService.isServicioDisponible());
        stats.put("recordatoriosHabilitados", recordatoriosEnabled);
        stats.put("maxReintentos", maxReintentos);
        
        return stats;
    }
}
