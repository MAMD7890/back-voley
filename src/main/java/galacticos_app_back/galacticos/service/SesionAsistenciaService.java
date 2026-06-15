package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.EstudianteResponseDTO;
import galacticos_app_back.galacticos.dto.asistencia.EstudianteAsistenciaDiaDTO;
import galacticos_app_back.galacticos.entity.AsistenciaEstudiante;
import galacticos_app_back.galacticos.entity.Sede;
import galacticos_app_back.galacticos.repository.AsistenciaEstudianteRepository;
import galacticos_app_back.galacticos.dto.asistencia.ReporteAsistenciaDTO;
import galacticos_app_back.galacticos.dto.asistencia.SesionAsistenciaRequestDTO;
import galacticos_app_back.galacticos.entity.*;
import galacticos_app_back.galacticos.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SesionAsistenciaService {

    @Autowired private SesionAsistenciaRepository sesionRepository;
    @Autowired private AsistenciaV2Repository asistenciaRepository;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private AsistenciaEstudianteRepository asistenciaEstudianteRepository;
    @Autowired private MembresiaCoreRepository membresiaCoreRepository;

    @Autowired
    private galacticos_app_back.galacticos.repository.SedeRepository sedeRepository;

    @Autowired
    private galacticos_app_back.galacticos.repository.EquipoRepository equipoRepository;

    private LocalDateTime ahora() {
        return LocalDateTime.now(ZoneId.of("America/Bogota"));
    }

    // ─── Guardar sesión de asistencia ────────────────────────────────────────

    @Transactional
    public Map<String, Object> guardarSesion(SesionAsistenciaRequestDTO dto) {
        if (dto.getIdSede() == null || dto.getFecha() == null) {
            throw new IllegalArgumentException("idSede y fecha son obligatorios");
        }

        Sede sede = sedeRepository.findById(dto.getIdSede())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada: " + dto.getIdSede()));

        Equipo equipo = null;
        if (dto.getIdEquipo() != null) {
            equipo = equipoRepository.findById(dto.getIdEquipo())
                    .orElseThrow(() -> new RuntimeException("Equipo no encontrado: " + dto.getIdEquipo()));
        }

        // Buscar sesión existente o crear nueva
        final Equipo equipoFinal = equipo;
        SesionAsistencia sesion;
        if (equipoFinal != null) {
            sesion = sesionRepository
                    .findBySedeIdSedeAndEquipoIdEquipoAndFecha(dto.getIdSede(), dto.getIdEquipo(), dto.getFecha())
                    .orElseGet(() -> {
                        SesionAsistencia nueva = new SesionAsistencia();
                        nueva.setSede(sede);
                        nueva.setEquipo(equipoFinal);
                        nueva.setFecha(dto.getFecha());
                        nueva.setFechaCreacion(ahora());
                        return sesionRepository.save(nueva);
                    });
        } else {
            sesion = sesionRepository
                    .findBySedeIdSedeAndEquipoIsNullAndFecha(dto.getIdSede(), dto.getFecha())
                    .orElseGet(() -> {
                        SesionAsistencia nueva = new SesionAsistencia();
                        nueva.setSede(sede);
                        nueva.setFecha(dto.getFecha());
                        nueva.setFechaCreacion(ahora());
                        return sesionRepository.save(nueva);
                    });
        }

        final SesionAsistencia sesionFinal = sesion;
        final LocalDate corte = LocalDate.now().minusMonths(2);
        int creados = 0, actualizados = 0, omitidos = 0, errores = 0;

        for (SesionAsistenciaRequestDTO.RegistroAsistenciaDTO r : dto.getRegistros()) {
            try {
                Estudiante est = estudianteRepository.findById(r.getIdEstudiante())
                        .orElseThrow(() -> new RuntimeException("Estudiante no encontrado: " + r.getIdEstudiante()));

                // Excluir inactivos y estudiantes sin membresía reciente (>2 meses vencida o sin membresía)
                if (!Boolean.TRUE.equals(est.getEstado()) ||
                        membresiaCoreRepository.countMembresiasRecientes(est.getIdEstudiante(), corte) == 0) {
                    omitidos++;
                    continue;
                }

                Optional<AsistenciaV2> existente = asistenciaRepository
                        .findBySesionIdSesionAndEstudianteIdEstudiante(
                                sesionFinal.getIdSesion(), r.getIdEstudiante());

                if (existente.isPresent()) {
                    AsistenciaV2 a = existente.get();
                    a.setAsistio(r.getAsistio() != null ? r.getAsistio() : false);
                    a.setObservaciones(r.getObservaciones());
                    asistenciaRepository.save(a);
                    actualizados++;
                } else {
                    AsistenciaV2 a = new AsistenciaV2();
                    a.setSesion(sesionFinal);
                    a.setEstudiante(est);
                    a.setAsistio(r.getAsistio() != null ? r.getAsistio() : false);
                    a.setObservaciones(r.getObservaciones());
                    asistenciaRepository.save(a);
                    creados++;
                }
            } catch (Exception e) {
                errores++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("idSesion", sesionFinal.getIdSesion());
        resultado.put("fecha", sesionFinal.getFecha());
        resultado.put("idSede", sede.getIdSede());
        resultado.put("nombreSede", sede.getNombre());
        resultado.put("creados", creados);
        resultado.put("actualizados", actualizados);
        resultado.put("omitidos", omitidos);
        resultado.put("errores", errores);
        return resultado;
    }

    // ─── Reporte: stats del header ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReporteAsistenciaDTO.Stats obtenerStats(LocalDate desde, LocalDate hasta,
                                                    Integer idSede, String busqueda, Boolean asistio,
                                                    Estudiante.EstadoPago estadoPago) {
        LocalDate corte = LocalDate.now().minusMonths(2);
        List<AsistenciaV2> registros = asistenciaRepository.findWithFilters(desde, hasta, idSede, busqueda, asistio, estadoPago, corte);

        long presentes = registros.stream().filter(a -> Boolean.TRUE.equals(a.getAsistio())).count();
        long ausentes  = registros.stream().filter(a -> Boolean.FALSE.equals(a.getAsistio())).count();
        long estudiantes = registros.stream()
                .map(a -> a.getEstudiante().getIdEstudiante()).distinct().count();
        long dias = registros.stream()
                .map(a -> a.getSesion().getFecha()).distinct().count();
        double porcentaje = registros.isEmpty() ? 0
                : Math.round((presentes * 100.0 / registros.size()) * 10.0) / 10.0;

        return ReporteAsistenciaDTO.Stats.builder()
                .totalRegistros(registros.size())
                .totalEstudiantes(estudiantes)
                .totalDias(dias)
                .totalPresentes(presentes)
                .totalAusentes(ausentes)
                .porcentajeGeneral(porcentaje)
                .build();
    }

    // ─── Reporte: vista detalle (lista de registros individuales) ────────────

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetalle(LocalDate desde, LocalDate hasta,
                                              Integer idSede, String busqueda,
                                              Boolean asistio, Estudiante.EstadoPago estadoPago,
                                              int page, int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "sesion.fecha")
                        .and(Sort.by(Sort.Direction.ASC, "estudiante.nombreCompleto")));
        LocalDate corte = LocalDate.now().minusMonths(2);
        Page<AsistenciaV2> resultado = asistenciaRepository.findWithFiltersPaged(
                desde, hasta, idSede, busqueda, asistio, estadoPago, corte, pageable);
        List<ReporteAsistenciaDTO.DetalleRegistro> content = resultado.getContent().stream()
                .map(this::toDetalleRegistro)
                .collect(Collectors.toList());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", content);
        response.put("totalElements", resultado.getTotalElements());
        response.put("totalPages", resultado.getTotalPages());
        response.put("page", resultado.getNumber());
        response.put("size", resultado.getSize());
        return response;
    }

    // ─── Reporte: vista resumen (agrupado por estudiante) ────────────────────

    @Transactional(readOnly = true)
    public List<ReporteAsistenciaDTO.ResumenEstudiante> obtenerResumen(LocalDate desde, LocalDate hasta,
                                                                        Integer idSede, String busqueda,
                                                                        Boolean asistio,
                                                                        Estudiante.EstadoPago estadoPago) {
        LocalDate corte = LocalDate.now().minusMonths(2);
        List<AsistenciaV2> registros = asistenciaRepository.findWithFilters(desde, hasta, idSede, busqueda, asistio, estadoPago, corte);

        // Agrupar por estudiante
        Map<Integer, List<AsistenciaV2>> porEstudiante = registros.stream()
                .collect(Collectors.groupingBy(a -> a.getEstudiante().getIdEstudiante()));

        // Para % correcto: días únicos por sede del estudiante en el rango
        // totalDias = número de registros del estudiante (ya que al guardar sesión se crean todos)
        return porEstudiante.entrySet().stream()
                .map(entry -> {
                    List<AsistenciaV2> lista = entry.getValue();
                    AsistenciaV2 primero = lista.get(0);
                    Estudiante est = primero.getEstudiante();

                    long presentes = lista.stream().filter(a -> Boolean.TRUE.equals(a.getAsistio())).count();
                    long total = lista.size();
                    long ausentesCount = total - presentes;
                    double pct = total > 0 ? Math.round((presentes * 100.0 / total) * 10.0) / 10.0 : 0;

                    return ReporteAsistenciaDTO.ResumenEstudiante.builder()
                            .idEstudiante(est.getIdEstudiante())
                            .nombreCompleto(est.getNombreCompleto())
                            .numeroDocumento(est.getNumeroDocumento())
                            .nombreSede(est.getSede() != null ? est.getSede().getNombre() : null)
                            .totalDias(total)
                            .diasPresente(presentes)
                            .diasAusente(ausentesCount)
                            .porcentajeAsistencia(pct)
                            .build();
                })
                .sorted(Comparator.comparing(ReporteAsistenciaDTO.ResumenEstudiante::getNombreCompleto))
                .collect(Collectors.toList());
    }

    // ─── Detalle de un estudiante específico ─────────────────────────────────

    @Transactional(readOnly = true)
    public ReporteAsistenciaDTO.DetalleEstudiante obtenerDetalleEstudiante(Integer idEstudiante) {
        Estudiante est = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado: " + idEstudiante));

        List<AsistenciaV2> historial = asistenciaRepository.findByEstudianteOrderByFechaDesc(idEstudiante);

        long presentes = historial.stream().filter(a -> Boolean.TRUE.equals(a.getAsistio())).count();
        long total = historial.size();
        double pct = total > 0 ? Math.round((presentes * 100.0 / total) * 10.0) / 10.0 : 0;

        List<ReporteAsistenciaDTO.DetalleRegistro> registros = historial.stream()
                .map(this::toDetalleRegistro)
                .collect(Collectors.toList());

        return ReporteAsistenciaDTO.DetalleEstudiante.builder()
                .idEstudiante(est.getIdEstudiante())
                .nombreCompleto(est.getNombreCompleto())
                .numeroDocumento(est.getNumeroDocumento())
                .nombreSede(est.getSede() != null ? est.getSede().getNombre() : null)
                .totalDias(total)
                .diasPresente(presentes)
                .diasAusente(total - presentes)
                .porcentajeAsistencia(pct)
                .historial(registros)
                .build();
    }

    // ─── Migración de asistencias legado → v2 ────────────────────────────────

    @Transactional
    public Map<String, Object> migrarAsistenciasLegado() {
        List<AsistenciaEstudiante> legado = asistenciaEstudianteRepository.findAll();

        int sesionesCreadas = 0, creadas = 0, omitidas = 0, errores = 0;

        Map<String, List<AsistenciaEstudiante>> grupos = legado.stream()
                .filter(a -> a.getEstudiante() != null
                        && a.getEstudiante().getSede() != null
                        && a.getFecha() != null)
                .collect(Collectors.groupingBy(a ->
                        a.getEstudiante().getSede().getIdSede() + "_" + a.getFecha()));

        for (List<AsistenciaEstudiante> registros : grupos.values()) {
            AsistenciaEstudiante primero = registros.get(0);
            Sede sede = primero.getEstudiante().getSede();
            LocalDate fecha = primero.getFecha();

            final SesionAsistencia sesion;
            var sesionOpt = sesionRepository.findBySedeIdSedeAndEquipoIsNullAndFecha(sede.getIdSede(), fecha);
            if (sesionOpt.isPresent()) {
                sesion = sesionOpt.get();
            } else {
                SesionAsistencia nueva = new SesionAsistencia();
                nueva.setSede(sede);
                nueva.setFecha(fecha);
                nueva.setFechaCreacion(ahora());
                sesion = sesionRepository.save(nueva);
                sesionesCreadas++;
            }

            for (AsistenciaEstudiante a : registros) {
                try {
                    boolean existe = asistenciaRepository
                            .findBySesionIdSesionAndEstudianteIdEstudiante(
                                    sesion.getIdSesion(), a.getEstudiante().getIdEstudiante())
                            .isPresent();
                    if (existe) {
                        omitidas++;
                    } else {
                        AsistenciaV2 nuevo = new AsistenciaV2();
                        nuevo.setSesion(sesion);
                        nuevo.setEstudiante(a.getEstudiante());
                        nuevo.setAsistio(a.getAsistio() != null ? a.getAsistio() : false);
                        nuevo.setObservaciones(a.getObservaciones());
                        asistenciaRepository.save(nuevo);
                        creadas++;
                    }
                } catch (Exception e) {
                    errores++;
                }
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalLegado", legado.size());
        resultado.put("sesionesCreadas", sesionesCreadas);
        resultado.put("registrosCreados", creadas);
        resultado.put("registrosOmitidos", omitidas);
        resultado.put("errores", errores);
        return resultado;
    }

    // ─── Estudiantes con asistencia para un día ───────────────────────────────

    @Transactional(readOnly = true)
    public List<EstudianteAsistenciaDiaDTO> obtenerEstudiantesConAsistencia(Integer idSede, LocalDate fecha, String busqueda, Estudiante.EstadoPago estadoPago) {
        Optional<SesionAsistencia> sesionOpt = sesionRepository
                .findBySedeIdSedeAndEquipoIsNullAndFecha(idSede, fecha);

        Map<Integer, AsistenciaV2> asistenciaMap = new HashMap<>();
        if (sesionOpt.isPresent()) {
            asistenciaRepository
                    .findBySesionIdSesionOrderByEstudianteNombreCompletoAsc(sesionOpt.get().getIdSesion())
                    .forEach(a -> asistenciaMap.put(a.getEstudiante().getIdEstudiante(), a));
        }

        String filtro = busqueda != null ? busqueda.toLowerCase() : null;
        return estudianteRepository.findBySedeIdSede(idSede).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEstado()))
                .filter(e -> filtro == null
                        || e.getNombreCompleto().toLowerCase().contains(filtro)
                        || (e.getNumeroDocumento() != null && e.getNumeroDocumento().contains(busqueda)))
                .filter(e -> estadoPago == null || estadoPago.equals(e.getEstadoPago()))
                .sorted(Comparator.comparing(Estudiante::getNombreCompleto))
                .map(est -> {
                    AsistenciaV2 a = asistenciaMap.get(est.getIdEstudiante());
                    return EstudianteAsistenciaDiaDTO.builder()
                            .estudiante(EstudianteResponseDTO.fromEntity(est))
                            .idAsistencia(a != null ? a.getIdAsistencia() : null)
                            .asistio(a != null ? a.getAsistio() : null)
                            .observaciones(a != null ? a.getObservaciones() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ─── Deduplicar sesiones duplicadas (mismo día/sede) ─────────────────────

    @Transactional
    public Map<String, Object> deduplicarSesiones() {
        // Agrupa todas las sesiones sin equipo por sede+fecha
        Map<String, List<SesionAsistencia>> grupos = sesionRepository.findAllSinEquipoOrdenadas()
                .stream()
                .collect(Collectors.groupingBy(s -> s.getSede().getIdSede() + "_" + s.getFecha()));

        int sesionesEliminadas = 0, asistenciasMigradas = 0, asistenciasEliminadas = 0;

        for (List<SesionAsistencia> sesiones : grupos.values()) {
            if (sesiones.size() <= 1) continue;

            // La primera (menor idSesion) es la canónica, las demás son duplicadas
            SesionAsistencia canonical = sesiones.get(0);

            for (int i = 1; i < sesiones.size(); i++) {
                SesionAsistencia duplicada = sesiones.get(i);

                List<AsistenciaV2> registros = asistenciaRepository
                        .findBySesionIdSesionOrderByEstudianteNombreCompletoAsc(duplicada.getIdSesion());

                for (AsistenciaV2 a : registros) {
                    Optional<AsistenciaV2> enCanonical = asistenciaRepository
                            .findBySesionIdSesionAndEstudianteIdEstudiante(
                                    canonical.getIdSesion(), a.getEstudiante().getIdEstudiante());

                    if (enCanonical.isPresent()) {
                        // Ya existe en la sesión canónica: si la duplicada tiene asistio=true y la canónica no, prevalece true
                        AsistenciaV2 c = enCanonical.get();
                        if (Boolean.TRUE.equals(a.getAsistio()) && !Boolean.TRUE.equals(c.getAsistio())) {
                            c.setAsistio(true);
                            if (a.getObservaciones() != null && !a.getObservaciones().isBlank()) {
                                c.setObservaciones(a.getObservaciones());
                            }
                            asistenciaRepository.save(c);
                        }
                        asistenciaRepository.delete(a);
                        asistenciasEliminadas++;
                    } else {
                        // No existe en la canónica: mover
                        a.setSesion(canonical);
                        asistenciaRepository.save(a);
                        asistenciasMigradas++;
                    }
                }

                sesionRepository.delete(duplicada);
                sesionesEliminadas++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("sesionesEliminadas", sesionesEliminadas);
        resultado.put("asistenciasMigradas", asistenciasMigradas);
        resultado.put("asistenciasEliminadas", asistenciasEliminadas);
        return resultado;
    }

    // ─── Limpieza de asistencias de estudiantes inelegibles ──────────────────

    @Transactional
    public Map<String, Object> limpiarAsistenciasInelegibles() {
        LocalDate corte = LocalDate.now().minusMonths(2);
        int eliminados = asistenciaRepository.deleteInelegibles(corte);
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("registrosEliminados", eliminados);
        resultado.put("corte", corte.toString());
        return resultado;
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private ReporteAsistenciaDTO.DetalleRegistro toDetalleRegistro(AsistenciaV2 a) {
        Estudiante est = a.getEstudiante();
        return ReporteAsistenciaDTO.DetalleRegistro.builder()
                .idAsistencia(a.getIdAsistencia())
                .fecha(a.getSesion().getFecha())
                .idEstudiante(est.getIdEstudiante())
                .nombreCompleto(est.getNombreCompleto())
                .numeroDocumento(est.getNumeroDocumento())
                .nombreSede(est.getSede() != null ? est.getSede().getNombre() : null)
                .asistio(a.getAsistio())
                .observaciones(a.getObservaciones())
                .build();
    }
}
