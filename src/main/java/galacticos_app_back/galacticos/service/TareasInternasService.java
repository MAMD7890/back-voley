package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.WhatsAppMessageResult;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Membresia;
import galacticos_app_back.galacticos.entity.RecordatorioPago;
import galacticos_app_back.galacticos.entity.RecordatorioPago.EstadoEnvio;
import galacticos_app_back.galacticos.entity.RecordatorioPago.TipoRecordatorio;
import galacticos_app_back.galacticos.repository.EstudianteRepository;
import galacticos_app_back.galacticos.repository.MembresiaRepository;
import galacticos_app_back.galacticos.repository.RecordatorioPagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de tareas internas para actualización de estados y envío de recordatorios.
 *
 * Este servicio es invocado exclusivamente por endpoints internos protegidos con API Key,
 * que a su vez son llamados por AWS Lambda con EventBridge:
 *
 *   - POST /api/internal/estados/actualizar   → ejecutarActualizacionEstados()
 *     Cron Lambda: 0 0 * * ? (medianoche todos los días)
 *
 *   - POST /api/internal/recordatorios/enviar → ejecutarEnvioRecordatorios()
 *     Cron Lambda: 0 10 * * ? (10:00 AM todos los días)
 *
 * Días de recordatorio implementados (relativos a fechaFin de membresía):
 *   -5 días : Aviso preventivo temprano
 *   -2 días : Recordatorio urgente
 *    0 días : Último día, vence hoy
 *   +1 día  : Primer aviso de mora
 *   +2 días : Segundo aviso de mora
 *   +3 días : Aviso final
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TareasInternasService {

    private final MembresiaRepository membresiaRepository;
    private final EstudianteRepository estudianteRepository;
    private final RecordatorioPagoRepository recordatorioPagoRepository;
    private final TwilioWhatsAppService twilioWhatsAppService;

    @Value("${recordatorio.max-reintentos:3}")
    private int maxReintentos;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Días relativos a fechaFin en los que se envía recordatorio
    private static final int[] DIAS_RECORDATORIO = {-5, -2, 0, 1, 2, 3};

    // ─────────────────────────────────────────────────────────────────
    //  TAREA 1: ACTUALIZAR ESTADOS (medianoche)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Revisa todas las membresías vencidas y pone en EN_MORA a los estudiantes
     * que no pagaron. Se ejecuta a medianoche vía Lambda.
     *
     * Lógica: si fechaFin < hoy y el estudiante no es COMPROMISO_PAGO → EN_MORA
     */
    @Transactional
    public Map<String, Object> ejecutarActualizacionEstados() {
        LocalDate hoy = LocalDate.now();
        log.info("🌙 ====== ACTUALIZACIÓN DE ESTADOS - {} ======", hoy.format(FORMATTER));

        int actualizados = 0;
        int omitidos = 0;
        int errores = 0;

        List<Membresia> membresiasVencidas = membresiaRepository.findMembresiasVencidasSinMora(hoy);
        log.info("📋 Membresías vencidas encontradas: {}", membresiasVencidas.size());

        for (Membresia membresia : membresiasVencidas) {
            Estudiante estudiante = membresia.getEstudiante();
            try {
                if (estudiante == null || !Boolean.TRUE.equals(estudiante.getEstado())) {
                    omitidos++;
                    continue;
                }
                // Solo actualizar si no está ya en mora o en compromiso
                Estudiante.EstadoPago estadoActual = estudiante.getEstadoPago();
                if (estadoActual == Estudiante.EstadoPago.EN_MORA ||
                    estadoActual == Estudiante.EstadoPago.COMPROMISO_PAGO) {
                    omitidos++;
                    continue;
                }

                estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                estudianteRepository.save(estudiante);
                actualizados++;

                log.info("   🔴 {} → EN_MORA (vencía: {})",
                        estudiante.getNombreCompleto(),
                        membresia.getFechaFin().format(FORMATTER));

            } catch (Exception e) {
                errores++;
                log.error("   ❌ Error actualizando estudiante ID {}: {}",
                        estudiante != null ? estudiante.getIdEstudiante() : "null", e.getMessage());
            }
        }

        log.info("📊 Resultado: {} actualizados a EN_MORA, {} omitidos, {} errores", actualizados, omitidos, errores);

        return Map.of(
            "tarea", "actualizacion_estados",
            "fecha", hoy.toString(),
            "actualizados", actualizados,
            "omitidos", omitidos,
            "errores", errores,
            "timestamp", LocalDateTime.now().toString()
        );
    }

    // ─────────────────────────────────────────────────────────────────
    //  TAREA 2: ENVIAR RECORDATORIOS WHATSAPP (10:00 AM)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Procesa las membresías del día y envía los mensajes de WhatsApp correspondientes.
     * Se ejecuta a las 10:00 AM vía Lambda.
     */
    @Transactional
    public Map<String, Object> ejecutarEnvioRecordatorios() {
        LocalDate hoy = LocalDate.now();
        log.info("📱 ====== ENVÍO DE RECORDATORIOS - {} ======", hoy.format(FORMATTER));

        Map<String, Integer> stats = new HashMap<>();
        stats.put("procesadas", 0);
        stats.put("enviadas", 0);
        stats.put("omitidas", 0);
        stats.put("fallidas", 0);

        for (int dias : DIAS_RECORDATORIO) {
            procesarDia(hoy, dias, stats);
        }

        procesarReintentos(stats);

        log.info("📊 Resultado: {} procesadas | {} enviadas | {} omitidas | {} fallidas",
                stats.get("procesadas"), stats.get("enviadas"),
                stats.get("omitidas"), stats.get("fallidas"));

        return Map.of(
            "tarea", "envio_recordatorios",
            "fecha", hoy.toString(),
            "estadisticas", stats,
            "timestamp", LocalDateTime.now().toString()
        );
    }

    // ─────────────────────────────────────────────────────────────────
    //  LÓGICA INTERNA DE RECORDATORIOS
    // ─────────────────────────────────────────────────────────────────

    private void procesarDia(LocalDate hoy, int dias, Map<String, Integer> stats) {
        // La membresía que vence en (hoy - dias) es la que corresponde a este recordatorio
        // Ej: dias=-5 → buscamos membresías cuyo fechaFin = hoy + 5
        //     dias=+1 → buscamos membresías cuyo fechaFin = hoy - 1
        LocalDate fechaVencimientoBuscada = hoy.minusDays(dias);

        TipoRecordatorio tipo = TipoRecordatorio.fromDiasDiferencia(dias);
        if (tipo == null) {
            log.warn("⚠️ No existe TipoRecordatorio para {} días", dias);
            return;
        }

        log.info("🔍 Procesando: {} (fechaFin buscada: {})", tipo.getDescripcion(), fechaVencimientoBuscada.format(FORMATTER));

        // Para pre-vencimiento buscamos membresías activas; para post-vencimiento cualquiera
        List<Membresia> membresias;
        if (dias <= 0) {
            membresias = membresiaRepository.findMembresiasActivasPorFechaVencimiento(fechaVencimientoBuscada);
        } else {
            membresias = membresiaRepository.findMembresiasParaNotificacion(fechaVencimientoBuscada, fechaVencimientoBuscada);
        }

        log.info("   📋 Membresías encontradas: {}", membresias.size());

        for (Membresia membresia : membresias) {
            stats.merge("procesadas", 1, Integer::sum);
            procesarMembresia(membresia, tipo, stats);
        }
    }

    private void procesarMembresia(Membresia membresia, TipoRecordatorio tipo, Map<String, Integer> stats) {
        Estudiante estudiante = membresia.getEstudiante();

        if (estudiante == null || !Boolean.TRUE.equals(estudiante.getEstado())) {
            stats.merge("omitidas", 1, Integer::sum);
            return;
        }

        String numero = obtenerNumeroWhatsApp(estudiante);
        if (numero == null || numero.isBlank()) {
            log.warn("   ⚠️ Sin número WhatsApp - {}", estudiante.getNombreCompleto());
            stats.merge("omitidas", 1, Integer::sum);
            return;
        }

        if (yaSeEnvio(membresia, tipo)) {
            log.debug("   ⏭️ Ya enviado - {} | {}", estudiante.getNombreCompleto(), tipo);
            stats.merge("omitidas", 1, Integer::sum);
            return;
        }

        enviarYRegistrar(membresia, estudiante, tipo, numero, stats);
    }

    private void enviarYRegistrar(Membresia membresia, Estudiante estudiante,
                                   TipoRecordatorio tipo, String numero,
                                   Map<String, Integer> stats) {

        String fechaStr = membresia.getFechaFin().format(FORMATTER);

        log.info("   📤 Enviando [{}] a {} ({})", tipo.getDescripcion(), estudiante.getNombreCompleto(), numero);

        WhatsAppMessageResult resultado = twilioWhatsAppService.enviarRecordatorioPago(
                numero, estudiante.getNombreCompleto(), fechaStr, tipo.getDiasDiferencia());

        RecordatorioPago rec = new RecordatorioPago();
        rec.setEstudiante(estudiante);
        rec.setMembresia(membresia);
        rec.setTipoRecordatorio(tipo);
        rec.setFechaVencimientoReferencia(membresia.getFechaFin());
        rec.setFechaEnvio(LocalDateTime.now());
        rec.setIntentos(1);

        if (resultado.isExito()) {
            rec.setEstadoEnvio(EstadoEnvio.ENVIADO);
            rec.setTwilioMessageSid(resultado.getMessageSid());
            rec.setMensaje("Enviado exitosamente");
            stats.merge("enviadas", 1, Integer::sum);
            log.info("   ✅ SID: {}", resultado.getMessageSid());
        } else {
            rec.setEstadoEnvio(EstadoEnvio.FALLIDO);
            rec.setErrorDetalle(resultado.getError());
            stats.merge("fallidas", 1, Integer::sum);
            log.error("   ❌ Error: {}", resultado.getError());
        }

        recordatorioPagoRepository.save(rec);
    }

    private void procesarReintentos(Map<String, Integer> stats) {
        List<RecordatorioPago> fallidos = recordatorioPagoRepository
                .findByEstadoEnvioAndIntentosLessThan(EstadoEnvio.FALLIDO, maxReintentos);

        if (fallidos.isEmpty()) return;
        log.info("🔄 Procesando {} reintentos...", fallidos.size());

        for (RecordatorioPago rec : fallidos) {
            Estudiante est = rec.getEstudiante();
            String numero = obtenerNumeroWhatsApp(est);
            if (numero == null) continue;

            String fechaStr = rec.getFechaVencimientoReferencia().format(FORMATTER);
            log.info("   🔄 Reintento ({}/{}) → {}", rec.getIntentos() + 1, maxReintentos, est.getNombreCompleto());

            WhatsAppMessageResult resultado = twilioWhatsAppService.enviarRecordatorioPago(
                    numero, est.getNombreCompleto(), fechaStr, rec.getTipoRecordatorio().getDiasDiferencia());

            rec.setIntentos(rec.getIntentos() + 1);
            rec.setFechaEnvio(LocalDateTime.now());

            if (resultado.isExito()) {
                rec.setEstadoEnvio(EstadoEnvio.ENVIADO);
                rec.setTwilioMessageSid(resultado.getMessageSid());
                rec.setErrorDetalle(null);
                stats.merge("enviadas", 1, Integer::sum);
                log.info("   ✅ Reintento exitoso. SID: {}", resultado.getMessageSid());
            } else {
                rec.setErrorDetalle(resultado.getError());
                stats.merge("fallidas", 1, Integer::sum);
            }

            recordatorioPagoRepository.save(rec);
        }
    }

    private boolean yaSeEnvio(Membresia membresia, TipoRecordatorio tipo) {
        return recordatorioPagoRepository.existsByMembresiaAndTipoRecordatorioAndFechaVencimientoReferencia(
                membresia, tipo, membresia.getFechaFin());
    }

    private String obtenerNumeroWhatsApp(Estudiante estudiante) {
        if (estudiante.getWhatsappEstudiante() != null && !estudiante.getWhatsappEstudiante().isBlank())
            return estudiante.getWhatsappEstudiante();
        if (estudiante.getCelularEstudiante() != null && !estudiante.getCelularEstudiante().isBlank())
            return estudiante.getCelularEstudiante();
        if (estudiante.getTelefonoTutor() != null && !estudiante.getTelefonoTutor().isBlank())
            return estudiante.getTelefonoTutor();
        return null;
    }

    // ─────────────────────────────────────────────────────────────────
    //  ESTADÍSTICAS (para endpoint de salud)
    // ─────────────────────────────────────────────────────────────────

    public Map<String, Object> obtenerEstadisticas() {
        return Map.of(
            "servicioTwilioActivo", twilioWhatsAppService.isServicioDisponible(),
            "maxReintentos", maxReintentos,
            "pendientesReintento", recordatorioPagoRepository
                    .findByEstadoEnvioAndIntentosLessThan(EstadoEnvio.FALLIDO, maxReintentos).size(),
            "totalRecordatorios", recordatorioPagoRepository.count()
        );
    }
}
