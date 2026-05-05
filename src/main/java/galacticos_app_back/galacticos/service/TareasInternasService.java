package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.WhatsAppMessageResult;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Membresia;
import galacticos_app_back.galacticos.entity.MembresiaHistorial;
import galacticos_app_back.galacticos.entity.RecordatorioPago;
import galacticos_app_back.galacticos.entity.RecordatorioPago.EstadoEnvio;
import galacticos_app_back.galacticos.entity.RecordatorioPago.TipoRecordatorio;
import galacticos_app_back.galacticos.repository.EstudianteRepository;
import galacticos_app_back.galacticos.repository.MembresiaHistorialRepository;
import galacticos_app_back.galacticos.repository.MembresiaRepository;
import galacticos_app_back.galacticos.repository.PagoRepository;
import galacticos_app_back.galacticos.repository.RecordatorioPagoRepository;

import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final PagoRepository pagoRepository;
    private final MetaWhatsAppService metaWhatsAppService;
    private final MembresiaHistorialRepository membresiaHistorialRepository;

    private static final ZoneId ZONA_CO = ZoneId.of("America/Bogota");

    @Value("${recordatorio.max-reintentos:3}")
    private int maxReintentos;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Días relativos a fechaFin en los que se envía recordatorio
    private static final int[] DIAS_RECORDATORIO = {-5, -2, 0, 1, 2, 3};

    // ─────────────────────────────────────────────────────────────────
    //  TAREA 1: ACTUALIZAR ESTADOS (medianoche)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Revisa y corrige estados de pago. Se ejecuta a medianoche vía Lambda.
     *
     * Fase 0 (corrección): detecta y corrige estados inconsistentes automáticos.
     * Fase 1: membresías vencidas → EN_MORA (respeta cambios manuales).
     * Fase 2: DECLINADO con >5 días desde vencimiento → EN_MORA.
     * Fase 3: COMPROMISO_PAGO con fechaLimiteCompromiso vencida → EN_MORA.
     */
    @Transactional
    public Map<String, Object> ejecutarActualizacionEstados() {
        LocalDate hoy = LocalDate.now();
        log.info("🌙 ====== ACTUALIZACIÓN DE ESTADOS - {} ======", hoy.format(FORMATTER));

        int actualizados = 0;
        int omitidos = 0;
        int errores = 0;
        int corregidos = 0;

        // ── FASE 0: CORRECCIÓN DE INCONSISTENCIAS (solo estados NO cambiados manualmente) ──
        log.info("🔧 Fase 0 - Corrección de inconsistencias...");
        List<Estudiante> todosActivos = estudianteRepository.findByEstado(true);
        for (Estudiante estudiante : todosActivos) {
            // COMPROMISO_PAGO se maneja exclusivamente en Fase 3
            if (estudiante.getEstadoPago() == Estudiante.EstadoPago.COMPROMISO_PAGO) continue;

            try {
                List<Membresia> mems = membresiaRepository.findByEstudianteIdEstudiante(estudiante.getIdEstudiante());
                Membresia reciente = mems.stream()
                        .filter(m -> m.getFechaFin() != null && Boolean.TRUE.equals(m.getEstado()))
                        .max(java.util.Comparator.comparing(Membresia::getFechaFin))
                        .orElse(null);

                boolean tieneAlgunaReal = mems.stream().anyMatch(m -> Boolean.TRUE.equals(m.getEstado()));
                boolean cambioManual = Boolean.TRUE.equals(estudiante.getCambiadoManualmente());

                // cambiadoManualmente solo se respeta mientras la membresía esté vigente.
                // Si ya venció (o no tiene membresía), se ignora el flag y se aplican reglas normales.
                boolean membresiaVigente = reciente != null && !reciente.getFechaFin().isBefore(hoy);
                if (cambioManual && membresiaVigente) continue;

                if (reciente != null
                        && !reciente.getFechaFin().isBefore(hoy)
                        && membresiaRespaldadaPorPago(reciente)) {
                    // Membresía activa con pago real → AL_DIA
                    if (estudiante.getEstadoPago() != Estudiante.EstadoPago.AL_DIA) {
                        log.info("   🔧 {} corregido: {} → AL_DIA (membresía vigente hasta {}, pago verificado)",
                                estudiante.getNombreCompleto(), estudiante.getEstadoPago(), reciente.getFechaFin());
                        estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
                        estudianteRepository.save(estudiante);
                        corregidos++;
                    }
                } else if (estudiante.getEstadoPago() == Estudiante.EstadoPago.AL_DIA) {
                    // Marcado AL_DIA pero sin membresía activa vigente
                    boolean tieneMembresiaSinActivar = mems.stream()
                            .anyMatch(m -> m.getFechaFin() != null && !m.getFechaFin().isBefore(hoy));
                    if (!tieneMembresiaSinActivar) {
                        log.info("   🔧 {} corregido: AL_DIA → PENDIENTE (sin membresía vigente)",
                                estudiante.getNombreCompleto());
                        estudiante.setEstadoPago(Estudiante.EstadoPago.PENDIENTE);
                        estudianteRepository.save(estudiante);
                        corregidos++;
                    }
                } else if (estudiante.getEstadoPago() == Estudiante.EstadoPago.EN_MORA && !tieneAlgunaReal) {
                    // EN_MORA pero nunca tuvo membresía real → fue movido incorrectamente → PENDIENTE
                    log.info("   🔧 {} corregido: EN_MORA → PENDIENTE (sin membresía real registrada)",
                            estudiante.getNombreCompleto());
                    estudiante.setEstadoPago(Estudiante.EstadoPago.PENDIENTE);
                    estudianteRepository.save(estudiante);
                    corregidos++;
                } else if (estudiante.getEstadoPago() == Estudiante.EstadoPago.PENDIENTE && !tieneAlgunaReal) {
                    // PENDIENTE sin membresía real: si ya venció la gracia de 5 días → EN_MORA
                    LocalDate fechaReg = estudiante.getFechaRegistro();
                    if (fechaReg != null && fechaReg.plusDays(5).isBefore(hoy)) {
                        log.info("   🔴 {} → EN_MORA (PENDIENTE sin membresía, registrado hace >5 días: {})",
                                estudiante.getNombreCompleto(), fechaReg);
                        estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                        estudianteRepository.save(estudiante);
                        actualizados++;
                    } else {
                        log.debug("   ⏳ {} en gracia PENDIENTE (registrado: {}, faltan {} días)",
                                estudiante.getNombreCompleto(), fechaReg,
                                fechaReg != null ? java.time.temporal.ChronoUnit.DAYS.between(hoy, fechaReg.plusDays(5)) : "?");
                    }
                }
            } catch (Exception e) {
                log.error("   ❌ Error en fase de corrección para estudiante {}: {}",
                        estudiante.getIdEstudiante(), e.getMessage());
            }
        }
        log.info("📋 Fase 0 completada: {} inconsistencias corregidas", corregidos);

        // ── FASE 1: membresías vencidas (excluye manuales, DECLINADO, EN_MORA, COMPROMISO_PAGO) ──
        List<Membresia> membresiasVencidas = membresiaRepository.findMembresiasVencidasSinMora(hoy);
        log.info("📋 Fase 1 - Membresías vencidas: {}", membresiasVencidas.size());

        for (Membresia membresia : membresiasVencidas) {
            Estudiante estudiante = membresia.getEstudiante();
            try {
                if (estudiante == null || !Boolean.TRUE.equals(estudiante.getEstado())) {
                    omitidos++;
                    continue;
                }
                // La membresía en Fase 1 ya venció (fechaFin < hoy), por lo tanto
                // cambiadoManualmente se ignora: el vencimiento tiene prioridad sobre el cambio manual.
                estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                estudiante.setCambiadoManualmente(false);
                estudianteRepository.save(estudiante);
                actualizados++;
                log.info("   🔴 {} → EN_MORA (vencía: {})",
                        estudiante.getNombreCompleto(), membresia.getFechaFin().format(FORMATTER));
            } catch (Exception e) {
                errores++;
                log.error("   ❌ Error actualizando estudiante ID {}: {}",
                        estudiante != null ? estudiante.getIdEstudiante() : "null", e.getMessage());
            }
        }

        // ── FASE 2: DECLINADO con ventana de gracia de 5 días vencida ──
        LocalDate limiteDeclinado = hoy.minusDays(5);
        List<Membresia> membresiasDeclinadas = membresiaRepository.findMembresiasDeclinadasVencidas(limiteDeclinado);
        log.info("📋 Fase 2 - DECLINADO con gracia vencida (>5 días): {}", membresiasDeclinadas.size());

        for (Membresia membresia : membresiasDeclinadas) {
            Estudiante estudiante = membresia.getEstudiante();
            try {
                if (estudiante == null || !Boolean.TRUE.equals(estudiante.getEstado())) {
                    omitidos++;
                    continue;
                }
                estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                estudianteRepository.save(estudiante);
                actualizados++;
                log.info("   🔴 {} → EN_MORA desde DECLINADO (vencía: {}, días: {})",
                        estudiante.getNombreCompleto(),
                        membresia.getFechaFin().format(FORMATTER),
                        java.time.temporal.ChronoUnit.DAYS.between(membresia.getFechaFin(), hoy));
            } catch (Exception e) {
                errores++;
                log.error("   ❌ Error actualizando DECLINADO ID {}: {}",
                        estudiante != null ? estudiante.getIdEstudiante() : "null", e.getMessage());
            }
        }

        // ── FASE 3: COMPROMISO_PAGO con fecha límite vencida → EN_MORA ──
        log.info("📋 Fase 3 - COMPROMISO_PAGO con fecha límite vencida...");
        List<Estudiante> compromisos = estudianteRepository.findByEstado(true).stream()
                .filter(e -> e.getEstadoPago() == Estudiante.EstadoPago.COMPROMISO_PAGO
                        && e.getFechaLimiteCompromiso() != null
                        && e.getFechaLimiteCompromiso().isBefore(hoy))
                .collect(java.util.stream.Collectors.toList());

        for (Estudiante estudiante : compromisos) {
            try {
                log.info("   🔴 {} → EN_MORA (compromiso vencido el {})",
                        estudiante.getNombreCompleto(), estudiante.getFechaLimiteCompromiso().format(FORMATTER));
                estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                estudiante.setCambiadoManualmente(false);
                estudiante.setFechaLimiteCompromiso(null);
                estudianteRepository.save(estudiante);
                actualizados++;
            } catch (Exception e) {
                errores++;
                log.error("   ❌ Error procesando COMPROMISO_PAGO ID {}: {}",
                        estudiante.getIdEstudiante(), e.getMessage());
            }
        }

        log.info("📊 Resultado: {} corregidos, {} actualizados a EN_MORA, {} omitidos, {} errores",
                corregidos, actualizados, omitidos, errores);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("tarea", "actualizacion_estados");
        resultado.put("fecha", hoy.toString());
        resultado.put("corregidos", corregidos);
        resultado.put("actualizados", actualizados);
        resultado.put("omitidos", omitidos);
        resultado.put("errores", errores);
        resultado.put("timestamp", LocalDateTime.now().toString());
        return resultado;
    }

    // ─────────────────────────────────────────────────────────────────
    //  CORRECCIÓN PUNTUAL: EN_MORA SIN MEMBRESÍA → PENDIENTE (una sola vez)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Busca todos los estudiantes activos que están EN_MORA pero NUNCA tuvieron
     * una membresía real (estado=true). Los mueve a PENDIENTE y les pone fechaRegistro=hoy
     * para que el ciclo normal de 5 días arranque desde cero.
     *
     * Se invoca una sola vez vía Lambda para sanear registros incorrectos previos.
     */
    @Transactional
    public Map<String, Object> corregirEnMoraSinMembresia() {
        LocalDate hoy = LocalDate.now();
        log.info("🔧 ====== CORRECCIÓN EN_MORA SIN MEMBRESÍA - {} ======", hoy.format(FORMATTER));

        int corregidos = 0;
        int omitidos = 0;
        int errores = 0;

        List<Estudiante> enMora = estudianteRepository.findByEstado(true).stream()
                .filter(e -> e.getEstadoPago() == Estudiante.EstadoPago.EN_MORA)
                .collect(java.util.stream.Collectors.toList());

        log.info("📋 Estudiantes EN_MORA activos encontrados: {}", enMora.size());

        for (Estudiante estudiante : enMora) {
            try {
                List<Membresia> mems = membresiaRepository.findByEstudianteIdEstudiante(estudiante.getIdEstudiante());
                boolean tieneAlgunaReal = mems.stream().anyMatch(m -> Boolean.TRUE.equals(m.getEstado()));

                if (tieneAlgunaReal) {
                    omitidos++;
                    continue; // tiene membresía real (activa o expirada), no tocar
                }

                // Sin membresía real → corregir a PENDIENTE y reiniciar contador de gracia
                log.info("   🔧 {} → PENDIENTE (sin membresía real, fechaRegistro reiniciada a {})",
                        estudiante.getNombreCompleto(), hoy);
                estudiante.setEstadoPago(Estudiante.EstadoPago.PENDIENTE);
                estudiante.setFechaRegistro(hoy);
                estudianteRepository.save(estudiante);
                corregidos++;
            } catch (Exception e) {
                errores++;
                log.error("   ❌ Error corrigiendo estudiante ID {}: {}",
                        estudiante.getIdEstudiante(), e.getMessage());
            }
        }

        log.info("📊 Corrección finalizada: {} corregidos, {} omitidos (tienen membresía real), {} errores",
                corregidos, omitidos, errores);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("tarea", "correccion_en_mora_sin_membresia");
        resultado.put("fecha", hoy.toString());
        resultado.put("corregidos", corregidos);
        resultado.put("omitidos", omitidos);
        resultado.put("errores", errores);
        resultado.put("descripcion", "Corregidos a PENDIENTE con 5 dias de gracia desde hoy");
        resultado.put("timestamp", LocalDateTime.now().toString());
        return resultado;
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

        WhatsAppMessageResult resultado = metaWhatsAppService.enviarRecordatorioPago(
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

            WhatsAppMessageResult resultado = metaWhatsAppService.enviarRecordatorioPago(
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
    //  VALIDACIÓN DE PAGO RESPALDANDO UNA MEMBRESÍA
    // ─────────────────────────────────────────────────────────────────

    /**
     * Verifica que exista un pago APROBADO que respalde la membresía.
     *
     * Criterios:
     * - estadoPago = PAGADO
     * - fechaPago entre (fechaInicio - 30 días) y (fechaFin + 5 días)
     *   La tolerancia cubre pagos anticipados o con pequeño retraso de registro.
     * - valor >= al mínimo esperado según duración (con 10% de tolerancia)
     *
     * Precios de referencia: 1 mes=80.000, 2 meses=150.000, 3 meses=210.000
     */
    private boolean membresiaRespaldadaPorPago(Membresia membresia) {
        if (membresia.getFechaInicio() == null || membresia.getFechaFin() == null) return false;

        java.math.BigDecimal valorMinimo = calcularValorMinimoMembresia(membresia);
        LocalDate desde = membresia.getFechaInicio().minusDays(30);
        LocalDate hasta = membresia.getFechaFin().plusDays(5);

        List<galacticos_app_back.galacticos.entity.Pago> pagos = pagoRepository.findPagosAprobadosEnRango(
                membresia.getEstudiante().getIdEstudiante(),
                desde, hasta, valorMinimo);

        if (pagos.isEmpty()) {
            log.warn("   ⚠️ Membresía ID {} (estudiante: {}) con estado=true pero SIN pago aprobado " +
                     "en rango [{} → {}] con valor >= {}",
                    membresia.getIdMembresia(),
                    membresia.getEstudiante().getNombreCompleto(),
                    desde, hasta, valorMinimo);
        }
        return !pagos.isEmpty();
    }

    /**
     * Calcula el valor mínimo aceptable para validar el pago de una membresía.
     *
     * Precios fijos por duración:
     *   ~1 mes  (≤35 días)  →  80.000
     *   ~2 meses (≤65 días) → 150.000
     *   ~3 meses (≤95 días) → 210.000
     *   >3 meses            → 210.000 + 80.000 por cada mes adicional
     *
     * Se aplica 10% de tolerancia hacia abajo para cubrir pequeñas diferencias
     * de registro o redondeos.
     */
    private java.math.BigDecimal calcularValorMinimoMembresia(Membresia membresia) {
        long dias = java.time.temporal.ChronoUnit.DAYS.between(
                membresia.getFechaInicio(), membresia.getFechaFin());

        int tramo = dias <= 35 ? 1 : dias <= 65 ? 2 : dias <= 95 ? 3 : 4;
        java.math.BigDecimal precioExacto = switch (tramo) {
            case 1 -> new java.math.BigDecimal("80000");
            case 2 -> new java.math.BigDecimal("150000");
            case 3 -> new java.math.BigDecimal("210000");
            default -> {
                // Más de 3 meses: 210k base + 80k por cada mes adicional
                long mesesExtra = Math.round((dias - 90) / 30.0);
                yield new java.math.BigDecimal("210000")
                        .add(new java.math.BigDecimal("80000").multiply(java.math.BigDecimal.valueOf(mesesExtra)));
            }
        };

        // 10% de tolerancia hacia abajo
        return precioExacto.multiply(new java.math.BigDecimal("0.90"))
                           .setScale(0, java.math.RoundingMode.FLOOR);
    }

    // ─────────────────────────────────────────────────────────────────
    //  RECONCILIACIÓN PAGOS ↔ MEMBRESÍAS (corrección histórica)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Revisa cada membresía activa (estado=true) y compara su duración con la suma
     * de meses que representan los pagos APROBADOS encontrados en su rango.
     *
     * Regla: si los pagos cubren MÁS meses de los que muestra la membresía, se extiende
     * la fechaFin y el estudiante queda AL_DIA. Nunca se reduce una membresía.
     *
     * Caso típico: Wompi registró $150.000 (2 meses) pero la membresía solo tenía 1 mes.
     *
     * Se invoca vía Lambda una sola vez (o cuando se detecte data sucia).
     * POST /api/internal/membresias/reconciliar
     */
    @Transactional
    public Map<String, Object> reconciliarPagosConMembresias() {
        LocalDate hoy = LocalDate.now();
        log.info("🔄 ====== RECONCILIACIÓN PAGOS-MEMBRESÍAS - {} ======", hoy.format(FORMATTER));

        int extendidas = 0;
        int sinCambio  = 0;
        int sinPago    = 0;
        int errores    = 0;

        List<Membresia> membresiasActivas = membresiaRepository.findByEstado(true).stream()
                .filter(m -> Boolean.TRUE.equals(m.getEstudiante().getEstado()))
                .filter(m -> m.getFechaInicio() != null && m.getFechaFin() != null)
                .collect(java.util.stream.Collectors.toList());

        log.info("📋 Membresías activas a revisar: {}", membresiasActivas.size());

        for (Membresia membresia : membresiasActivas) {
            Estudiante estudiante = membresia.getEstudiante();
            try {
                LocalDate desde = membresia.getFechaInicio().minusDays(30);
                LocalDate hasta = membresia.getFechaFin().plusDays(5);

                List<galacticos_app_back.galacticos.entity.Pago> pagos = pagoRepository
                        .findPagosAppEnRango(estudiante.getIdEstudiante(), desde, hasta);

                if (pagos.isEmpty()) {
                    log.warn("   ⚠️ {} – membresía {} sin pagos en rango [{} → {}]",
                            estudiante.getNombreCompleto(), membresia.getIdMembresia(), desde, hasta);
                    sinPago++;
                    continue;
                }

                int totalMeses = pagos.stream()
                        .mapToInt(p -> calcularMesesPorMontoInterno(p.getValor()))
                        .sum();

                int mesesPeriodoActual = calcularMesesPeriodoInterno(
                        membresia.getFechaInicio(), membresia.getFechaFin());

                if (totalMeses > mesesPeriodoActual) {
                    LocalDate nuevaFechaFin = membresia.getFechaInicio().plusMonths(totalMeses);
                    LocalDate fechaFinAnteriorRec = membresia.getFechaFin();
                    Boolean estadoAnteriorRec = membresia.getEstado();
                    log.info("   🔄 {} | {} → {} (pagos={}m, período={}m)",
                            estudiante.getNombreCompleto(),
                            membresia.getFechaFin(), nuevaFechaFin,
                            totalMeses, mesesPeriodoActual);
                    membresia.setFechaFin(nuevaFechaFin);
                    membresia.setFechaUltimoCambio(LocalDateTime.now(ZONA_CO));
                    membresia.setMotivoCambio("RECONCILIACION_PAGOS");
                    membresiaRepository.save(membresia);
                    guardarHistorial(membresia, estudiante,
                            membresia.getFechaInicio(), fechaFinAnteriorRec, estadoAnteriorRec,
                            membresia.getFechaInicio(), nuevaFechaFin, membresia.getEstado(),
                            "RECONCILIACION_PAGOS", "LAMBDA");

                    // Si la extensión supera hoy → poner AL_DIA
                    if (!nuevaFechaFin.isBefore(hoy)
                            && estudiante.getEstadoPago() != Estudiante.EstadoPago.AL_DIA) {
                        log.info("   ✅ {} → AL_DIA (membresía extendida hasta {})",
                                estudiante.getNombreCompleto(), nuevaFechaFin);
                        estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
                        estudiante.setCambiadoManualmente(false);
                        estudianteRepository.save(estudiante);
                    }
                    extendidas++;
                } else {
                    sinCambio++;
                }
            } catch (Exception e) {
                errores++;
                log.error("   ❌ Error reconciliando membresía {} – estudiante {}: {}",
                        membresia.getIdMembresia(), estudiante.getIdEstudiante(), e.getMessage());
            }
        }

        log.info("📊 Reconciliación: {} extendidas, {} sin cambio, {} sin pago, {} errores",
                extendidas, sinCambio, sinPago, errores);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("tarea", "reconciliacion_pagos_membresias");
        resultado.put("fecha", hoy.toString());
        resultado.put("extendidas", extendidas);
        resultado.put("sinCambio", sinCambio);
        resultado.put("sinPago", sinPago);
        resultado.put("errores", errores);
        resultado.put("timestamp", LocalDateTime.now().toString());
        return resultado;
    }

    /**
     * Convierte un monto de pago en la cantidad de meses que representa.
     * Precios fijos con 10 % de tolerancia: $189k → 3m | $135k → 2m | $72k → 1m
     */
    private int calcularMesesPorMontoInterno(java.math.BigDecimal monto) {
        if (monto == null) return 0;
        if (monto.compareTo(new java.math.BigDecimal("189000")) >= 0) return 3;
        if (monto.compareTo(new java.math.BigDecimal("135000")) >= 0) return 2;
        if (monto.compareTo(new java.math.BigDecimal("72000"))  >= 0) return 1;
        return 0;
    }

    /** Calcula cuántos meses-membresía cubre el rango fechaInicio → fechaFin. */
    private int calcularMesesPeriodoInterno(LocalDate fechaInicio, LocalDate fechaFin) {
        long dias = java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, fechaFin);
        if (dias <= 35) return 1;
        if (dias <= 65) return 2;
        if (dias <= 95) return 3;
        return (int) Math.round(dias / 30.0);
    }

    // ─────────────────────────────────────────────────────────────────
    //  CORRECCIÓN PUNTUAL: eliminar pagos externos y recalcular membresías
    // ─────────────────────────────────────────────────────────────────

    /**
     * Busca pagos PAGADOS cuya referencia no empieza con "PAY-" (pagos de links externos
     * de Wompi como uniformes, matrículas, etc. que se registraron incorrectamente como
     * membresías). Para cada uno:
     *   1. Recalcula la fechaFin de la membresía del estudiante usando solo pagos PAY-.
     *   2. Corrige el estado del estudiante si la membresía recalculada ya venció.
     *   3. Elimina el pago externo de la BD.
     *
     * POST /api/internal/membresias/corregir-pagos-externos
     */
    @Transactional
    public Map<String, Object> corregirPagosExternos() {
        LocalDate hoy = LocalDate.now();
        log.info("🔧 ====== CORRECCIÓN PAGOS EXTERNOS - {} ======", hoy);

        int pagosEliminados = 0;
        int membresiasCorregidas = 0;
        int sinCambio = 0;
        int errores = 0;
        List<Map<String, Object>> detalles = new ArrayList<>();

        List<galacticos_app_back.galacticos.entity.Pago> pagosExternos = pagoRepository.findPagosExternos();
        log.info("📋 Pagos externos encontrados: {}", pagosExternos.size());

        for (galacticos_app_back.galacticos.entity.Pago pagoExterno : pagosExternos) {
            Map<String, Object> detalle = new HashMap<>();
            try {
                detalle.put("idPago", pagoExterno.getIdPago());
                detalle.put("referencia", pagoExterno.getReferenciaPago());
                detalle.put("monto", pagoExterno.getValor());

                Estudiante estudiante = pagoExterno.getEstudiante();
                if (estudiante == null) {
                    pagoRepository.delete(pagoExterno);
                    pagosEliminados++;
                    detalle.put("resultado", "ELIMINADO_SIN_ESTUDIANTE");
                    detalles.add(detalle);
                    continue;
                }

                detalle.put("idEstudiante", estudiante.getIdEstudiante());
                detalle.put("nombre", estudiante.getNombreCompleto());

                List<Membresia> membresias = membresiaRepository.findByEstudianteIdEstudiante(estudiante.getIdEstudiante());

                if (membresias != null && !membresias.isEmpty()) {
                    Membresia membresia = membresias.stream()
                            .filter(m -> m.getFechaFin() != null)
                            .max(Comparator.comparing(Membresia::getFechaFin))
                            .orElse(membresias.get(0));

                    LocalDate fechaInicio = membresia.getFechaInicio();
                    LocalDate fechaFinAnterior = membresia.getFechaFin();
                    detalle.put("fechaFinAnterior", fechaFinAnterior != null ? fechaFinAnterior.toString() : null);

                    if (fechaInicio != null) {
                        LocalDate desde = fechaInicio.minusDays(30);
                        LocalDate hasta = fechaFinAnterior != null ? fechaFinAnterior.plusDays(5) : hoy;

                        List<galacticos_app_back.galacticos.entity.Pago> pagosApp =
                                pagoRepository.findPagosAppEnRango(estudiante.getIdEstudiante(), desde, hasta);

                        int totalMesesApp = pagosApp.stream()
                                .mapToInt(p -> calcularMesesPorMontoInterno(p.getValor()))
                                .sum();

                        if (totalMesesApp > 0) {
                            LocalDate nuevaFechaFin = fechaInicio.plusMonths(totalMesesApp);
                            if (!nuevaFechaFin.equals(fechaFinAnterior)) {
                                Boolean estadoAnteriorCE = membresia.getEstado();
                                Boolean estadoNuevoCE = !nuevaFechaFin.isBefore(hoy);
                                membresia.setFechaFin(nuevaFechaFin);
                                membresia.setEstado(estadoNuevoCE);
                                membresia.setFechaUltimoCambio(LocalDateTime.now(ZONA_CO));
                                membresia.setMotivoCambio("CORRECCION_PAGO_EXTERNO");
                                membresiaRepository.save(membresia);
                                guardarHistorial(membresia, estudiante,
                                        fechaInicio, fechaFinAnterior, estadoAnteriorCE,
                                        fechaInicio, nuevaFechaFin, estadoNuevoCE,
                                        "CORRECCION_PAGO_EXTERNO", "LAMBDA");
                                membresiasCorregidas++;
                                detalle.put("fechaFinNueva", nuevaFechaFin.toString());

                                if (nuevaFechaFin.isBefore(hoy) &&
                                        estudiante.getEstadoPago() == Estudiante.EstadoPago.AL_DIA) {
                                    estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                                    estudianteRepository.save(estudiante);
                                    detalle.put("estadoEstudiante", "CORREGIDO_A_EN_MORA");
                                }
                            } else {
                                sinCambio++;
                            }
                        } else {
                            // Sin pagos PAY- reales → desactivar membresía
                            Boolean estadoAnteriorDes = membresia.getEstado();
                            membresia.setEstado(false);
                            membresia.setFechaUltimoCambio(LocalDateTime.now(ZONA_CO));
                            membresia.setMotivoCambio("DESACTIVADA_SIN_PAGOS_APP");
                            membresiaRepository.save(membresia);
                            guardarHistorial(membresia, estudiante,
                                    fechaInicio, fechaFinAnterior, estadoAnteriorDes,
                                    fechaInicio, fechaFinAnterior, false,
                                    "DESACTIVADA_SIN_PAGOS_APP", "LAMBDA");
                            membresiasCorregidas++;
                            detalle.put("membresiaDesactivada", true);

                            if (estudiante.getEstadoPago() == Estudiante.EstadoPago.AL_DIA) {
                                estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                                estudianteRepository.save(estudiante);
                                detalle.put("estadoEstudiante", "CORREGIDO_A_EN_MORA");
                            }
                        }
                    }
                }

                pagoRepository.delete(pagoExterno);
                pagosEliminados++;
                detalle.put("resultado", "ELIMINADO");

            } catch (Exception e) {
                errores++;
                detalle.put("error", e.getMessage());
                detalle.put("resultado", "ERROR");
                log.error("❌ Error procesando pago externo {}: {}", pagoExterno.getIdPago(), e.getMessage());
            }
            detalles.add(detalle);
        }

        log.info("📊 Corrección pagos externos: {} eliminados, {} membresías corregidas, {} sin cambio, {} errores",
                pagosEliminados, membresiasCorregidas, sinCambio, errores);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("tarea", "correccion_pagos_externos");
        resultado.put("fecha", hoy.toString());
        resultado.put("pagosExternosEncontrados", pagosEliminados + sinCambio + errores);
        resultado.put("pagosEliminados", pagosEliminados);
        resultado.put("membresiasCorregidas", membresiasCorregidas);
        resultado.put("sinCambio", sinCambio);
        resultado.put("errores", errores);
        resultado.put("detalles", detalles);
        resultado.put("timestamp", LocalDateTime.now().toString());
        return resultado;
    }

    // ─────────────────────────────────────────────────────────────────
    //  ESTADÍSTICAS (para endpoint de salud)
    // ─────────────────────────────────────────────────────────────────

    public Map<String, Object> obtenerEstadisticas() {
        return Map.of(
            "servicioWhatsAppActivo", metaWhatsAppService.isServicioDisponible(),
            "maxReintentos", maxReintentos,
            "pendientesReintento", recordatorioPagoRepository
                    .findByEstadoEnvioAndIntentosLessThan(EstadoEnvio.FALLIDO, maxReintentos).size(),
            "totalRecordatorios", recordatorioPagoRepository.count()
        );
    }

    private void guardarHistorial(Membresia membresia, Estudiante estudiante,
                                  LocalDate fechaInicioAnt, LocalDate fechaFinAnt, Boolean estadoAnt,
                                  LocalDate fechaInicioNueva, LocalDate fechaFinNueva, Boolean estadoNuevo,
                                  String motivo, String origen) {
        try {
            membresiaHistorialRepository.save(MembresiaHistorial.builder()
                    .membresia(membresia)
                    .estudiante(estudiante)
                    .fechaInicioAnterior(fechaInicioAnt)
                    .fechaFinAnterior(fechaFinAnt)
                    .estadoAnterior(estadoAnt)
                    .fechaInicioNueva(fechaInicioNueva)
                    .fechaFinNueva(fechaFinNueva)
                    .estadoNuevo(estadoNuevo)
                    .motivo(motivo)
                    .origen(origen)
                    .fechaCambio(LocalDateTime.now(ZONA_CO))
                    .build());
        } catch (Exception e) {
            log.warn("⚠️ No se pudo guardar historial membresía {}: {}", membresia.getIdMembresia(), e.getMessage());
        }
    }
}
