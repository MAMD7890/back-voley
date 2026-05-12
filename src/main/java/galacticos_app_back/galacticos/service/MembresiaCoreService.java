package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.CambioEstadoPagoDTO;
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
        int meses = calcularMesesSegunMonto(pago.getValor());

        List<TipoMembresia> tiposConvertibles = Arrays.asList(
                TipoMembresia.MORA, TipoMembresia.PENDIENTE_REGISTRO, TipoMembresia.ACUERDO_PAGO);
        List<EstadoMembresia> estadosConvertibles = Arrays.asList(
                EstadoMembresia.EN_MORA, EstadoMembresia.PENDIENTE_PAGO);

        if (membresiaCoreRepository.existsByPagoOrigenIdPago(pago.getIdPago())) {
            throw new IllegalStateException(
                    "El pago " + pago.getIdPago() + " ya está vinculado a una membresía existente");
        }

        List<MembresiaCore> convertibles = membresiaCoreRepository.findConvertibles(
                estudiante.getIdEstudiante(), tiposConvertibles, estadosConvertibles);

        MembresiaCore membresia;

        if (!convertibles.isEmpty()) {
            // Paso 2a — convertir registro existente
            membresia = convertibles.get(0);
            LocalDate fechaInicio = membresia.getFechaInicio();
            Integer diaPagoBox = estudiante.getDiaPago();
            int diaPago = diaPagoBox != null ? diaPagoBox : fechaInicio.getDayOfMonth();
            LocalDate fechaFin = calcularFechaFin(fechaInicio, meses, diaPago);

            desactivarActual(estudiante.getIdEstudiante());
            membresia.setEstadoMembresia(EstadoMembresia.PAGADA);
            membresia.setTipoMembresia(tipo);
            membresia.setFechaFin(fechaFin);
            membresia.setPagoOrigen(pago);
            membresia.setFechaLimiteGracia(null);
            membresia.setFechaLimiteCompromiso(null);
            membresia.setEsActiva(true);
            membresia.setFechaUltimoCambio(ahora());
            membresia.setMotivoCambio("PAGO_CONFIRMADO");

        } else {
            // Paso 2b — nueva membresía (primer pago o pago anticipado)
            LocalDate fechaInicio;
            int diaPago;

            // Si hay membresía vigente, el nuevo período empieza al día siguiente del fin actual
            List<MembresiaCore> vigentes = membresiaCoreRepository.findVigentesDeEstudiante(
                    estudiante.getIdEstudiante(), hoy);
            boolean esPagoAnticipado = !vigentes.isEmpty();
            if (esPagoAnticipado) {
                // La nueva membresía empieza el mismo día que termina la vigente más lejana
                fechaInicio = vigentes.get(0).getFechaFin();
            } else {
                fechaInicio = pago.getFechaPago() != null ? pago.getFechaPago() : hoy;
            }

            diaPago = fechaInicio.getDayOfMonth();
            LocalDate fechaFin = calcularFechaFin(fechaInicio, meses, diaPago);

            membresia = new MembresiaCore();
            membresia.setEstudiante(estudiante);
            membresia.setEquipo(null);
            membresia.setPlan(null);
            membresia.setPagoOrigen(pago);
            membresia.setTipoMembresia(tipo);
            membresia.setEstadoMembresia(EstadoMembresia.PAGADA);
            membresia.setFechaInicio(fechaInicio);
            membresia.setFechaFin(fechaFin);
            membresia.setValorMensual(pago.getValor() != null
                    ? pago.getValor().divide(new BigDecimal(meses), 2, java.math.RoundingMode.HALF_UP)
                    : null);
            membresia.setFechaCreacion(ahora());
            membresia.setFechaUltimoCambio(ahora());
            // Pago anticipado: la activa sigue siendo la vigente; si no hay vigente, esta es la activa
            membresia.setEsActiva(!esPagoAnticipado);
            if (!esPagoAnticipado) desactivarActual(estudiante.getIdEstudiante());

            // Actualizar billing anchor
            estudiante.setDiaPago(diaPago);
        }

        // Paso 3 — activar estudiante
        if (!Boolean.TRUE.equals(estudiante.getEstado())) {
            estudiante.setEstado(true);
        }
        estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
        estudiante.setCambiadoManualmente(false);
        estudianteRepository.save(estudiante);

        return membresiaCoreRepository.save(membresia);
    }

    // ─── Crear acuerdo de pago ────────────────────────────────────────────────

    @Transactional
    public MembresiaCore crearAcuerdoPago(Integer idEstudiante, LocalDate fechaLimiteCompromiso,
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

        return membresiaCoreRepository.save(acuerdo);
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

        // Crear registro MembresiaCore solo si el nuevo estado es COMPROMISO_PAGO
        if (cambioDTO.getNuevoEstado() == Estudiante.EstadoPago.COMPROMISO_PAGO) {
            try {
                List<TipoMembresia> tiposReemplazables = Arrays.asList(
                        TipoMembresia.PENDIENTE_REGISTRO, TipoMembresia.MORA);
                List<EstadoMembresia> estadosReemplazables = Arrays.asList(
                        EstadoMembresia.PENDIENTE_PAGO, EstadoMembresia.EN_MORA);

                List<MembresiaCore> reemplazables = membresiaCoreRepository.findConvertibles(
                        idEstudiante, tiposReemplazables, estadosReemplazables);

                if (!reemplazables.isEmpty()) {
                    MembresiaCore reemplazable = reemplazables.get(0);
                    OrigenAcuerdo origen = reemplazable.getTipoMembresia() == TipoMembresia.PENDIENTE_REGISTRO
                            ? OrigenAcuerdo.DESDE_PENDIENTE : OrigenAcuerdo.DESDE_MORA;

                    reemplazable.setEstadoMembresia(EstadoMembresia.CANCELADA);
                    reemplazable.setEsActiva(false);
                    reemplazable.setMotivoCambio("REEMPLAZADO_POR_ACUERDO");
                    reemplazable.setFechaUltimoCambio(ahora());
                    membresiaCoreRepository.save(reemplazable);

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
                    acuerdo.setObservacion(cambioDTO.getObservacion());
                    acuerdo.setFechaLimiteCompromiso(cambioDTO.getFechaLimiteCompromiso());
                    acuerdo.setOrigenAcuerdo(origen);
                    acuerdo.setEsActiva(true);
                    acuerdo.setFechaCreacion(ahora());
                    acuerdo.setFechaUltimoCambio(ahora());
                    membresiaCoreRepository.save(acuerdo);
                }
                // Si no hay convertible (ej: estudiante AL_DIA o sin membresía core aún),
                // solo se cambia el estado del estudiante — sin error.
            } catch (Exception e) {
                // No romper el flujo principal si falla la creación del core
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
    public MembresiaCore cambiarFechas(Integer idMembresiaCore,
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

            return membresiaCoreRepository.save(mc);
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
            return mc;
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
        return membresiaCoreRepository.save(mc);
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

        // Sin PAGADA vigente → crear nueva EN_MORA desde hoy
        Integer rawDiaPago = estudiante.getDiaPago();
        int diaPago = rawDiaPago != null ? rawDiaPago : hoy.getDayOfMonth();

        desactivarActual(idEstudiante);

        MembresiaCore mora = new MembresiaCore();
        mora.setEstudiante(estudiante);
        mora.setTipoMembresia(TipoMembresia.MORA);
        mora.setEstadoMembresia(EstadoMembresia.EN_MORA);
        mora.setFechaInicio(hoy);
        mora.setFechaFin(calcularFechaFin(hoy, 1, diaPago));
        mora.setFechaLimiteGracia(hoy.plusDays(15));
        mora.setPagoOrigen(null);
        mora.setEsActiva(true);
        mora.setFechaCreacion(ahora());
        mora.setFechaUltimoCambio(ahora());
        mora.setMotivoCambio("REACTIVACION_MANUAL");
        membresiaCoreRepository.save(mora);

        estudiante.setEstado(true);
        estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
        estudianteRepository.save(estudiante);
        return Estudiante.EstadoPago.EN_MORA;
    }

    // ─── Historial de membresías ──────────────────────────────────────────────

    public List<MembresiaCore> obtenerHistorico(Integer idEstudiante) {
        return membresiaCoreRepository
                .findByEstudianteIdEstudianteOrderByFechaInicioDesc(idEstudiante);
    }

    public Optional<MembresiaCore> obtenerMembresiaActiva(Integer idEstudiante) {
        return membresiaCoreRepository.findByEstudianteIdEstudianteAndEsActivaTrue(idEstudiante);
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

                // Marcar membresía vencida como FINALIZADA
                mc.setEstadoMembresia(EstadoMembresia.FINALIZADA);
                mc.setEsActiva(false);
                mc.setMotivoCambio("VENCIDA_SIN_PAGO_SIGUIENTE");
                mc.setFechaUltimoCambio(ahora());
                membresiaCoreRepository.save(mc);

                // Crear membresía MORA
                Integer rawDiaPago = estudiante.getDiaPago();
                int diaPago = rawDiaPago != null ? rawDiaPago : mc.getFechaFin().getDayOfMonth();
                LocalDate inicioMora = mc.getFechaFin();
                LocalDate finMora = calcularFechaFin(inicioMora, 1, diaPago);
                LocalDate limiteGracia = inicioMora.plusDays(15);

                MembresiaCore mora = new MembresiaCore();
                mora.setEstudiante(estudiante);
                mora.setEquipo(mc.getEquipo());
                mora.setPlan(mc.getPlan());
                mora.setTipoMembresia(TipoMembresia.MORA);
                mora.setEstadoMembresia(EstadoMembresia.EN_MORA);
                mora.setFechaInicio(inicioMora);
                mora.setFechaFin(finMora);
                mora.setFechaLimiteGracia(limiteGracia);
                mora.setPagoOrigen(null);
                mora.setEsActiva(true);
                mora.setFechaCreacion(ahora());
                mora.setFechaUltimoCambio(ahora());
                membresiaCoreRepository.save(mora);

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
        resultado.put("morasCreadas", procesadas);
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
        int morasCreadas = 0;
        int errores = 0;

        for (MembresiaCore mc : acuerdosVencidos) {
            try {
                mc.setEstadoMembresia(EstadoMembresia.CANCELADA);
                mc.setEsActiva(false);
                mc.setMotivoCambio("ACUERDO_VENCIDO");
                mc.setFechaUltimoCambio(ahora());
                membresiaCoreRepository.save(mc);

                Estudiante estudiante = mc.getEstudiante();

                if (mc.getOrigenAcuerdo() == OrigenAcuerdo.DESDE_PENDIENTE) {
                    // Nunca tuvo acceso — inactivar, estadoPago queda como EN_MORA (no SIN_MEMBRESIA)
                    estudiante.setEstado(false);
                    estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                } else {
                    // DESDE_MORA — tuvo acceso, darle 15 días de gracia nueva
                    Integer rawDiaPago = estudiante.getDiaPago();
                    int diaPago = rawDiaPago != null ? rawDiaPago : hoy.getDayOfMonth();
                    LocalDate finMora = calcularFechaFin(hoy, 1, diaPago);

                    MembresiaCore mora = new MembresiaCore();
                    mora.setEstudiante(estudiante);
                    mora.setEquipo(mc.getEquipo());
                    mora.setPlan(mc.getPlan());
                    mora.setTipoMembresia(TipoMembresia.MORA);
                    mora.setEstadoMembresia(EstadoMembresia.EN_MORA);
                    mora.setFechaInicio(hoy);
                    mora.setFechaFin(finMora);
                    mora.setFechaLimiteGracia(hoy.plusDays(15));
                    mora.setPagoOrigen(null);
                    mora.setEsActiva(true);
                    mora.setFechaCreacion(ahora());
                    mora.setFechaUltimoCambio(ahora());
                    membresiaCoreRepository.save(mora);

                    estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                    morasCreadas++;
                }

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
        resultado.put("morasCreadas", morasCreadas);
        resultado.put("errores", errores);
        return resultado;
    }

    // ─── Migración ────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> migrarMembresiasExistentes() {
        LocalDate hoy = hoy();
        LocalDate limiteHistorial = hoy.minusMonths(3);

        List<Membresia> todas = membresiaRepository.findAll();

        int migradas = 0;
        int morasCreadas = 0;
        int omitidas = 0;
        int errores = 0;

        // Por estudiante: fechaInicio más reciente y estado final a aplicar
        Map<Integer, LocalDate> ultimaFechaInicioPorEstudiante = new HashMap<>();
        // EN_MORA tiene prioridad sobre AL_DIA
        Map<Integer, Estudiante.EstadoPago> estadoFinalPorEstudiante = new HashMap<>();
        // El id de la membresiaCore que debe quedar como esActiva por estudiante
        Map<Integer, Integer> activaIdPorEstudiante = new HashMap<>();

        for (Membresia m : todas) {
            try {
                // Solo migrar si tiene pago real y fechaFin válida dentro del rango
                if (m.getEstudiante() == null || m.getPagoOrigen() == null) {
                    omitidas++;
                    continue;
                }
                LocalDate fechaFin = m.getFechaFin();
                if (fechaFin == null || fechaFin.isBefore(limiteHistorial)) {
                    omitidas++;
                    continue;
                }

                Integer idEstudiante = m.getEstudiante().getIdEstudiante();
                int diaPago = fechaFin.getDayOfMonth();

                boolean vencida = fechaFin.isBefore(hoy);
                long diasVencida = vencida ? ChronoUnit.DAYS.between(fechaFin, hoy) : 0;

                // Crear registro PAGADA o FINALIZADA
                EstadoMembresia estadoPrincipal = vencida ? EstadoMembresia.FINALIZADA : EstadoMembresia.PAGADA;

                MembresiaCore mc = new MembresiaCore();
                mc.setEstudiante(m.getEstudiante());
                mc.setEquipo(m.getEquipo());
                mc.setPlan(m.getPlan());
                mc.setPagoOrigen(m.getPagoOrigen());
                mc.setTipoMembresia(TipoMembresia.EFECTIVO);
                mc.setEstadoMembresia(estadoPrincipal);
                mc.setFechaInicio(m.getFechaInicio());
                mc.setFechaFin(fechaFin);
                mc.setValorMensual(m.getValorMensual());
                mc.setEsActiva(false); // se corrige al final por estudiante
                mc.setFechaCreacion(m.getFechaCreacion() != null ? m.getFechaCreacion() : ahora());
                mc.setFechaUltimoCambio(ahora());
                mc.setMotivoCambio("MIGRADO_DESDE_MEMBRESIA");
                MembresiaCore mcGuardado = membresiaCoreRepository.save(mc);
                migradas++;

                if (!vencida) {
                    // PAGADA vigente → esta es la activa
                    activaIdPorEstudiante.put(idEstudiante, mcGuardado.getIdMembresiaCore());
                    estadoFinalPorEstudiante.put(idEstudiante, Estudiante.EstadoPago.AL_DIA);
                } else if (diasVencida <= 15) {
                    // Venció hace ≤ 15 días → crear MORA, esa es la activa
                    LocalDate inicioMora = fechaFin; // mismo día del vencimiento
                    LocalDate finMora = calcularFechaFin(inicioMora, 1, diaPago);

                    MembresiaCore mora = new MembresiaCore();
                    mora.setEstudiante(m.getEstudiante());
                    mora.setEquipo(m.getEquipo());
                    mora.setPlan(m.getPlan());
                    mora.setPagoOrigen(null);
                    mora.setTipoMembresia(TipoMembresia.MORA);
                    mora.setEstadoMembresia(EstadoMembresia.EN_MORA);
                    mora.setFechaInicio(inicioMora);
                    mora.setFechaFin(finMora);
                    mora.setFechaLimiteGracia(inicioMora.plusDays(15));
                    mora.setValorMensual(m.getValorMensual());
                    mora.setEsActiva(false); // se corrige al final
                    mora.setFechaCreacion(ahora());
                    mora.setFechaUltimoCambio(ahora());
                    mora.setMotivoCambio("MORA_GENERADA_EN_MIGRACION");
                    MembresiaCore moraGuardada = membresiaCoreRepository.save(mora);
                    morasCreadas++;

                    activaIdPorEstudiante.put(idEstudiante, moraGuardada.getIdMembresiaCore());
                    estadoFinalPorEstudiante.put(idEstudiante, Estudiante.EstadoPago.EN_MORA);
                }
                // diasVencida > 15: solo FINALIZADA, no hay activa por esta membresía
                // (si el estudiante no tiene otra más reciente quedará SIN_MEMBRESIA)

                // Rastrear fechaInicio más reciente para diaPago
                LocalDate fi = m.getFechaInicio();
                if (fi != null) {
                    ultimaFechaInicioPorEstudiante.merge(idEstudiante, fi, (a, b) -> b.isAfter(a) ? b : a);
                }

            } catch (Exception e) {
                errores++;
            }
        }

        // Marcar esActiva=true en la membresía elegida por cada estudiante
        for (Map.Entry<Integer, Integer> entry : activaIdPorEstudiante.entrySet()) {
            try {
                membresiaCoreRepository.findById(entry.getValue()).ifPresent(mc -> {
                    mc.setEsActiva(true);
                    membresiaCoreRepository.save(mc);
                });
            } catch (Exception ignored) {}
        }

        // Setear diaPago y estadoPago en cada estudiante migrado
        for (Integer idEst : ultimaFechaInicioPorEstudiante.keySet()) {
            try {
                estudianteRepository.findById(idEst).ifPresent(est -> {
                    est.setDiaPago(ultimaFechaInicioPorEstudiante.get(idEst).getDayOfMonth());
                    Estudiante.EstadoPago nuevoEstado = estadoFinalPorEstudiante.get(idEst);
                    if (nuevoEstado != null) est.setEstadoPago(nuevoEstado);
                    estudianteRepository.save(est);
                });
            } catch (Exception ignored) {}
        }

        // Estudiantes sin membresía migrada → SIN_MEMBRESIA
        Set<Integer> idsMigrados = ultimaFechaInicioPorEstudiante.keySet();
        List<Estudiante> activos = estudianteRepository.findByEstado(true);
        int sinMembresia = 0;
        for (Estudiante est : activos) {
            if (!idsMigrados.contains(est.getIdEstudiante())) {
                est.setEstadoPago(Estudiante.EstadoPago.SIN_MEMBRESIA);
                estudianteRepository.save(est);
                sinMembresia++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("job", "Migracion");
        resultado.put("fecha", hoy.toString());
        resultado.put("membresiasMigradas", migradas);
        resultado.put("morasCreadas", morasCreadas);
        resultado.put("membresiasOmitidas", omitidas);
        resultado.put("estudiantesSinMembresia", sinMembresia);
        resultado.put("errores", errores);
        return resultado;
    }
}
