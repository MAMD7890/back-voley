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
        // diaPago es solo referencia para mora — no afecta la duración de la membresía
        return fechaInicio.plusMonths(meses);
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

    // Elimina pagos duplicados por referenciaPago, conservando el de menor idPago
    private List<Pago> deduplicarPagos(List<Pago> pagos) {
        Map<String, Pago> unicos = new LinkedHashMap<>();
        for (Pago p : pagos) {
            String key = (p.getReferenciaPago() != null && !p.getReferenciaPago().isBlank())
                    ? p.getReferenciaPago()
                    : "_id_" + p.getIdPago();
            unicos.putIfAbsent(key, p);
        }
        return new ArrayList<>(unicos.values());
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
                        && activa.getFechaInicio() != null
                        && activa.getFechaLimiteGracia() != null
                        && !activa.getFechaLimiteGracia().isBefore(hoy)) {
                    // Gracia aún vigente: el admin asignó una fecha de inicio con intención
                    base = activa.getFechaInicio();
                } else {
                    // PENDIENTE_REGISTRO/ACUERDO_PAGO con gracia vencida: usar última FINALIZADA
                    // solo si terminó hace ≤15 días; si fue antes, arrancar desde fechaPago.
                    // FINALIZADA: siempre arrancar desde la última FINALIZADA (sin límite de días).
                    LocalDate ultimaFin = ultimaFechaFinFinalizada(estudiante.getIdEstudiante());
                    boolean esPendiente = activa.getTipoMembresia() == TipoMembresia.PENDIENTE_REGISTRO
                            || activa.getTipoMembresia() == TipoMembresia.ACUERDO_PAGO;
                    if (ultimaFin != null && (!esPendiente || !ultimaFin.isBefore(hoy.minusDays(15)))) {
                        base = ultimaFin;
                    } else {
                        base = pago.getFechaPago() != null ? pago.getFechaPago() : hoy;
                    }
                }
                Integer rawDp1 = estudiante.getDiaPago();
                int diaPago = rawDp1 != null ? rawDp1 : base.getDayOfMonth();
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
                Integer rawDp2 = estudiante.getDiaPago();
                int diaPago = rawDp2 != null ? rawDp2 : fechaInicio.getDayOfMonth();
                LocalDate fechaFin = calcularFechaFin(fechaInicio, meses, diaPago);
                membresia = buildMembresia(estudiante, pago, tipo, fechaInicio, fechaFin,
                        EstadoMembresia.PAGADA, true);
                estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
                estudiante.setDiaPago(diaPago);
            }

        } else {
            // Sin membresía activa: primer pago
            LocalDate fechaInicio = pago.getFechaPago() != null ? pago.getFechaPago() : hoy;
            Integer rawDp3 = estudiante.getDiaPago();
            int diaPago = rawDp3 != null ? rawDp3 : fechaInicio.getDayOfMonth();
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
                // Tomar fechas y valor de la membresía activa actual
                Optional<MembresiaCore> activaAcuerdoOpt =
                        membresiaCoreRepository.findByEstudianteIdEstudianteAndEsActivaTrue(idEstudiante);

                LocalDate inicioAcuerdo;
                LocalDate finAcuerdo;
                OrigenAcuerdo origen;
                BigDecimal valorMensual = null;

                if (activaAcuerdoOpt.isPresent()) {
                    MembresiaCore activaBase = activaAcuerdoOpt.get();
                    inicioAcuerdo = cambioDTO.getFechaInicio() != null
                            ? cambioDTO.getFechaInicio() : activaBase.getFechaInicio();
                    finAcuerdo = cambioDTO.getFechaFin() != null
                            ? cambioDTO.getFechaFin() : activaBase.getFechaFin();
                    valorMensual = activaBase.getValorMensual();
                    origen = OrigenAcuerdo.DESDE_MORA;
                } else {
                    // Sin membresía activa: usar fechas del DTO o calcular desde hoy
                    inicioAcuerdo = cambioDTO.getFechaInicio() != null ? cambioDTO.getFechaInicio() : hoy();
                    if (cambioDTO.getFechaFin() != null) {
                        finAcuerdo = cambioDTO.getFechaFin();
                    } else {
                        Integer dp = estudiante.getDiaPago();
                        int diaPago = dp != null ? dp : inicioAcuerdo.getDayOfMonth();
                        finAcuerdo = calcularFechaFin(inicioAcuerdo, 1, diaPago);
                    }
                    origen = OrigenAcuerdo.DESDE_PENDIENTE;
                }

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
                boolean inicioEnRango = activa.getFechaInicio() != null && activa.getFechaFin() != null
                        && !nuevaFechaInicio.isBefore(activa.getFechaInicio())
                        && !nuevaFechaInicio.isAfter(activa.getFechaFin());
                boolean tienePago = activa.getPagoOrigen() != null;

                if (inicioEnRango && tienePago) {
                    // Ajustar fechas sobre la membresía existente (pago ya vinculado)
                    activa.setFechaInicio(nuevaFechaInicio);
                    activa.setFechaFin(nuevaFechaFin);
                    if (nuevaFechaFin.isAfter(hoy)) {
                        activa.setEstadoMembresia(EstadoMembresia.PAGADA);
                        estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
                    } else {
                        estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                    }
                    activa.setMotivoCambio("FECHAS_AJUSTADAS_MANUALMENTE");
                    activa.setFechaUltimoCambio(ahora());
                    estudianteRepository.save(estudiante);
                    return MembresiaCoreDTO.from(membresiaCoreRepository.save(activa));
                }

                // Fecha inicio fuera del rango o sin pago → crear nueva PENDIENTE_REGISTRO
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
            } else if (estudiante.getEstadoPago() != Estudiante.EstadoPago.COMPROMISO_PAGO) {
                // Solo pasar a EN_MORA si no tiene acuerdo de pago vigente —
                // si tiene COMPROMISO_PAGO el Job 4 es quien decide cuándo pasa a EN_MORA
                estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                estudianteRepository.save(estudiante);
            }
            activa.setMotivoCambio("FECHAS_AJUSTADAS_MANUALMENTE");
            activa.setFechaUltimoCambio(ahora());
            return MembresiaCoreDTO.from(membresiaCoreRepository.save(activa));
        }

        // ── Cualquier otro estado (PENDIENTE_PAGO, EN_MORA…) → actualizar fechas
        if (nuevaFechaInicio != null) activa.setFechaInicio(nuevaFechaInicio);
        if (nuevaFechaFin != null) activa.setFechaFin(nuevaFechaFin);
        // Si la fechaFin resultante quedó en el pasado → pasar a EN_MORA
        LocalDate finResultanteFinal = activa.getFechaFin();
        if (finResultanteFinal != null && !finResultanteFinal.isAfter(hoy)
                && activa.getEstadoMembresia() == EstadoMembresia.PENDIENTE_PAGO) {
            activa.setEstadoMembresia(EstadoMembresia.EN_MORA);
            estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
            estudianteRepository.save(estudiante);
        }
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

    @Transactional(readOnly = true)
    public Map<String, Object> previsualizarJob1ActivarPeriodos() {
        LocalDate hoy = hoy();

        List<MembresiaCore> iniciandoHoy    = membresiaCoreRepository.findPagadasQueInicianHoy(hoy);
        List<MembresiaCore> vigentesNoActivas = membresiaCoreRepository.findPagadasVigentesNoActivas(hoy);

        Map<Integer, MembresiaCore> candidatas = new LinkedHashMap<>();
        iniciandoHoy.forEach(m      -> candidatas.put(m.getIdMembresiaCore(), m));
        vigentesNoActivas.forEach(m -> candidatas.putIfAbsent(m.getIdMembresiaCore(), m));

        List<Map<String, Object>> detalle = new ArrayList<>();
        for (MembresiaCore mc : candidatas.values()) {
            boolean esRecuperacion = !iniciandoHoy.contains(mc);

            // Saltar si ya hay una membresía activa que aún no ha vencido
            boolean hayActivaVigente = membresiaCoreRepository
                    .findActivas(mc.getEstudiante().getIdEstudiante()).stream()
                    .anyMatch(a -> !a.getIdMembresiaCore().equals(mc.getIdMembresiaCore())
                            && a.getFechaFin() != null && !a.getFechaFin().isBefore(hoy));
            if (hayActivaVigente) continue;

            List<MembresiaCore> anteriores = membresiaCoreRepository
                    .findPagadasVencidasDeEstudiante(mc.getEstudiante().getIdEstudiante(), hoy);
            MembresiaCore anterior = anteriores.stream()
                    .filter(a -> !a.getIdMembresiaCore().equals(mc.getIdMembresiaCore()))
                    .findFirst().orElse(null);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idEstudiante",       mc.getEstudiante().getIdEstudiante());
            item.put("nombreEstudiante",   mc.getEstudiante().getNombreCompleto());
            item.put("idMembresiaCore",    mc.getIdMembresiaCore());
            item.put("fechaInicio",        mc.getFechaInicio());
            item.put("fechaFin",           mc.getFechaFin());
            item.put("tipo",               esRecuperacion ? "REACTIVACION" : "ACTIVACION_HOY");
            if (anterior != null) {
                item.put("finalizara", Map.of(
                        "idMembresiaCore", anterior.getIdMembresiaCore(),
                        "fechaFin",        anterior.getFechaFin()
                ));
            }
            detalle.add(item);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("fecha",              hoy.toString());
        resultado.put("totalActivaciones",  detalle.stream().filter(d -> "ACTIVACION_HOY".equals(d.get("tipo"))).count());
        resultado.put("totalReactivaciones",detalle.stream().filter(d -> "REACTIVACION".equals(d.get("tipo"))).count());
        resultado.put("totalProcesara",     detalle.size());
        resultado.put("detalle",            detalle);
        return resultado;
    }

    @Transactional
    public Map<String, Object> ejecutarJob1ActivarPeriodosFuturos() {
        LocalDate hoy = hoy();

        // Fase 1: membresías que inician hoy
        List<MembresiaCore> iniciandoHoy = membresiaCoreRepository.findPagadasQueInicianHoy(hoy);
        // Fase 2: membresías PAGADA vigentes hoy que por algún fallo nunca se activaron
        List<MembresiaCore> vigentesNoActivas = membresiaCoreRepository.findPagadasVigentesNoActivas(hoy);

        // Unir ambas listas evitando duplicados
        Map<Integer, MembresiaCore> candidatas = new LinkedHashMap<>();
        iniciandoHoy.forEach(m -> candidatas.put(m.getIdMembresiaCore(), m));
        vigentesNoActivas.forEach(m -> candidatas.putIfAbsent(m.getIdMembresiaCore(), m));

        int activadas = 0, reactivadas = 0, errores = 0;

        for (MembresiaCore mc : candidatas.values()) {
            boolean esFaseRecuperacion = !iniciandoHoy.contains(mc);
            try {
                Integer idEstudiante = mc.getEstudiante().getIdEstudiante();

                // Saltar si ya hay una membresía activa que aún no ha vencido
                boolean hayActivaVigente = membresiaCoreRepository
                        .findActivas(idEstudiante).stream()
                        .anyMatch(a -> !a.getIdMembresiaCore().equals(mc.getIdMembresiaCore())
                                && a.getFechaFin() != null && !a.getFechaFin().isBefore(hoy));
                if (hayActivaVigente) continue;

                // Finalizar membresías anteriores vencidas
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

                if (esFaseRecuperacion) reactivadas++; else activadas++;
            } catch (Exception e) {
                errores++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("job", "ActivarPeriodosFuturos");
        resultado.put("fecha", hoy.toString());
        resultado.put("procesadas", candidatas.size());
        resultado.put("activadas", activadas);
        resultado.put("reactivadas", reactivadas);
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
                mc.setEstadoMembresia(EstadoMembresia.FINALIZADA);
                mc.setEsActiva(true);
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

    // ─── Job 5 — Sincronizar estadoPago del estudiante con su membresía activa ─

    @Transactional
    public Map<String, Object> ejecutarJobSincronizarEstados() {

        // ── 1. Sincronizar estadoPago del estudiante con su membresía activa ──
        List<MembresiaCore> activas = membresiaCoreRepository.findAllByEsActivaTrue();

        int estadosActualizados = 0;
        int estadosSinCambio    = 0;
        int estadosErrores      = 0;

        for (MembresiaCore mc : activas) {
            try {
                Estudiante.EstadoPago esperado = resolverEstadoPago(mc);
                Estudiante est = mc.getEstudiante();
                if (est.getEstadoPago() != esperado) {
                    est.setEstadoPago(esperado);
                    estudianteRepository.save(est);
                    estadosActualizados++;
                } else {
                    estadosSinCambio++;
                }
            } catch (Exception e) {
                estadosErrores++;
            }
        }

        // ── 2. Corregir tipoMembresia mal asignado durante migración ──────────
        List<MembresiaCore> conTipoIncorrecto = membresiaCoreRepository.findConTipoIncorrecto();

        int tiposCorregidos = 0;
        int tiposErrores    = 0;

        for (MembresiaCore mc : conTipoIncorrecto) {
            try {
                Pago.MetodoPago metodoPago = mc.getPagoOrigen().getMetodoPago();
                TipoMembresia tipoCorrector = metodoPago == Pago.MetodoPago.ONLINE
                        ? TipoMembresia.ONLINE
                        : TipoMembresia.EFECTIVO;
                mc.setTipoMembresia(tipoCorrector);
                mc.setFechaUltimoCambio(ahora());
                membresiaCoreRepository.save(mc);
                tiposCorregidos++;
            } catch (Exception e) {
                tiposErrores++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("job", "SincronizarEstados");
        resultado.put("fecha", hoy().toString());
        resultado.put("estadosEvaluados", activas.size());
        resultado.put("estadosActualizados", estadosActualizados);
        resultado.put("estadosSinCambio", estadosSinCambio);
        resultado.put("estadosErrores", estadosErrores);
        resultado.put("tiposCorregidos", tiposCorregidos);
        resultado.put("tiposErrores", tiposErrores);
        return resultado;
    }

    private Estudiante.EstadoPago resolverEstadoPago(MembresiaCore mc) {
        return switch (mc.getEstadoMembresia()) {
            case PAGADA -> Estudiante.EstadoPago.AL_DIA;
            case PENDIENTE_PAGO -> mc.getTipoMembresia() == TipoMembresia.ACUERDO_PAGO
                    ? Estudiante.EstadoPago.COMPROMISO_PAGO
                    : Estudiante.EstadoPago.PENDIENTE;
            case EN_MORA, FINALIZADA, COMPROMISO_INCUMPLIDO, CANCELADA ->
                    Estudiante.EstadoPago.EN_MORA;
        };
    }

    // ─── Corrección de fechaFin calculada con diaPago como anchor ───────────

    @Transactional(readOnly = true)
    public Map<String, Object> previsualizarCorreccionFechas() {
        LocalDate hoy = hoy();
        List<MembresiaCore> candidatas = membresiaCoreRepository.findConPagoOrigenParaCorreccionFechas();

        List<Map<String, Object>> errores   = new ArrayList<>();
        List<Map<String, Object>> correctas = new ArrayList<>();

        for (MembresiaCore mc : candidatas) {
            Pago pago = mc.getPagoOrigen();
            int meses = calcularMesesDesdeValorPago(pago.getValor(), pago.getMetodoPago());
            if (meses == 0) meses = calcularMesesSegunMonto(pago.getValor());
            if (meses == 0) continue;

            LocalDate fechaFinCorrecta = mc.getFechaInicio().plusMonths(meses);
            if (fechaFinCorrecta.equals(mc.getFechaFin())) {
                correctas.add(Map.of("idMembresiaCore", mc.getIdMembresiaCore()));
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idMembresiaCore",     mc.getIdMembresiaCore());
            item.put("idEstudiante",        mc.getEstudiante().getIdEstudiante());
            item.put("nombreEstudiante",    mc.getEstudiante().getNombreCompleto());
            item.put("esActiva",            mc.getEsActiva());
            item.put("estadoMembresia",     mc.getEstadoMembresia());
            item.put("fechaInicio",         mc.getFechaInicio());
            item.put("fechaFin_actual",     mc.getFechaFin());
            item.put("fechaFin_correcta",   fechaFinCorrecta);
            item.put("diferenciaDias",      mc.getFechaFin().until(fechaFinCorrecta).getDays()
                                            + mc.getFechaFin().until(fechaFinCorrecta).getMonths() * 30);
            item.put("meses",               meses);
            item.put("pago_valor",          pago.getValor());
            item.put("estadoQueQuedara",    fechaFinCorrecta.isAfter(hoy) ? "PAGADA" : "FINALIZADA");
            item.put("estadoPagoQueQuedara", fechaFinCorrecta.isAfter(hoy) ? "AL_DIA" : "EN_MORA");
            errores.add(item);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalEvaluadas",  candidatas.size());
        resultado.put("conErrorFecha",   errores.size());
        resultado.put("correctas",       correctas.size());
        resultado.put("detalle_errores", errores);
        return resultado;
    }

    @Transactional
    public Map<String, Object> corregirFechasCalculadasConDiaPago() {
        LocalDate hoy = hoy();
        List<MembresiaCore> candidatas = membresiaCoreRepository.findConPagoOrigenParaCorreccionFechas();

        int corregidas = 0;
        int sinCambio  = 0;
        int errores    = 0;

        for (MembresiaCore mc : candidatas) {
            try {
                Pago pago = mc.getPagoOrigen();
                int meses = calcularMesesDesdeValorPago(pago.getValor(), pago.getMetodoPago());
                if (meses == 0) meses = calcularMesesSegunMonto(pago.getValor());
                if (meses == 0) { sinCambio++; continue; }

                LocalDate fechaFinCorrecta = mc.getFechaInicio().plusMonths(meses);
                if (fechaFinCorrecta.equals(mc.getFechaFin())) { sinCambio++; continue; }

                mc.setFechaFin(fechaFinCorrecta);
                boolean vigente = fechaFinCorrecta.isAfter(hoy);
                mc.setEstadoMembresia(vigente ? EstadoMembresia.PAGADA : EstadoMembresia.FINALIZADA);
                mc.setMotivoCambio("FECHA_FIN_CORREGIDA");
                mc.setFechaUltimoCambio(ahora());
                membresiaCoreRepository.save(mc);

                if (Boolean.TRUE.equals(mc.getEsActiva())) {
                    Estudiante est = mc.getEstudiante();
                    est.setEstadoPago(vigente
                            ? Estudiante.EstadoPago.AL_DIA
                            : Estudiante.EstadoPago.EN_MORA);
                    estudianteRepository.save(est);
                }
                corregidas++;
            } catch (Exception e) {
                errores++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("job",        "CorregirFechasCalculadasConDiaPago");
        resultado.put("fecha",      hoy.toString());
        resultado.put("evaluadas",  candidatas.size());
        resultado.put("corregidas", corregidas);
        resultado.put("sinCambio",  sinCambio);
        resultado.put("errores",    errores);
        return resultado;
    }

    // ─── Corrección de pagos duplicados ──────────────────────────────────────

    @Transactional
    public Map<String, Object> corregirPagosDuplicados() {
        List<String> refsDuplicadas = pagoRepository.findReferenciasConDuplicados();

        int pagosEliminados      = 0;
        int membresiasCorregidas = 0;
        int errores              = 0;

        for (String ref : refsDuplicadas) {
            try {
                List<Pago> grupo = pagoRepository.findByReferenciaPagoOrderByIdPagoAsc(ref);
                if (grupo.size() < 2) continue;

                Pago canonical = grupo.get(0); // el más antiguo es el canónico
                List<Pago> duplicados = grupo.subList(1, grupo.size());

                for (Pago dup : duplicados) {
                    // Redirigir membresias_core que apunten al duplicado al canónico
                    List<MembresiaCore> afectadas = membresiaCoreRepository
                            .findByPagoOrigenIdPago(dup.getIdPago());
                    for (MembresiaCore mc : afectadas) {
                        mc.setPagoOrigen(canonical);
                        mc.setFechaUltimoCambio(ahora());
                        membresiaCoreRepository.save(mc);
                    }
                    pagoRepository.delete(dup);
                    pagosEliminados++;
                }

                // Recalcular la membresía migrada del estudiante afectado
                if (canonical.getEstudiante() != null) {
                    recalcularMembresiasMigradas(canonical.getEstudiante().getIdEstudiante());
                    membresiasCorregidas++;
                }

            } catch (Exception e) {
                errores++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("job", "CorregirPagosDuplicados");
        resultado.put("fecha", hoy().toString());
        resultado.put("referenciasConDuplicados", refsDuplicadas.size());
        resultado.put("pagosEliminados", pagosEliminados);
        resultado.put("membresiasCorregidas", membresiasCorregidas);
        resultado.put("errores", errores);
        return resultado;
    }

    private void recalcularMembresiasMigradas(Integer idEstudiante) {
        List<MembresiaCore> migradas = membresiaCoreRepository
                .findByEstudianteIdEstudianteAndMotivoCambio(idEstudiante, "MIGRADO_DESDE_PAGOS");
        if (migradas.isEmpty()) return;

        MembresiaCore mc = migradas.get(0);
        LocalDate fechaInicio = mc.getFechaInicio();
        if (fechaInicio == null) return;

        List<Pago> pagosRaw = pagoRepository.findPagadosByEstudianteOrderByFechaAsc(idEstudiante);
        List<Pago> pagos    = deduplicarPagos(pagosRaw);

        int totalMeses = 0;
        BigDecimal totalPagado = BigDecimal.ZERO;
        for (Pago p : pagos) {
            int m = calcularMesesDesdeValorPago(p.getValor(), p.getMetodoPago());
            if (m > 0) {
                totalMeses += m;
                if (p.getValor() != null) totalPagado = totalPagado.add(p.getValor());
            }
        }
        if (totalMeses == 0) return;

        int dia = fechaInicio.getDayOfMonth();
        LocalDate nuevaFechaFin = calcularFechaFin(fechaInicio, totalMeses, dia);
        BigDecimal valorMensual = totalPagado.divide(
                new BigDecimal(totalMeses), 2, java.math.RoundingMode.HALF_UP);
        LocalDate hoy = hoy();
        EstadoMembresia nuevoEstado = nuevaFechaFin.isBefore(hoy)
                ? EstadoMembresia.FINALIZADA : EstadoMembresia.PAGADA;

        mc.setFechaFin(nuevaFechaFin);
        mc.setValorMensual(valorMensual);
        mc.setEstadoMembresia(nuevoEstado);
        mc.setMotivoCambio("MIGRADO_CORREGIDO");
        mc.setFechaUltimoCambio(ahora());
        membresiaCoreRepository.save(mc);

        if (Boolean.TRUE.equals(mc.getEsActiva())) {
            Estudiante est = mc.getEstudiante();
            est.setEstadoPago(nuevoEstado == EstadoMembresia.PAGADA
                    ? Estudiante.EstadoPago.AL_DIA
                    : Estudiante.EstadoPago.EN_MORA);
            estudianteRepository.save(est);
        }
    }

    // ─── Reversión de membresías creadas indebidamente por job ───────────────

    @Transactional(readOnly = true)
    public Map<String, Object> previsualizarReversionIndebida(LocalDate fechaEjecucion) {
        LocalDate fecha = fechaEjecucion != null ? fechaEjecucion : hoy();
        java.time.LocalDateTime inicio = fecha.atStartOfDay();
        java.time.LocalDateTime fin    = fecha.plusDays(1).atStartOfDay();

        List<MembresiaCore> indebidas = membresiaCoreRepository
                .findActivasCreadasEnRangoConPagoAnteriorA(inicio, fin, fecha);

        List<Map<String, Object>> detalle = new ArrayList<>();

        for (MembresiaCore mc : indebidas) {
            Map<String, Object> item = new LinkedHashMap<>();
            Estudiante est = mc.getEstudiante();

            item.put("idMembresiaCore", mc.getIdMembresiaCore());
            item.put("idEstudiante", est.getIdEstudiante());
            item.put("nombreEstudiante", est.getNombreCompleto());
            item.put("estadoActualEstudiante", est.getEstadoPago());
            item.put("membresiaIndebida_fechaInicio", mc.getFechaInicio());
            item.put("membresiaIndebida_fechaFin", mc.getFechaFin());

            if (mc.getPagoOrigen() != null) {
                item.put("pagoOrigen_id", mc.getPagoOrigen().getIdPago());
                item.put("pagoOrigen_fecha", mc.getPagoOrigen().getFechaPago());
                item.put("pagoOrigen_referencia", mc.getPagoOrigen().getReferenciaPago());
                item.put("pagoOrigen_valor", mc.getPagoOrigen().getValor());
            }

            List<MembresiaCore> candidatas = membresiaCoreRepository
                    .findFinalizadasDesactivadasAntesDeId(est.getIdEstudiante(), mc.getIdMembresiaCore());
            String estadoQueQuedara;
            if (!candidatas.isEmpty()) {
                MembresiaCore anterior = candidatas.get(0);
                item.put("hayMembresiaAnterior", true);
                item.put("membresiaRestaurar_id", anterior.getIdMembresiaCore());
                item.put("membresiaRestaurar_fechaInicio", anterior.getFechaInicio());
                item.put("membresiaRestaurar_fechaFin", anterior.getFechaFin());
                estadoQueQuedara = (anterior.getFechaFin() != null && anterior.getFechaFin().isAfter(fecha))
                        ? Estudiante.EstadoPago.AL_DIA.name()
                        : Estudiante.EstadoPago.EN_MORA.name();
            } else {
                item.put("hayMembresiaAnterior", false);
                estadoQueQuedara = Estudiante.EstadoPago.EN_MORA.name();
            }

            item.put("estadoQueQuedara", estadoQueQuedara);
            detalle.add(item);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("fecha", fecha.toString());
        resultado.put("totalIndebidas", indebidas.size());
        resultado.put("detalle", detalle);
        return resultado;
    }

    @Transactional
    public Map<String, Object> revertirMembresiasCreadasIndebidamente(LocalDate fechaEjecucion) {
        LocalDate fecha = fechaEjecucion != null ? fechaEjecucion : hoy();
        java.time.LocalDateTime inicio = fecha.atStartOfDay();
        java.time.LocalDateTime fin    = fecha.plusDays(1).atStartOfDay();

        List<MembresiaCore> indebidas = membresiaCoreRepository
                .findActivasCreadasEnRangoConPagoAnteriorA(inicio, fin, fecha);

        int revertidas = 0;
        int sinAnterior = 0;
        int errores = 0;

        for (MembresiaCore mc : indebidas) {
            try {
                Estudiante est = mc.getEstudiante();

                // 1. Cancelar la membresía indebida
                mc.setEstadoMembresia(EstadoMembresia.CANCELADA);
                mc.setEsActiva(false);
                mc.setMotivoCambio("REVERTIDA_JOB_INDEBIDO");
                mc.setFechaUltimoCambio(ahora());
                membresiaCoreRepository.save(mc);

                // 2. Restaurar la FINALIZADA que fue desactivada
                List<MembresiaCore> candidatas = membresiaCoreRepository
                        .findFinalizadasDesactivadasAntesDeId(
                                est.getIdEstudiante(), mc.getIdMembresiaCore());
                if (!candidatas.isEmpty()) {
                    MembresiaCore anterior = candidatas.get(0);
                    // Si la membresía anterior aún no ha vencido, restaurarla como PAGADA
                    if (anterior.getFechaFin() != null && anterior.getFechaFin().isAfter(fecha)) {
                        anterior.setEstadoMembresia(EstadoMembresia.PAGADA);
                        est.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
                    } else {
                        est.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                    }
                    anterior.setEsActiva(true);
                    anterior.setFechaUltimoCambio(ahora());
                    membresiaCoreRepository.save(anterior);
                } else {
                    sinAnterior++;
                    est.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                }

                // 3. Actualizar estado del estudiante
                estudianteRepository.save(est);
                revertidas++;

            } catch (Exception e) {
                errores++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("job", "RevertirMembresiasCreadasIndebidamente");
        resultado.put("fecha", fecha.toString());
        resultado.put("indebidas", indebidas.size());
        resultado.put("revertidas", revertidas);
        resultado.put("sinMembresiaAnterior", sinAnterior);
        resultado.put("errores", errores);
        return resultado;
    }

    // ─── Job: corregir estudiantes AL_DIA sin membresía activa ──────────────

    @Transactional(readOnly = true)
    public Map<String, Object> previsualizarCorreccionAlDiaSinMembresia() {
        LocalDate hoy = hoy();
        List<Estudiante> afectados = estudianteRepository.findAlDiaSinMembresiaPagadaVigente();

        List<Map<String, Object>> seCreara   = new ArrayList<>();
        List<Map<String, Object>> sinPago    = new ArrayList<>();

        for (Estudiante est : afectados) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idEstudiante",     est.getIdEstudiante());
            item.put("nombreEstudiante", est.getNombreCompleto());
            item.put("fechaRegistro",    est.getFechaRegistro());
            item.put("estadoActual",     est.getEstadoPago());

            // Membresía actual (si existe alguna en historial, aunque no esté activa)
            List<MembresiaCore> historial = membresiaCoreRepository
                    .findByEstudianteIdEstudianteOrderByFechaInicioDesc(est.getIdEstudiante());
            if (!historial.isEmpty()) {
                MembresiaCore ultima = historial.get(0);
                Map<String, Object> membActual = new LinkedHashMap<>();
                membActual.put("id",          ultima.getIdMembresiaCore());
                membActual.put("estado",      ultima.getEstadoMembresia());
                membActual.put("fechaInicio", ultima.getFechaInicio());
                membActual.put("fechaFin",    ultima.getFechaFin());
                membActual.put("esActiva",    ultima.getEsActiva());
                item.put("membresiaActual", membActual);
            } else {
                item.put("membresiaActual", "SIN_MEMBRESIA");
            }

            List<Pago> pagos = pagoRepository.findPagadosSinMembresiaByEstudiante(est.getIdEstudiante());
            if (pagos.isEmpty() || membresiaCoreRepository.existsByPagoOrigenIdPago(pagos.get(0).getIdPago())) {
                item.put("accion", "OMITIR — sin pago huérfano disponible");
                sinPago.add(item);
                continue;
            }

            Pago p = pagos.get(0);
            int meses = calcularMesesDesdeValorPago(p.getValor(), p.getMetodoPago());
            if (meses == 0) meses = calcularMesesSegunMonto(p.getValor());
            if (meses == 0) meses = 1;

            // Calcular fechas exactas que se usarán
            LocalDate fechaInicio;
            String origenFechaInicio;
            if (historial.isEmpty()) {
                fechaInicio = est.getFechaRegistro() != null ? est.getFechaRegistro()
                        : (p.getFechaPago() != null ? p.getFechaPago() : hoy);
                origenFechaInicio = est.getFechaRegistro() != null ? "fechaRegistro" : "fechaPago";
            } else {
                MembresiaCore activaActual = historial.stream()
                        .filter(m -> Boolean.TRUE.equals(m.getEsActiva()))
                        .findFirst().orElse(historial.get(0));
                boolean esPendiente = activaActual.getTipoMembresia() == TipoMembresia.PENDIENTE_REGISTRO
                        || activaActual.getTipoMembresia() == TipoMembresia.ACUERDO_PAGO;
                LocalDate baseFinFinalizada = ultimaFechaFinFinalizada(est.getIdEstudiante());
                if (baseFinFinalizada != null && (!esPendiente || !baseFinFinalizada.isBefore(hoy.minusDays(15)))) {
                    fechaInicio = baseFinFinalizada;
                    origenFechaInicio = "finUltimaFinalizada";
                } else {
                    fechaInicio = p.getFechaPago() != null ? p.getFechaPago() : hoy;
                    origenFechaInicio = "fechaPago";
                }
            }
            Integer rawDp = est.getDiaPago();
            int diaPago = rawDp != null ? rawDp : fechaInicio.getDayOfMonth();
            LocalDate fechaFin = calcularFechaFin(fechaInicio, meses, diaPago);
            boolean vigente = fechaFin.isAfter(hoy);

            Map<String, Object> membNueva = new LinkedHashMap<>();
            membNueva.put("fechaInicio",       fechaInicio);
            membNueva.put("fechaFin",          fechaFin);
            membNueva.put("meses",             meses);
            membNueva.put("estado",            vigente ? "PAGADA" : "FINALIZADA");
            membNueva.put("origenFechaInicio", origenFechaInicio);

            item.put("accion",       "CREAR");
            item.put("pago_id",      p.getIdPago());
            item.put("pago_fecha",   p.getFechaPago());
            item.put("pago_valor",   p.getValor());
            item.put("pago_metodo",  p.getMetodoPago());
            item.put("membresiaQueQuedara", membNueva);
            item.put("estadoQueQuedara", vigente ? "AL_DIA" : "EN_MORA");
            seCreara.add(item);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalAfectados",    afectados.size());
        resultado.put("seCrearan",         seCreara.size());
        resultado.put("seOmitiran",        sinPago.size());
        resultado.put("detalle_crear",     seCreara);
        resultado.put("detalle_omitir",    sinPago);
        return resultado;
    }

    @Transactional
    public Map<String, Object> ejecutarJobCorregirAlDiaSinMembresia() {
        List<Estudiante> afectados = estudianteRepository.findAlDiaSinMembresiaPagadaVigente();

        int creadas  = 0;
        int sinPago  = 0;
        int errores  = 0;

        for (Estudiante est : afectados) {
            try {
                List<Pago> pagos = pagoRepository.findPagadosSinMembresiaByEstudiante(est.getIdEstudiante());
                if (pagos.isEmpty()) {
                    sinPago++;
                    continue;
                }
                Pago pago = pagos.get(0);
                if (membresiaCoreRepository.existsByPagoOrigenIdPago(pago.getIdPago())) {
                    sinPago++;
                    continue;
                }
                crearMembresiaCorrigiendoAlDia(est, pago);
                creadas++;
            } catch (Exception e) {
                errores++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("job", "CorregirAlDiaSinMembresia");
        resultado.put("fecha", hoy().toString());
        resultado.put("afectados", afectados.size());
        resultado.put("creadas", creadas);
        resultado.put("sinPago", sinPago);
        resultado.put("errores", errores);
        return resultado;
    }

    private void crearMembresiaCorrigiendoAlDia(Estudiante est, Pago pago) {
        List<MembresiaCore> historial = membresiaCoreRepository
                .findByEstudianteIdEstudianteOrderByFechaInicioDesc(est.getIdEstudiante());

        if (historial.isEmpty()) {
            // Sin historial previo: usar fechaRegistro como fechaInicio
            LocalDate hoy = hoy();
            int meses = calcularMesesDesdeValorPago(pago.getValor(), pago.getMetodoPago());
            if (meses == 0) meses = calcularMesesSegunMonto(pago.getValor());
            if (meses == 0) meses = 1;

            LocalDate fechaInicio = est.getFechaRegistro() != null ? est.getFechaRegistro()
                    : (pago.getFechaPago() != null ? pago.getFechaPago() : hoy);

            // Para primer registro siempre usar el día de fechaInicio como anchor,
            // ignorando diaPago previo que puede ser de otro contexto y dar coberturas injustas
            int diaPago = fechaInicio.getDayOfMonth();
            LocalDate fechaFin = calcularFechaFin(fechaInicio, meses, diaPago);

            boolean vigente = fechaFin.isAfter(hoy);
            EstadoMembresia estado = vigente ? EstadoMembresia.PAGADA : EstadoMembresia.FINALIZADA;
            TipoMembresia tipo = pago.getMetodoPago() == Pago.MetodoPago.ONLINE
                    ? TipoMembresia.ONLINE : TipoMembresia.EFECTIVO;

            MembresiaCore mc = buildMembresia(est, pago, tipo, fechaInicio, fechaFin, estado, true);
            membresiaCoreRepository.save(mc);

            est.setDiaPago(diaPago);
            est.setEstadoPago(vigente ? Estudiante.EstadoPago.AL_DIA : Estudiante.EstadoPago.EN_MORA);
            estudianteRepository.save(est);
        } else {
            // Tiene historial: seguir las reglas normales
            TipoMembresia tipo = pago.getMetodoPago() == Pago.MetodoPago.ONLINE
                    ? TipoMembresia.ONLINE : TipoMembresia.EFECTIVO;
            crearMembresiaParaPago(est, pago, tipo);
        }
    }

    // ─── Registro de pago manual con rango de fechas ────────────────────────

    @Transactional
    public Map<String, Object> crearMembresiaManualConFechas(Estudiante estudiante, Pago pago,
                                                              LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDate hoy = hoy();

        if (fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException(
                    "fechaInicio (" + fechaInicio + ") no puede ser posterior a fechaFin (" + fechaFin + ")");
        }

        TipoMembresia tipo = pago.getMetodoPago() == Pago.MetodoPago.ONLINE
                ? TipoMembresia.ONLINE : TipoMembresia.EFECTIVO;

        Optional<MembresiaCore> activaOpt =
                membresiaCoreRepository.findByEstudianteIdEstudianteAndEsActivaTrue(estudiante.getIdEstudiante());

        MembresiaCore membresia;

        if (activaOpt.isPresent()) {
            MembresiaCore activa = activaOpt.get();

            // Caso 1: membresía PAGADA activa
            if (activa.getEstadoMembresia() == EstadoMembresia.PAGADA) {

                // Solapamiento → error
                boolean solapa = !fechaInicio.isAfter(activa.getFechaFin())
                        && !fechaFin.isBefore(activa.getFechaInicio());
                if (solapa) {
                    throw new IllegalStateException(
                            "Ya existe una membresía PAGADA activa del " + activa.getFechaInicio()
                            + " al " + activa.getFechaFin() + ". No se puede registrar un pago en esas fechas.");
                }

                // Pago anticipado: inicio después del fin de la activa
                if (fechaInicio.isAfter(activa.getFechaFin())) {
                    membresia = buildMembresiaManual(estudiante, pago, tipo, fechaInicio, fechaFin,
                            EstadoMembresia.PAGADA, false);
                    membresiaCoreRepository.save(membresia);
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("tipo", "ANTICIPADO");
                    r.put("membresia", MembresiaCoreDTO.from(membresia));
                    return r;
                }
            }

            // Caso 2: membresía PENDIENTE_REGISTRO activa → finalizarla
            if (activa.getTipoMembresia() == TipoMembresia.PENDIENTE_REGISTRO) {
                activa.setEstadoMembresia(EstadoMembresia.FINALIZADA);
                activa.setEsActiva(false);
                activa.setMotivoCambio("REEMPLAZADA_POR_PAGO_MANUAL");
                activa.setFechaUltimoCambio(ahora());
                membresiaCoreRepository.save(activa);
            } else {
                desactivarActual(estudiante.getIdEstudiante());
            }
        }

        // Crear membresía con las fechas dadas
        boolean vigente = fechaFin.isAfter(hoy);
        EstadoMembresia estado = vigente ? EstadoMembresia.PAGADA : EstadoMembresia.FINALIZADA;
        membresia = buildMembresiaManual(estudiante, pago, tipo, fechaInicio, fechaFin, estado, true);
        membresiaCoreRepository.save(membresia);

        if (vigente) {
            estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
        } else {
            estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
        }
        if (!Boolean.TRUE.equals(estudiante.getEstado())) estudiante.setEstado(true);
        estudianteRepository.save(estudiante);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("tipo", "NUEVO");
        r.put("membresia", MembresiaCoreDTO.from(membresia));
        return r;
    }

    private MembresiaCore buildMembresiaManual(Estudiante est, Pago pago, TipoMembresia tipo,
                                                LocalDate inicio, LocalDate fin,
                                                EstadoMembresia estado, boolean activa) {
        long meses = java.time.temporal.ChronoUnit.MONTHS.between(inicio, fin);
        if (meses < 1) meses = 1;
        BigDecimal valorMensual = pago.getValor() != null
                ? pago.getValor().divide(new BigDecimal(meses), 2, java.math.RoundingMode.HALF_UP)
                : null;

        MembresiaCore m = new MembresiaCore();
        m.setEstudiante(est);
        m.setPagoOrigen(pago);
        m.setTipoMembresia(tipo);
        m.setEstadoMembresia(estado);
        m.setFechaInicio(inicio);
        m.setFechaFin(fin);
        m.setValorMensual(valorMensual);
        m.setEsActiva(activa);
        m.setFechaCreacion(ahora());
        m.setFechaUltimoCambio(ahora());
        m.setMotivoCambio("PAGO_CONFIRMADO");
        return m;
    }

    // ─── Job: recuperar membresías faltantes por bug de webhook ─────────────

    @Transactional(readOnly = true)
    public Map<String, Object> previsualizarRecuperarMembresiasFaltantes() {
        LocalDate hoy = hoy();
        List<Pago> huerfanos = pagoRepository.findPagadosOnlineSinMembresia();

        Map<Integer, Pago> porEstudiante = new LinkedHashMap<>();
        for (Pago p : huerfanos) {
            porEstudiante.putIfAbsent(p.getEstudiante().getIdEstudiante(), p);
        }

        List<Map<String, Object>> seCreara = new ArrayList<>();

        for (Pago pago : porEstudiante.values()) {
            Estudiante est = pago.getEstudiante();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idEstudiante",     est.getIdEstudiante());
            item.put("nombreEstudiante", est.getNombreCompleto());
            item.put("estadoActual",     est.getEstadoPago());
            item.put("pago_id",          pago.getIdPago());
            item.put("pago_fecha",       pago.getFechaPago());
            item.put("pago_valor",       pago.getValor());
            item.put("pago_metodo",      pago.getMetodoPago());

            Optional<MembresiaCore> activaOpt = membresiaCoreRepository
                    .findByEstudianteIdEstudianteAndEsActivaTrue(est.getIdEstudiante());
            if (activaOpt.isPresent()) {
                MembresiaCore activa = activaOpt.get();
                Map<String, Object> membActual = new LinkedHashMap<>();
                membActual.put("id",          activa.getIdMembresiaCore());
                membActual.put("estado",      activa.getEstadoMembresia());
                membActual.put("fechaInicio", activa.getFechaInicio());
                membActual.put("fechaFin",    activa.getFechaFin());
                membActual.put("esActiva",    activa.getEsActiva());
                item.put("membresiaActual", membActual);
            } else {
                item.put("membresiaActual", "SIN_MEMBRESIA");
            }

            int meses = calcularMesesDesdeValorPago(pago.getValor(), pago.getMetodoPago());
            if (meses == 0) meses = calcularMesesSegunMonto(pago.getValor());
            if (meses == 0) meses = 1;

            LocalDate fechaInicio;
            String origenFecha;

            if (activaOpt.isPresent()) {
                MembresiaCore activa = activaOpt.get();
                boolean vigente = activa.getEstadoMembresia() == EstadoMembresia.PAGADA
                        && !activa.getFechaFin().isBefore(hoy);
                if (vigente) {
                    fechaInicio = activa.getFechaFin();
                    origenFecha = "finVigente_anticipado";
                } else if (activa.getTipoMembresia() == TipoMembresia.PENDIENTE_REGISTRO
                        && activa.getFechaInicio() != null
                        && activa.getFechaLimiteGracia() != null
                        && !activa.getFechaLimiteGracia().isBefore(hoy)) {
                    fechaInicio = activa.getFechaInicio();
                    origenFecha = "pendienteRegistro_gracia";
                } else {
                    boolean esPendiente = activa.getTipoMembresia() == TipoMembresia.PENDIENTE_REGISTRO
                            || activa.getTipoMembresia() == TipoMembresia.ACUERDO_PAGO;
                    LocalDate base = ultimaFechaFinFinalizada(est.getIdEstudiante());
                    if (base != null && (!esPendiente || !base.isBefore(hoy.minusDays(15)))) {
                        fechaInicio = base;
                        origenFecha = "finUltimaFinalizada";
                    } else {
                        fechaInicio = pago.getFechaPago() != null ? pago.getFechaPago() : hoy;
                        origenFecha = "fechaPago";
                    }
                }
            } else {
                fechaInicio = pago.getFechaPago() != null ? pago.getFechaPago() : hoy;
                origenFecha = "fechaPago_sinHistorial";
            }

            Integer rawDp = est.getDiaPago();
            int diaPago = rawDp != null ? rawDp : fechaInicio.getDayOfMonth();
            LocalDate fechaFin = calcularFechaFin(fechaInicio, meses, diaPago);
            boolean saldada = !fechaFin.isBefore(hoy);

            Map<String, Object> membNueva = new LinkedHashMap<>();
            membNueva.put("fechaInicio",       fechaInicio);
            membNueva.put("fechaFin",          fechaFin);
            membNueva.put("meses",             meses);
            membNueva.put("estado",            saldada ? "PAGADA" : "FINALIZADA");
            membNueva.put("origenFechaInicio", origenFecha);

            item.put("accion",              "CREAR");
            item.put("membresiaQueQuedara", membNueva);
            item.put("estadoQueQuedara",    saldada ? "AL_DIA" : "EN_MORA");
            seCreara.add(item);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("pagosHuerfanos",       huerfanos.size());
        resultado.put("estudiantesAfectados", porEstudiante.size());
        resultado.put("seCrearan",            seCreara.size());
        resultado.put("detalle_crear",        seCreara);
        return resultado;
    }

    @Transactional
    public Map<String, Object> ejecutarJobRecuperarMembresiasFaltantes() {
        List<Pago> huerfanos = pagoRepository.findPagadosOnlineSinMembresia();

        // Un pago por estudiante: el más reciente (la query ya viene DESC)
        Map<Integer, Pago> porEstudiante = new LinkedHashMap<>();
        for (Pago p : huerfanos) {
            porEstudiante.putIfAbsent(p.getEstudiante().getIdEstudiante(), p);
        }

        int creadas  = 0;
        int omitidas = 0;
        int errores  = 0;

        for (Pago pago : porEstudiante.values()) {
            try {
                crearMembresiaParaPago(pago.getEstudiante(), pago, TipoMembresia.ONLINE);
                creadas++;
            } catch (IllegalStateException e) {
                // pagoOrigen ya vinculado — condición de carrera o duplicado
                omitidas++;
            } catch (Exception e) {
                errores++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("job", "RecuperarMembresiasFaltantes");
        resultado.put("fecha", hoy().toString());
        resultado.put("pagosHuerfanos", huerfanos.size());
        resultado.put("estudiantesEvaluados", porEstudiante.size());
        resultado.put("creadas", creadas);
        resultado.put("omitidas", omitidas);
        resultado.put("errores", errores);
        return resultado;
    }

    // ─── Migrar acuerdos de pago faltantes (suplemento a migración inicial) ──

    @Transactional
    public Map<String, Object> migrarAcuerdosPendientes() {
        LocalDate hoy = hoy();
        List<Estudiante> compromisos = estudianteRepository
                .findByEstadoPago(Estudiante.EstadoPago.COMPROMISO_PAGO);

        int creados  = 0;
        int omitidos = 0;
        int errores  = 0;

        for (Estudiante est : compromisos) {
            try {
                // Si ya tiene un ACUERDO_PAGO activo no crear otro
                Optional<MembresiaCore> acuerdoExistente =
                        membresiaCoreRepository.findFirstByEstudianteIdEstudianteAndTipoMembresiaAndEstadoMembresia(
                                est.getIdEstudiante(), TipoMembresia.ACUERDO_PAGO, EstadoMembresia.PENDIENTE_PAGO);
                if (acuerdoExistente.isPresent()) {
                    omitidos++;
                    continue;
                }

                Optional<MembresiaCore> activaOpt =
                        membresiaCoreRepository.findByEstudianteIdEstudianteAndEsActivaTrue(
                                est.getIdEstudiante());

                LocalDate inicioAcuerdo;
                BigDecimal valorMensual = null;
                OrigenAcuerdo origen;
                Integer dpAcuerdo = est.getDiaPago();
                int diaAcuerdo = dpAcuerdo != null ? dpAcuerdo : hoy.getDayOfMonth();

                if (activaOpt.isPresent()) {
                    MembresiaCore activa = activaOpt.get();
                    inicioAcuerdo = activa.getFechaFin() != null ? activa.getFechaFin() : hoy;
                    valorMensual  = activa.getValorMensual();
                    origen        = OrigenAcuerdo.DESDE_MORA;
                    activa.setEsActiva(false);
                    activa.setFechaUltimoCambio(ahora());
                    membresiaCoreRepository.save(activa);
                } else {
                    inicioAcuerdo = est.getFechaRegistro() != null ? est.getFechaRegistro() : hoy;
                    origen        = OrigenAcuerdo.DESDE_PENDIENTE;
                }

                LocalDate finAcuerdo = calcularFechaFin(inicioAcuerdo, 1, diaAcuerdo);
                membresiaCoreRepository.save(
                        buildAcuerdoMigracion(est, inicioAcuerdo, finAcuerdo, valorMensual, origen));
                creados++;

            } catch (Exception e) {
                errores++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("job", "MigrarAcuerdosPendientes");
        resultado.put("fecha", hoy.toString());
        resultado.put("creados", creados);
        resultado.put("omitidos", omitidos);
        resultado.put("errores", errores);
        return resultado;
    }

    private MembresiaCore buildAcuerdoMigracion(Estudiante est, LocalDate inicio, LocalDate fin,
                                                  BigDecimal valorMensual, OrigenAcuerdo origen) {
        MembresiaCore acuerdo = new MembresiaCore();
        acuerdo.setEstudiante(est);
        acuerdo.setPagoOrigen(null);
        acuerdo.setTipoMembresia(TipoMembresia.ACUERDO_PAGO);
        acuerdo.setEstadoMembresia(EstadoMembresia.PENDIENTE_PAGO);
        acuerdo.setFechaInicio(inicio);
        acuerdo.setFechaFin(fin);
        acuerdo.setValorMensual(valorMensual);
        acuerdo.setObservacion(est.getObservacionPago());
        acuerdo.setFechaLimiteCompromiso(est.getFechaLimiteCompromiso());
        acuerdo.setOrigenAcuerdo(origen);
        acuerdo.setEsActiva(true);
        acuerdo.setFechaCreacion(ahora());
        acuerdo.setFechaUltimoCambio(ahora());
        acuerdo.setMotivoCambio("MIGRADO_ACUERDO_PAGO");
        return acuerdo;
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

                // Pagos PAGADOS ordenados ASC, deduplicados por referenciaPago
                List<Pago> pagosRaw = pagoRepository.findPagadosByEstudianteOrderByFechaAsc(idEst);
                List<Pago> pagos = deduplicarPagos(pagosRaw);

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
                    // Sin pagos pero con compromiso: crear ACUERDO_PAGO desde cero
                    if (est.getEstadoPago() == Estudiante.EstadoPago.COMPROMISO_PAGO) {
                        LocalDate inicio = est.getFechaRegistro() != null ? est.getFechaRegistro() : hoy;
                        Integer dpReg = est.getDiaPago();
                        int d = (dpReg != null && dpReg > 0) ? dpReg : inicio.getDayOfMonth();
                        LocalDate fin = calcularFechaFin(inicio, 1, d);
                        membresiaCoreRepository.save(
                                buildAcuerdoMigracion(est, inicio, fin, null, OrigenAcuerdo.DESDE_PENDIENTE));
                        migradas++;
                    } else {
                        omitidas++;
                    }
                    continue;
                }

                // fechaInicio: día (diaPago o 3) del mes del pago más antiguo
                LocalDate fechaPagoViejo = pagoMasViejo.getFechaPago() != null
                        ? pagoMasViejo.getFechaPago() : hoy;
                Integer rawDia = est.getDiaPago();
                int dia = (rawDia != null && rawDia > 0) ? rawDia : 3;
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

                // Si tiene compromiso de pago: la membresía migrada queda inactiva
                // y se crea el ACUERDO_PAGO vigente encima
                if (est.getEstadoPago() == Estudiante.EstadoPago.COMPROMISO_PAGO) {
                    mc.setEsActiva(false);
                    mc.setFechaUltimoCambio(ahora());
                    membresiaCoreRepository.save(mc);
                    LocalDate inicioAcuerdo = fechaFin;
                    LocalDate finAcuerdo    = calcularFechaFin(inicioAcuerdo, 1, dia);
                    membresiaCoreRepository.save(
                            buildAcuerdoMigracion(est, inicioAcuerdo, finAcuerdo,
                                    valorMensual, OrigenAcuerdo.DESDE_MORA));
                }

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
