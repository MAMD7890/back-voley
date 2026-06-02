package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.AsistenciaEstudianteConEstadoPagoDTO;
import galacticos_app_back.galacticos.entity.AsistenciaEstudiante;
import galacticos_app_back.galacticos.repository.AsistenciaEstudianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AsistenciaEstudianteService {
    
    @Autowired
    private AsistenciaEstudianteRepository asistenciaEstudianteRepository;
    
    public List<AsistenciaEstudiante> obtenerTodos() {
        return asistenciaEstudianteRepository.findAll();
    }
    
    /**
     * Obtiene todas las asistencias con el estado de pago de cada estudiante
     */
    public List<AsistenciaEstudianteConEstadoPagoDTO> obtenerTodosConEstadoPago() {
        return asistenciaEstudianteRepository.findAll().stream()
                .map(AsistenciaEstudianteConEstadoPagoDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene las asistencias de una fecha específica con estado de pago
     */
    public List<AsistenciaEstudianteConEstadoPagoDTO> obtenerPorFechaConEstadoPago(LocalDate fecha) {
        return asistenciaEstudianteRepository.findByFecha(fecha).stream()
                .map(AsistenciaEstudianteConEstadoPagoDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene las asistencias de un equipo con estado de pago
     */
    public List<AsistenciaEstudianteConEstadoPagoDTO> obtenerPorEquipoConEstadoPago(Integer idEquipo) {
        return asistenciaEstudianteRepository.findByEquipoIdEquipo(idEquipo).stream()
                .map(AsistenciaEstudianteConEstadoPagoDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene las asistencias de un equipo en una fecha específica con estado de pago
     */
    public List<AsistenciaEstudianteConEstadoPagoDTO> obtenerPorEquipoYFechaConEstadoPago(Integer idEquipo, LocalDate fecha) {
        return asistenciaEstudianteRepository.findByEquipoIdEquipoAndFecha(idEquipo, fecha).stream()
                .map(AsistenciaEstudianteConEstadoPagoDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene las asistencias de un estudiante con estado de pago
     */
    public List<AsistenciaEstudianteConEstadoPagoDTO> obtenerPorEstudianteConEstadoPago(Integer idEstudiante) {
        return asistenciaEstudianteRepository.findByEstudianteIdEstudiante(idEstudiante).stream()
                .map(AsistenciaEstudianteConEstadoPagoDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public Optional<AsistenciaEstudiante> obtenerPorId(Integer id) {
        return asistenciaEstudianteRepository.findById(id);
    }
    
    public AsistenciaEstudiante crear(AsistenciaEstudiante asistencia) {
        // Si ya existe un registro para ese estudiante y fecha, actualizar en lugar de duplicar
        if (asistencia.getEstudiante() != null && asistencia.getFecha() != null) {
            List<AsistenciaEstudiante> existentes = asistenciaEstudianteRepository
                    .findByEstudianteIdEstudianteAndFecha(
                            asistencia.getEstudiante().getIdEstudiante(),
                            asistencia.getFecha());
            if (!existentes.isEmpty()) {
                AsistenciaEstudiante existente = existentes.get(0);
                existente.setAsistio(asistencia.getAsistio());
                existente.setObservaciones(asistencia.getObservaciones());
                if (asistencia.getEquipo() != null) {
                    existente.setEquipo(asistencia.getEquipo());
                }
                return asistenciaEstudianteRepository.save(existente);
            }
        }
        return asistenciaEstudianteRepository.save(asistencia);
    }
    
    @org.springframework.transaction.annotation.Transactional
    public List<AsistenciaEstudiante> crearBulk(List<AsistenciaEstudiante> asistencias) {
        return asistencias.stream()
                .map(this::crear)
                .collect(Collectors.toList());
    }

    public AsistenciaEstudiante actualizar(Integer id, AsistenciaEstudiante asistencia) {
        Optional<AsistenciaEstudiante> existenteOpt = asistenciaEstudianteRepository.findById(id);
        if (existenteOpt.isEmpty()) return null;

        AsistenciaEstudiante existente = existenteOpt.get();
        if (asistencia.getAsistio() != null)      existente.setAsistio(asistencia.getAsistio());
        if (asistencia.getObservaciones() != null) existente.setObservaciones(asistencia.getObservaciones());
        if (asistencia.getFecha() != null)         existente.setFecha(asistencia.getFecha());
        if (asistencia.getEquipo() != null)        existente.setEquipo(asistencia.getEquipo());
        if (asistencia.getEstudiante() != null)    existente.setEstudiante(asistencia.getEstudiante());
        return asistenciaEstudianteRepository.save(existente);
    }
    
    public void eliminar(Integer id) {
        asistenciaEstudianteRepository.deleteById(id);
    }
}
