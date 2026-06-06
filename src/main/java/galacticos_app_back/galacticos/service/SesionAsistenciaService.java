package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.asistencia.ReporteAsistenciaDTO;
import galacticos_app_back.galacticos.dto.asistencia.SesionAsistenciaRequestDTO;
import galacticos_app_back.galacticos.entity.*;
import galacticos_app_back.galacticos.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        int creados = 0, actualizados = 0, errores = 0;

        for (SesionAsistenciaRequestDTO.RegistroAsistenciaDTO r : dto.getRegistros()) {
            try {
                Estudiante est = estudianteRepository.findById(r.getIdEstudiante())
                        .orElseThrow(() -> new RuntimeException("Estudiante no encontrado: " + r.getIdEstudiante()));

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
        resultado.put("errores", errores);
        return resultado;
    }

    // ─── Reporte: stats del header ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReporteAsistenciaDTO.Stats obtenerStats(LocalDate desde, LocalDate hasta,
                                                    Integer idSede, String busqueda, Boolean asistio) {
        List<AsistenciaV2> registros = asistenciaRepository.findWithFilters(desde, hasta, idSede, busqueda, asistio);

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
    public List<ReporteAsistenciaDTO.DetalleRegistro> obtenerDetalle(LocalDate desde, LocalDate hasta,
                                                                      Integer idSede, String busqueda,
                                                                      Boolean asistio) {
        return asistenciaRepository.findWithFilters(desde, hasta, idSede, busqueda, asistio)
                .stream()
                .map(this::toDetalleRegistro)
                .collect(Collectors.toList());
    }

    // ─── Reporte: vista resumen (agrupado por estudiante) ────────────────────

    @Transactional(readOnly = true)
    public List<ReporteAsistenciaDTO.ResumenEstudiante> obtenerResumen(LocalDate desde, LocalDate hasta,
                                                                        Integer idSede, String busqueda,
                                                                        Boolean asistio) {
        List<AsistenciaV2> registros = asistenciaRepository.findWithFilters(desde, hasta, idSede, busqueda, asistio);

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
