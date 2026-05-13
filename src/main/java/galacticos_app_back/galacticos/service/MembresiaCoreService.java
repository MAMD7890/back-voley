package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.CambioEstadoPagoDTO;
import galacticos_app_back.galacticos.dto.MembresiaCoreDTO;
import galacticos_app_back.galacticos.entity.*;
import galacticos_app_back.galacticos.entity.MembresiaCore.EstadoMembresia;
import galacticos_app_back.galacticos.entity.MembresiaCore.OrigenAcuerdo;
import galacticos_app_back.galacticos.entity.MembresiaCore.TipoMembresia;
import galacticos_app_back.galacticos.repository.EstudianteRepository;
import galacticos_app_back.galacticos.repository.MembresiaCoreRepository;
import galacticos_app_back.galacticos.repository.MembresiaRepository;
import galacticos_app_back.galacticos.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MembresiaCoreService {

    @Autowired
    private MembresiaCoreRepository membresiaCoreRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private MembresiaRepository membresiaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private LocalDate hoy() {
        return LocalDate.now(BOGOTA);
    }

    /**
     * Calcula fechaFin usando el billing anchor (diaPago) del estudiante.
     * Ej: diaPago=31, meses=1, fechaInicio=2026-01-31 → 2026-02-28
     */
    public LocalDate calcularFechaFin(LocalDate fechaInicio, int meses, int diaPago) {
        YearMonth targetMonth = YearMonth.from(fechaInicio).plusMonths(meses);
        int dia = Math.min(diaPago, targetMonth.lengthOfMonth());
        return targetMonth.atDay(dia);
    }

    private int calcularMesesSegunMonto(BigDecimal valor) {
        if (valor == null) return 1;
        BigDecimal v = valor.abs();
        if (v.compareTo(new BigDecimal("200000")) >= 0) return 3;
        if (v.compareTo(new BigDecimal("130000")) >= 0) return 2;
        return 1;
    }

    // Planes: ONLINE exacto (80k/150k/210k). EFECTIVO por rango.
    private int calcularMesesDesdeValorPago(BigDecimal valor, Pago.MetodoPago metodo) {
        if (valor == null) return 0;
        BigDecimal v = valor.abs();
        if (metodo == Pago.MetodoPago.ONLINE) {
            if (v.compareTo(new BigDecimal("210000")) == 0) return 3;
            if (v.compareTo(new BigDecimal("150000")) == 0) return 2;
            if (v.compareTo(new BigDecimal("80000"))  == 0) return 1;
            return 0;
        } else {
            if (v.compareTo(new BigDecimal("150000")) > 0) return 3;
            if (v.compareTo(new BigDecimal("90000"))  > 0) return 2;
            return 1;
        }
    }

    // Retorna la fechaFin de la última FINALIZADA del estudiante, o null si no existe.
    private LocalDate ultimaFechaFinFinalizada(Integer idEstudiante) {
        List<MembresiaCore> fins = membresiaCoreRepository.findFinalizadasDeEstudiante(idEstudiante);
        return fins.isEmpty() ? null : fins.get(0).getFechaFin();
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(BOGOTA);
    }

    // Quita el flag esActiva de la membresía que lo tenga actualmente
    private void desactivarActual(Integer idEstudiante) {
        membresiaCoreRepository.findActivas(idEstudiante).forEach(m -> {
            m.setEsActiva(false);
            m.setFechaUltimoCambio(ahora());
            membresiaCoreRepository.save(m);
        });
    }

    // ─── Crear membresía para pago (ONLINE / EFECTIVO) ───────────────────────

    @Transactional
    public MembresiaCore crearMembresiaParaPago(Estudiante estudiante, Pago pago, TipoMembresia tipo) {
        LocalDate hoy = hoy();
        int meses = calcularMesesDesdeValorPago(pago.getValor(), pago.getMetodoPago());
        if (meses == 0) meses = calcularMesesSegunMonto(pago.getValor()); // fallback

        if (membresiaCoreRepository.existsByPagoOrigenIdPago(pago.getIdPago())) {
            throw new IllegalStateException(
                    "El pago " + pago.getIdPago() + " ya está vinculado a una membresía existente");
        }

        boolean esActivo = Boolean.TRUE.equals(estudiante.getEstado());
        Optional<MembresiaCore> activaOpt = membresiaCoreRepository
                .findByEstudianteIdEstudianteAndEsActivaTrue(estudiante.getIdEstudiante());

        MembresiaCore membresia;

        if (activaOpt.isPresent()) {
            MembresiaCore activa = activaOpt.get();
            boolean vigente = activa.getEstadoMembresia() == EstadoMembresia.PAGADA
                    && !activa.getFechaFin().isBefore(hoy);

            if (vigente) {
                // Pago anticipado: nuevo período encola tras la vigente
                LocalDate fechaInicio = activa.getFechaFin();
                int diaPago = fechaInicio.getDayOfMonth();
                LocalDate fechaFin = calcularFechaFin(fechaInicio, meses, diaPago);
                membresia = buildMembresia(estudiante, pago, tipo, fechaInicio, fechaFin,
                        EstadoMembresia.PAGADA, false);
                membresiaCoreRepository.save(membresia);
                estudianteRepository.save(estudiante);
                return membresia;
            }

            // Activa es FINALIZADA o ACUERDO_PAGO (estudiante en mora o con acuerdo)
            if (activa.getTipoMembresia() == TipoMembresia.ACUERDO_PAGO) {
                activa.setEstadoMembresia(EstadoMembresia.CANCELADA);
                activa.setMotivoCambio("PAGO_RECIBIDO");
                activa.setFechaUltimoCambio(ahora());
                membresiaCoreRepository.save(activa);
            }
            desactivarActual(estudiante.getIdEstudiante());

            if (esActivo) {
                // PENDIENTE_REGISTRO con fechaInicio asignada (reactivación o cambiarFechas manual):
                // usar esa fecha como base en lugar de buscar la última FINALIZADA.
                // Para cualquier otro tipo activo, continúa desde la última FINALIZADA.
                LocalDate base;
                if (activa.getTipoMembresia() == TipoMembresia.PENDIENTE_REGISTRO
                        && activa.getFechaInicio() != null) {
                    base = activa.getFechaInicio();
                } else {
                    base = ultimaFechaFinFinalizada(estudiante.getIdEstudiante());
                    if (base == null) base = pago.getFechaPago() != null ? pago.getFechaPago() : hoy;
                }
                int diaPago = estudiante.getDiaPago() != null ? estudiante.getDiaPago() : base.getDayOfMonth();
                LocalDate fechaFin = calcularFechaFin(base, meses, diaPago);
                boolean saldada = !fechaFin.isBefore(hoy);
                EstadoMembresia estado = saldada ? EstadoMembresia.PAGADA : EstadoMembresia.FINALIZADA;
                membresia = buildMembresia(estudiante, pago, tipo, base, fechaFin, estado, true);
                if (saldada) {
                    estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
                    estudiante.setDiaPago(diaPago);
                } else {
                    estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                }
            } else {
                // Inactivo: arranque limpio
                LocalDate fechaInicio = pago.getFechaPago() != null ? pago.getFechaPago() : hoy;
                int diaPago = estudiante.getDiaPago() != null ? estudiante.getDiaPago() : fechaInicio.getDayOfMonth();
                LocalDate fechaFin = calcularFechaFin(fechaInicio, meses, diaPago);
                membresia = buildMembresia(estudiante, pago, tipo, fechaInicio, fechaFin,
                        EstadoMembresia.PAGADA, true);
                estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
                estudiante.setDiaPago(diaPago);
            }

        } else {
            // Sin membresía activa: primer pago
            LocalDate fechaInicio = pago.getFechaPago() != null ? pago.getFechaPago() : hoy;
            int diaPago = estudiante.getDiaPago() != null ? estudiante.getDiaPago() : fechaInicio.getDayOfMonth();
            LocalDate fechaFin = calcularFechaFin(fechaInicio, meses, diaPago);
            membresia = buildMembresia(estudiante, pago, tipo, fechaInicio, fechaFin,
                    EstadoMembresia.PAGADA, true);
            estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
            estudiante.setDiaPago(diaPago);
        }

        if (!Boolean.TRUE.equals(estudiante.getEstado())) estudiante.setEstado(true);
        estudiante.setCambiadoManualmente(false);
        estudianteRepository.save(estudiante);
        return membresiaCoreRepository.save(membresia);
    }

    private MembresiaCore buildMembresia(Estudiante est, Pago pago, TipoMembresia tipo,
                                          LocalDate inicio, LocalDate fin,
                                          EstadoMembresia estado, boolean activa) {
        int meses = calcularMesesDesdeValorPago(pago.getValor(), pago.getMetodoPago());
        if (meses == 0) meses = 1;
        MembresiaCore m = new MembresiaCore();
        m.setEstudiante(est);
        m.setPagoOrigen(pago);
        m.setTipoMembresia(tipo);
        m.setEstadoMembresia(estado);
        m.setFechaInicio(inicio);
        m.setFechaFin(fin);
        m.setValorMensual(pago.getValor() != null
                ? pago.getValor().divide(new BigDecimal(meses), 2, java.math.RoundingMode.HALF_UP)
                : null);
        m.setEsActiva(activa);
        m.setFechaCreacion(ahora());
        m.setFechaUltimoCambio(ahora());
        m.setMotivoCambio("PAGO_CONFIRMADO");
        return m;
    }

    // ─── Crear acuerdo de pago ────────────────────────────────────────────────

    @Transactional
    public MembresiaCoreDTO crearAcuerdoPago(Integer idEstudiante, LocalDate fechaLimiteCompromiso,
                                              String observacion) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado: " + idEstudiante));

        List<TipoMembresia> tiposReemplazables = Arrays.asList(
                TipoMembresia.PENDIENTE_REGISTRO, TipoMembresia.MORA);
        List<EstadoMembresia> estadosReemplazables = Arrays.asList(
                EstadoMembresia.PENDIENTE_PAGO, EstadoMembresia.EN_MORA);

        List<MembresiaCore> reemplazables = membresiaCoreRepository.findConvertibles(
                idEstudiante, tiposReemplazables, estadosReemplazables);

        if (reemplazables.isEmpty()) {
            throw new RuntimeException("No hay membresía reemplazable para crear acuerdo de pago");
        }

        MembresiaCore reemplazable = reemplazables.get(0);
        OrigenAcuerdo origen = reemplazable.getTipoMembresia() == TipoMembresia.PENDIENTE_REGISTRO
                ? OrigenAcuerdo.DESDE_PENDIENTE
                : OrigenAcuerdo.DESDE_MORA;

        // Cancelar el registro reemplazable
        reemplazable.setEstadoMembresia(EstadoMembresia.CANCELADA);
        reemplazable.setEsActiva(false);
        reemplazable.setMotivoCambio("REEMPLAZADO_POR_ACUERDO");
        reemplazable.setFechaUltimoCambio(ahora());
        membresiaCoreRepository.save(reemplazable);

        // Crear nueva membresía tipo ACUERDO_PAGO
        MembresiaCore acuerdo = new MembresiaCore();
        acuerdo.setEstudiante(estudiante);
        acuerdo.setEquipo(reemplazable.getEquipo());
        acuerdo.setPlan(reemplazable.getPlan());
        acuerdo.setPagoOrigen(null);
        acuerdo.setTipoMembresia(TipoMembresia.ACUERDO_PAGO);
        acuerdo.setEstadoMembresia(EstadoMembresia.PENDIENTE_PAGO);
        acuerdo.setFechaInicio(reemplazable.getFechaInicio());
        acuerdo.setFechaFin(reemplazable.getFechaFin());
        acuerdo.setValorMensual(reemplazable.getValorMensual());
        acuerdo.setObservacion(observacion);
        acuerdo.setFechaLimiteCompromiso(fechaLimiteCompromiso);
        acuerdo.setOrigenAcuerdo(origen);
        acuerdo.setEsActiva(true);
        acuerdo.setFechaCreacion(ahora());
        acuerdo.setFechaUltimoCambio(ahora());

        estudiante.setEstadoPago(Estudiante.EstadoPago.COMPROMISO_PAGO);
        estudianteRepository.save(estudiante);

        return MembresiaCoreDTO.from(membresiaCoreRepository.save(acuerdo));
    }

    // ─── cambiarEstadoPago — mismo flujo que el original + crea MembresiaCore ─

    /**
     * Replica el comportamiento de EstudianteService.cambiarEstadoPago() y,
     * cuando el nuevo estado es COMPROMISO_PAGO, también crea el registro
     * MembresiaCore de tipo ACUERDO_PAGO.
     *
     * Si el estudiante no tiene membresía convertible en membresia_core
     * (ej: la migración todavía no corrió), solo actualiza el estudiante
     * sin lanzar error, para no romper el flujo existente.
     */
    @Transactional
    public Estudiante cambiarEstadoPago(Integer idEstudiante, CambioEstadoPagoDTO cambioDTO) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado: " + idEstudiante));

        Estudiante.EstadoPago estadoAnterior = estudiante.getEstadoPago();
        estudiante.setEstadoPago(cambioDTO.getNuevoEstado());
        estudiante.setCambiadoManualmente(true);
        System.out.println(String.format("[MembresiaCoreService] Estado cambiado - Estudiante: %s, De: %s, A: %s",
                estudiante.getNombreCompleto(), estadoAnterior, cambioDTO.getNuevoEstado()));

        if (cambioDTO.getNuevoEstado() == Estudiante.EstadoPago.COMPROMISO_PAGO) {
            estudiante.setFechaLimiteCompromiso(cambioDTO.getFechaLimiteCompromiso());
        } else {
            estudiante.setFechaLimiteCompromiso(null);
        }
        estudiante.setObservacionPago(cambioDTO.getObservacion());
        estudianteRepository.save(estudiante);

        // Crear ACUERDO_PAGO en MembresiaCore cuando el nuevo estado es COMPROMISO_PAGO
        if (cambioDTO.getNuevoEstado() == Estudiante.EstadoPago.COMPROMISO_PAGO) {
            try {
                // Buscar última FINALIZADA para usarla como base del acuerdo
                List<MembresiaCore> finalizadas = membresiaCoreRepository
                        .findFinalizadasDeEstudiante(idEstudiante);

                // Determinar fechaInicio del acuerdo
                LocalDate inicioAcuerdo;
                OrigenAcuerdo origen;
                if (cambioDTO.getFechaInicio() != null) {
                    inicioAcuerdo = cambioDTO.getFechaInicio();
                    origen = finalizadas.isEmpty() ? OrigenAcuerdo.DESDE_PENDIENTE : OrigenAcuerdo.DESDE_MORA;
                } else if (!finalizadas.isEmpty()) {
                    inicioAcuerdo = finalizadas.get(0).getFechaFin();
                    origen = OrigenAcuerdo.DESDE_MORA;
                } else {
                    inicioAcuerdo = hoy();
                    origen = OrigenAcuerdo.DESDE_PENDIENTE;
                }

                // Determinar fechaFin del acuerdo
                int diaPago = estudiante.getDiaPago() != null
                        ? estudiante.getDiaPago() : inicioAcuerdo.getDayOfMonth();
                LocalDate finAcuerdo = cambioDTO.getFechaFin() != null
                        ? cambioDTO.getFechaFin()
                        : calcularFechaFin(inicioAcuerdo, 1, diaPago);

                // Valor mensual de la última finalizada si existe
                BigDecimal valorMensual = finalizadas.isEmpty() ? null : finalizadas.get(0).getValorMensual();

                // Desactivar la membresía activa actual (sin cancelarla — queda en historial)
                desactivarActual(idEstudiante);

                MembresiaCore acuerdo = new MembresiaCore();
                acuerdo.setEstudiante(estudiante);
                acuerdo.setPagoOrigen(null);
                acuerdo.setTipoMembresia(TipoMembresia.ACUERDO_PAGO);
                acuerdo.setEstadoMembresia(EstadoMembresia.PENDIENTE_PAGO);
                acuerdo.setFechaInicio(inicioAcuerdo);
                acuerdo.setFechaFin(finAcuerdo);
                acuerdo.setValorMensual(valorMensual);
                acuerdo.setObservacion(cambioDTO.getObservacion());
                acuerdo.setFechaLimiteCompromiso(cambioDTO.getFechaLimiteCompromiso());
                acuerdo.setOrigenAcuerdo(origen);
                acuerdo.setEsActiva(true);
                acuerdo.setFechaCreacion(ahora());
                acuerdo.setFechaUltimoCambio(ahora());
                membresiaCoreRepository.save(acuerdo);

            } catch (Exception e) {
                System.err.println("[MembresiaCoreService] Error creando ACUERDO_PAGO para estudiante "
                        + idEstudiante + ": " + e.getMessage());
            }
        }

        return estudiante;
    }

    // ─── Crear membresía PENDIENTE_REGISTRO (carga Excel) ────────────────────

    @Transactional
    public MembresiaCore crearMembresiaRegistroExcel(Estudiante estudiante) {
        LocalDate fechaInicio = hoy();
        int diaPago = fechaInicio.getDayOfMonth();
        LocalDate fechaFin = calcularFechaFin(fechaInicio, 1, diaPago);
        LocalDate fechaLimiteGracia = fechaInicio.plusDays(15);

        estudiante.setDiaPago(diaPago);
        estudianteRepository.save(estudiante);

        MembresiaCore membresia = new MembresiaCore();
        membresia.setEstudiante(estudiante);
        membresia.setTipoMembresia(TipoMembresia.PENDIENTE_REGISTRO);
        membresia.setEstadoMembresia(EstadoMembresia.PENDIENTE_PAGO);
        membresia.setFechaInicio(fechaInicio);
        membresia.setFechaFin(fechaFin);
        membresia.setFechaLimiteGracia(fechaLimiteGracia);
        membresia.setPagoOrigen(null);
        membresia.setEsActiva(true);
        membresia.setFechaCreacion(ahora());
        membresia.setFechaUltimoCambio(ahora());

        return membresiaCoreRepository.save(membresia);
    }

    // ─── Pagos del estudiante ─────────────────────────────────────────────────

    public List<Pago> obtenerPagosEstudiante(Integer idEstudiante) {
        return pagoRepository.findByEstudianteIdEstudiante(idEstudiante);
    }

    // ─── Cambiar fechaInicio y/o fechaFin de una membresía ───────────────────

    @Transactional
    public MembresiaCoreDTO cambiarFechas(Integer idMembresiaCore,
                                          LocalDate nuevaFechaInicio,
                                          LocalDate nuevaFechaFin) {
        MembresiaCore mc = membresiaCoreRepository.findById(idMembresiaCore)
                .orElseThrow(() -> new RuntimeException("Membresía no encontrada: " + idMembresiaCore));

        LocalDate hoy           = hoy();
        LocalDate inicioAnterior = mc.getFechaInicio();
        LocalDate finAnterior    = mc.getFechaFin();

        // ── Caso reactivación: solo cambia fechaInicio y supera la fechaFin anterior ─
        // El estudiante estaba vencido/inactivo y el admin lo reactiva desde una nueva fecha.
        // Se resetea a PENDIENTE_PAGO sin tocar fechaFin (se recalcula desde el nuevo inicio).
        if (nuevaFechaInicio != null && nuevaFechaFin == null
                && finAnterior != null && nuevaFechaInicio.isAfter(finAnterior)
                && !nuevaFechaInicio.equals(inicioAnterior)) {

            Integer rawDiaPago = mc.getEstudiante().getDiaPago();
            int diaPago = rawDiaPago != null ? rawDiaPago : nuevaFechaInicio.getDayOfMonth();
            LocalDate nuevaFechaFinCalculada = calcularFechaFin(nuevaFechaInicio, 1, diaPago);

            desactivarActual(mc.getEstudiante().getIdEstudiante());
            mc.setFechaInicio(nuevaFechaInicio);
            mc.setFechaFin(nuevaFechaFinCalculada);
            mc.setFechaLimiteGracia(nuevaFechaInicio.plusDays(15));
            mc.setEstadoMembresia(EstadoMembresia.PENDIENTE_PAGO);
            mc.setTipoMembresia(TipoMembresia.PENDIENTE_REGISTRO);
            mc.setPagoOrigen(null);
            mc.setEsActiva(true);
            mc.setMotivoCambio("REACTIVACION_MANUAL");
            mc.setFechaUltimoCambio(ahora());

            Estudiante estudiante = mc.getEstudiante();
            estudiante.setEstado(true);
            estudiante.setEstadoPago(Estudiante.EstadoPago.PENDIENTE);
            estudianteRepository.save(estudiante);

            return MembresiaCoreDTO.from(membresiaCoreRepository.save(mc));
        }

        // ── Validación normal ────────────────────────────────────────────────────────
        LocalDate inicioFinal = nuevaFechaInicio != null ? nuevaFechaInicio : inicioAnterior;
        LocalDate finFinal    = nuevaFechaFin    != null ? nuevaFechaFin    : finAnterior;

        if (inicioFinal != null && finFinal != null && inicioFinal.isAfter(finFinal)) {
            throw new IllegalArgumentException(
                    "fechaInicio (" + inicioFinal + ") no puede ser posterior a fechaFin (" + finFinal + ")");
        }

        boolean cambio = false;

        if (nuevaFechaInicio != null && !nuevaFechaInicio.equals(inicioAnterior)) {
            mc.setFechaInicio(nuevaFechaInicio);
            cambio = true;
        }
        if (nuevaFechaFin != null && !nuevaFechaFin.equals(finAnterior)) {
            mc.setFechaFin(nuevaFechaFin);
            cambio = true;
        }

        if (!cambio) {
            return MembresiaCoreDTO.from(mc);
        }

        // ── Si la fechaFin resultante es futura, marcar como vigente ─────────────────
        if (finFinal != null && finFinal.isAfter(hoy)) {
            desactivarActual(mc.getEstudiante().getIdEstudiante());
            mc.setEstadoMembresia(EstadoMembresia.PAGADA);
            mc.setEsActiva(true);
            Estudiante estudiante = mc.getEstudiante();
            estudiante.setEstado(true);
            estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
            estudianteRepository.save(estudiante);
        }

        mc.setMotivoCambio("FECHAS_AJUSTADAS_MANUALMENTE");
        mc.setFechaUltimoCambio(ahora());
        return MembresiaCoreDTO.from(membresiaCoreRepository.save(mc));
    }

    // ─── Cambiar fechas por idEstudiante (nuevo endpoint) ────────────────────

    @Transactional
    public MembresiaCoreDTO cambiarFechasPorEstudiante(Integer idEstudiante,
                                                        LocalDate nuevaFechaInicio,
                                                        LocalDate nuevaFechaFin) {
        if (nuevaFechaInicio == null && nuevaFechaFin == null) {
            throw new IllegalArgumentException("Debe enviar al menos fechaFin");
        }

        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado: " + idEstudiante));

        LocalDate hoy = hoy();

        Optional<MembresiaCore> activaOpt =
                membresiaCoreRepository.findByEstudianteIdEstudianteAndEsActivaTrue(idEstudiante);

        // ── Sin membresía activa ──────────────────────────────────────────────
        if (activaOpt.isEmpty()) {
            if (nuevaFechaInicio == null) {
                throw new IllegalArgumentException(
                        "El estudiante no tiene membresía activa. Envíe fechaInicio y fechaFin para crear una nueva.");
            }
            LocalDate finFinal = nuevaFechaFin != null ? nuevaFechaFin
                    : calcularFechaFin(nuevaFechaInicio, 1, nuevaFechaInicio.getDayOfMonth());
            MembresiaCore nueva = buildPendienteManual(estudiante, nuevaFechaInicio, finFinal);
            estudiante.setEstadoPago(Estudiante.EstadoPago.PENDIENTE);
            estudianteRepository.save(estudiante);
            return MembresiaCoreDTO.from(membresiaCoreRepository.save(nueva));
        }

        MembresiaCore activa = activaOpt.get();

        // ── Activa es PAGADA → actualizar fechas y re-evaluar estado ─────────
        if (activa.getEstadoMembresia() == EstadoMembresia.PAGADA) {
            if (nuevaFechaInicio != null) activa.setFechaInicio(nuevaFechaInicio);
            if (nuevaFechaFin != null) activa.setFechaFin(nuevaFechaFin);
            LocalDate finResultante = activa.getFechaFin();
            if (finResultante != null && !finResultante.isAfter(hoy)) {
                // fechaFin quedó en el pasado → vencida
                activa.setEstadoMembresia(EstadoMembresia.FINALIZADA);
                estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                estudianteRepository.save(estudiante);
            }
            activa.setMotivoCambio("FECHAS_AJUSTADAS_MANUALMENTE");
            activa.setFechaUltimoCambio(ahora());
            return MembresiaCoreDTO.from(membresiaCoreRepository.save(activa));
        }

        // ── Activa es FINALIZADA ──────────────────────────────────────────────
        if (activa.getEstadoMembresia() == EstadoMembresia.FINALIZADA) {
            if (nuevaFechaInicio != null && nuevaFechaFin != null) {
                // Crear nueva PENDIENTE_REGISTRO con el rango indicado
                activa.setEsActiva(false);
                activa.setFechaUltimoCambio(ahora());
                membresiaCoreRepository.save(activa);
                MembresiaCore nueva = buildPendienteManual(estudiante, nuevaFechaInicio, nuevaFechaFin);
                estudiante.setEstadoPago(Estudiante.EstadoPago.PENDIENTE);
                estudianteRepository.save(estudiante);
                return MembresiaCoreDTO.from(membresiaCoreRepository.save(nueva));
            }
            // Solo fechaInicio → mover solo el inicio, conservar fechaFin y estado
            if (nuevaFechaFin == null) {
                activa.setFechaInicio(nuevaFechaInicio);
                activa.setMotivoCambio("FECHAS_AJUSTADAS_MANUALMENTE");
                activa.setFechaUltimoCambio(ahora());
                return MembresiaCoreDTO.from(membresiaCoreRepository.save(activa));
            }
            // Solo fechaFin → mover fechaFin de la FINALIZADA activa
            activa.setFechaFin(nuevaFechaFin);
            if (nuevaFechaFin.isAfter(hoy)) {
                activa.setEstadoMembresia(EstadoMembresia.PAGADA);
                estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
                estudianteRepository.save(estudiante);
            }
            activa.setMotivoCambio("FECHAS_AJUSTADAS_MANUALMENTE");
            activa.setFechaUltimoCambio(ahora());
            return MembresiaCoreDTO.from(membresiaCoreRepository.save(activa));
        }

        // ── Cualquier otro estado (PENDIENTE_PAGO, EN_MORA…) → actualizar fechas
        if (nuevaFechaInicio != null) activa.setFechaInicio(nuevaFechaInicio);
        if (nuevaFechaFin != null) activa.setFechaFin(nuevaFechaFin);
        activa.setMotivoCambio("FECHAS_AJUSTADAS_MANUALMENTE");
        activa.setFechaUltimoCambio(ahora());
        return MembresiaCoreDTO.from(membresiaCoreRepository.save(activa));
    }

    private MembresiaCore buildPendienteManual(Estudiante estudiante,
                                                LocalDate fechaInicio, LocalDate fechaFin) {
        MembresiaCore m = new MembresiaCore();
        m.setEstudiante(estudiante);
        m.setTipoMembresia(TipoMembresia.PENDIENTE_REGISTRO);
        m.setEstadoMembresia(EstadoMembresia.PENDIENTE_PAGO);
        m.setFechaInicio(fechaInicio);
        m.setFechaFin(fechaFin);
        m.setFechaLimiteGracia(fechaInicio.plusDays(15));
        m.setPagoOrigen(null);
        m.setEsActiva(true);
        m.setFechaCreacion(ahora());
        m.setFechaUltimoCambio(ahora());
        m.setMotivoCambio("FECHAS_ASIGNADAS_MANUALMENTE");
        return m;
    }

    // ─── Vigencia activa del estudiante ──────────────────────────────────────

    public Map<String, Object> obtenerVigencia(Integer idEstudiante) {
        return membresiaCoreRepository.findByEstudianteIdEstudianteAndEsActivaTrue(idEstudiante)
                .map(mc -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("idMembresiaCore", mc.getIdMembresiaCore());
                    r.put("fechaInicio", mc.getFechaInicio());
                    r.put("fechaFin", mc.getFechaFin());
                    r.put("estadoMembresia", mc.getEstadoMembresia());
                    r.put("tipoMembresia", mc.getTipoMembresia());
                    return r;
                })
                .orElse(null);
    }

    // ─── Inactivar / reactivar estudiante manualmente ────────────────────────

    @Transactional
    public void inactivarEstudiante(Integer idEstudiante) {
        desactivarActual(idEstudiante);
    }

    /**
     * Cuando el admin reactiva un estudiante que estaba en mora:
     * crea una nueva membresía EN_MORA desde hoy con 15 días de gracia.
     * La inactivación posterior siempre es manual.
     */
    @Transactional
    public Estudiante.EstadoPago reactivarEstudiante(Integer idEstudiante) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado: " + idEstudiante));

        LocalDate hoy = hoy();

        // Si tiene una PAGADA vigente (inactivado antes de que venciera), restaurarla
        List<MembresiaCore> vigentes = membresiaCoreRepository.findVigentesDeEstudiante(idEstudiante, hoy);
        if (!vigentes.isEmpty()) {
            MembresiaCore pagadaVigente = vigentes.get(0);
            desactivarActual(idEstudiante);
            pagadaVigente.setEsActiva(true);
            pagadaVigente.setFechaUltimoCambio(ahora());
            pagadaVigente.setMotivoCambio("REACTIVACION_MANUAL");
            membresiaCoreRepository.save(pagadaVigente);
            estudiante.setEstado(true);
            estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
            estudianteRepository.save(estudiante);
            return Estudiante.EstadoPago.AL_DIA;
        }

        // Sin PAGADA vigente → crear PENDIENTE_REGISTRO con 15 días de gracia
        // Job 3 convierte a EN_MORA si no hay pago en ese plazo
        Integer rawDiaPago = estudiante.getDiaPago();
        int diaPago = rawDiaPago != null ? rawDiaPago : hoy.getDayOfMonth();

        desactivarActual(idEstudiante);

        MembresiaCore pendiente = new MembresiaCore();
        pendiente.setEstudiante(estudiante);
        pendiente.setTipoMembresia(TipoMembresia.PENDIENTE_REGISTRO);
        pendiente.setEstadoMembresia(EstadoMembresia.PENDIENTE_PAGO);
        pendiente.setFechaInicio(hoy);
        pendiente.setFechaFin(calcularFechaFin(hoy, 1, diaPago));
        pendiente.setFechaLimiteGracia(hoy.plusDays(15));
        pendiente.setPagoOrigen(null);
        pendiente.setEsActiva(true);
        pendiente.setFechaCreacion(ahora());
        pendiente.setFechaUltimoCambio(ahora());
        pendiente.setMotivoCambio("REACTIVACION_MANUAL");
        membresiaCoreRepository.save(pendiente);

        estudiante.setEstado(true);
        estudiante.setEstadoPago(Estudiante.EstadoPago.PENDIENTE);
        estudianteRepository.save(estudiante);
        return Estudiante.EstadoPago.PENDIENTE;
    }

    // ─── Historial de membresías ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MembresiaCoreDTO> obtenerHistorico(Integer idEstudiante) {
        return membresiaCoreRepository
                .findByEstudianteIdEstudianteOrderByFechaInicioDesc(idEstudiante)
                .stream()
                .map(MembresiaCoreDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<MembresiaCoreDTO> obtenerMembresiaActiva(Integer idEstudiante) {
        return membresiaCoreRepository
                .findByEstudianteIdEstudianteAndEsActivaTrue(idEstudiante)
                .map(MembresiaCoreDTO::from);
    }

    // ─── Job 1 — Activar períodos futuros ────────────────────────────────────

    @Transactional
    public Map<String, Object> ejecutarJob1ActivarPeriodosFuturos() {
        LocalDate hoy = hoy();
        List<MembresiaCore> iniciandoHoy = membresiaCoreRepository.findPagadasQueInicianHoy(hoy);

        int activadas = 0;
        int errores = 0;

        for (MembresiaCore mc : iniciandoHoy) {
            try {
                Integer idEstudiante = mc.getEstudiante().getIdEstudiante();
                // Marcar membresía anterior como FINALIZADA
                List<MembresiaCore> anteriores = membresiaCoreRepository
                        .findPagadasVencidasDeEstudiante(idEstudiante, hoy);
                for (MembresiaCore anterior : anteriores) {
                    if (!anterior.getIdMembresiaCore().equals(mc.getIdMembresiaCore())) {
                        anterior.setEstadoMembresia(EstadoMembresia.FINALIZADA);
                        anterior.setEsActiva(false);
                        anterior.setMotivoCambio("PERIODO_SIGUIENTE_ACTIVADO");
                        anterior.setFechaUltimoCambio(ahora());
                        membresiaCoreRepository.save(anterior);
                        break;
                    }
                }

                mc.setEsActiva(true);
                mc.setFechaUltimoCambio(ahora());
                membresiaCoreRepository.save(mc);

                Estudiante estudiante = mc.getEstudiante();
                estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
                estudianteRepository.save(estudiante);
                activadas++;
            } catch (Exception e) {
                errores++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("job", "ActivarPeriodosFuturos");
        resultado.put("fecha", hoy.toString());
        resultado.put("procesadas", iniciandoHoy.size());
        resultado.put("activadas", activadas);
        resultado.put("errores", errores);
        return resultado;
    }

    // ─── Job 2 — Detectar vencimientos ───────────────────────────────────────

    @Transactional
    public Map<String, Object> ejecutarJob2DetectarVencimientos() {
        LocalDate hoy = hoy();
        List<MembresiaCore> vencidas = membresiaCoreRepository.findPagadasVencidas(hoy);

        int procesadas = 0;
        int omitidas = 0;
        int errores = 0;

        for (MembresiaCore mc : vencidas) {
            try {
                Integer idEstudiante = mc.getEstudiante().getIdEstudiante();

                long otrasActivas = membresiaCoreRepository.countMembresiasActivasOEnMora(
                        idEstudiante, mc.getIdMembresiaCore());
                if (otrasActivas > 0) {
                    omitidas++;
                    continue;
                }

                Estudiante estudiante = mc.getEstudiante();

                // Estudiante inactivo: FINALIZADA con esActiva=true (representa el estado actual),
                // sin crear nueva EN_MORA — la inactivación ya fue manual.
                if (Boolean.FALSE.equals(estudiante.getEstado())) {
                    mc.setEstadoMembresia(EstadoMembresia.FINALIZADA);
                    mc.setEsActiva(true);
                    mc.setMotivoCambio("VENCIDA_ESTUDIANTE_INACTIVO");
                    mc.setFechaUltimoCambio(ahora());
                    membresiaCoreRepository.save(mc);
                    procesadas++;
                    continue;
                }

                // PAGADA vencida → FINALIZADA, queda activa (muestra última fecha pagada)
                mc.setEstadoMembresia(EstadoMembresia.FINALIZADA);
                mc.setEsActiva(true);
                mc.setMotivoCambio("VENCIDA_SIN_PAGO_SIGUIENTE");
                mc.setFechaUltimoCambio(ahora());
                membresiaCoreRepository.save(mc);

                estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                estudianteRepository.save(estudiante);
                procesadas++;

            } catch (Exception e) {
                errores++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("job", "DetectarVencimientos");
        resultado.put("fecha", hoy.toString());
        resultado.put("evaluadas", vencidas.size());
        resultado.put("finalizadasActivas", procesadas);
        resultado.put("omitidas", omitidas);
        resultado.put("errores", errores);
        return resultado;
    }

    // ─── Job 3 — Convertir PENDIENTE_PAGO con gracia vencida → EN_MORA ──────

    @Transactional
    public Map<String, Object> ejecutarJob3CancelarGraciasVencidas() {
        LocalDate hoy = hoy();
        List<MembresiaCore> pendientes = membresiaCoreRepository.findPendientesConGraciaVencida(hoy);

        int convertidas = 0;
        int errores = 0;

        for (MembresiaCore mc : pendientes) {
            try {
                mc.setEstadoMembresia(EstadoMembresia.EN_MORA);
                mc.setMotivoCambio("GRACIA_VENCIDA_PASA_A_MORA");
                mc.setFechaUltimoCambio(ahora());
                membresiaCoreRepository.save(mc);

                Estudiante estudiante = mc.getEstudiante();
                estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                estudianteRepository.save(estudiante);
                convertidas++;
            } catch (Exception e) {
                errores++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("job", "PendientesAMora");
        resultado.put("fecha", hoy.toString());
        resultado.put("convertidas", convertidas);
        resultado.put("errores", errores);
        return resultado;
    }

    // ─── Job 4 — Cancelar acuerdos vencidos ──────────────────────────────────

    @Transactional
    public Map<String, Object> ejecutarJob4CancelarAcuerdosVencidos() {
        LocalDate hoy = hoy();
        List<MembresiaCore> acuerdosVencidos = membresiaCoreRepository.findAcuerdosVencidos(hoy);

        int canceladas = 0;
        int errores = 0;

        for (MembresiaCore mc : acuerdosVencidos) {
            try {
                // Marcar acuerdo como COMPROMISO_INCUMPLIDO
                mc.setEstadoMembresia(EstadoMembresia.COMPROMISO_INCUMPLIDO);
                mc.setEsActiva(false);
                mc.setMotivoCambio("ACUERDO_VENCIDO_SIN_PAGO");
                mc.setFechaUltimoCambio(ahora());
                membresiaCoreRepository.save(mc);

                Estudiante estudiante = mc.getEstudiante();

                // Restaurar la última FINALIZADA como activa
                List<MembresiaCore> finalizadas = membresiaCoreRepository
                        .findFinalizadasDeEstudiante(estudiante.getIdEstudiante());
                if (!finalizadas.isEmpty()) {
                    MembresiaCore ultima = finalizadas.get(0);
                    ultima.setEsActiva(true);
                    ultima.setFechaUltimoCambio(ahora());
                    membresiaCoreRepository.save(ultima);
                }

                if (mc.getOrigenAcuerdo() == OrigenAcuerdo.DESDE_PENDIENTE) {
                    estudiante.setEstado(false);
                }
                estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                estudianteRepository.save(estudiante);
                canceladas++;
            } catch (Exception e) {
                errores++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("job", "CancelarAcuerdosVencidos");
        resultado.put("fecha", hoy.toString());
        resultado.put("canceladas", canceladas);
        resultado.put("errores", errores);
        return resultado;
    }

    // ─── Migración ────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> migrarMembresiasExistentes() {
        LocalDate hoy = hoy();
        List<Estudiante> todos = estudianteRepository.findAll();

        int migradas = 0;
        int omitidas = 0;
        int errores = 0;

        for (Estudiante est : todos) {
            try {
                Integer idEst = est.getIdEstudiante();

                // Pagos PAGADOS ordenados ASC
                List<Pago> pagos = pagoRepository.findPagadosByEstudianteOrderByFechaAsc(idEst);

                // Calcular total de meses cubiertos y encontrar el pago más antiguo válido
                int totalMeses = 0;
                Pago pagoMasViejo = null;
                BigDecimal totalPagado = BigDecimal.ZERO;

                for (Pago p : pagos) {
                    int m = calcularMesesDesdeValorPago(p.getValor(), p.getMetodoPago());
                    if (m > 0) {
                        totalMeses += m;
                        if (pagoMasViejo == null) pagoMasViejo = p;
                        if (p.getValor() != null) totalPagado = totalPagado.add(p.getValor());
                    }
                }

                if (pagoMasViejo == null || totalMeses == 0) {
                    omitidas++;
                    continue;
                }

                // fechaInicio: día (diaPago o 3) del mes del pago más antiguo
                LocalDate fechaPagoViejo = pagoMasViejo.getFechaPago() != null
                        ? pagoMasViejo.getFechaPago() : hoy;
                int dia = (est.getDiaPago() != null && est.getDiaPago() > 0) ? est.getDiaPago() : 3;
                YearMonth ym = YearMonth.from(fechaPagoViejo);
                dia = Math.min(dia, ym.lengthOfMonth());
                LocalDate fechaInicio = fechaPagoViejo.withDayOfMonth(dia);

                // fechaFin: fechaInicio + totalMeses
                LocalDate fechaFin = calcularFechaFin(fechaInicio, totalMeses, dia);

                // valorMensual promedio
                BigDecimal valorMensual = totalMeses > 0
                        ? totalPagado.divide(new BigDecimal(totalMeses), 2, java.math.RoundingMode.HALF_UP)
                        : null;

                // Estado: PAGADA si vigente, FINALIZADA si vencida (siempre esActiva=true — sin MORA)
                boolean vencida = fechaFin.isBefore(hoy);
                EstadoMembresia estado = vencida ? EstadoMembresia.FINALIZADA : EstadoMembresia.PAGADA;

                MembresiaCore mc = new MembresiaCore();
                mc.setEstudiante(est);
                mc.setPagoOrigen(pagoMasViejo);
                mc.setTipoMembresia(TipoMembresia.EFECTIVO);
                mc.setEstadoMembresia(estado);
                mc.setFechaInicio(fechaInicio);
                mc.setFechaFin(fechaFin);
                mc.setValorMensual(valorMensual);
                mc.setEsActiva(true);
                mc.setFechaCreacion(ahora());
                mc.setFechaUltimoCambio(ahora());
                mc.setMotivoCambio("MIGRADO_DESDE_PAGOS");
                membresiaCoreRepository.save(mc);
                migradas++;

                // Actualizar fechaRegistro si es null
                if (est.getFechaRegistro() == null) {
                    est.setFechaRegistro(fechaInicio);
                    estudianteRepository.save(est);
                }

            } catch (Exception e) {
                errores++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("job", "Migracion");
        resultado.put("fecha", hoy.toString());
        resultado.put("membresiasMigradas", migradas);
        resultado.put("membresiasOmitidas", omitidas);
        resultado.put("errores", errores);
        return resultado;
    }
}
