package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Membresia;
import galacticos_app_back.galacticos.entity.RecordatorioPago;
import galacticos_app_back.galacticos.entity.RecordatorioPago.EstadoEnvio;
import galacticos_app_back.galacticos.entity.RecordatorioPago.TipoRecordatorio;
import galacticos_app_back.galacticos.repository.RecordatorioPagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de recordatorios de pago.
 * Proporciona operaciones CRUD y consultas especializadas.
 * 
 * @author Galacticos App
 * @version 2.0
 */
@Service
public class RecordatorioPagoService {
    
    @Autowired
    private RecordatorioPagoRepository recordatorioPagoRepository;
    
    // ==================== OPERACIONES CRUD ====================
    
    public List<RecordatorioPago> obtenerTodos() {
        return recordatorioPagoRepository.findAll();
    }
    
    public Optional<RecordatorioPago> obtenerPorId(Integer id) {
        return recordatorioPagoRepository.findById(id);
    }
    
    public RecordatorioPago crear(RecordatorioPago recordatorio) {
        return recordatorioPagoRepository.save(recordatorio);
    }
    
    public RecordatorioPago actualizar(Integer id, RecordatorioPago recordatorio) {
        Optional<RecordatorioPago> existente = recordatorioPagoRepository.findById(id);
        if (existente.isPresent()) {
            recordatorio.setIdRecordatorio(id);
            return recordatorioPagoRepository.save(recordatorio);
        }
        return null;
    }
    
    public void eliminar(Integer id) {
        recordatorioPagoRepository.deleteById(id);
    }
    
    // ==================== CONSULTAS ESPECIALIZADAS ====================
    
    /**
     * Obtiene todos los recordatorios de un estudiante ordenados por fecha.
     */
    public List<RecordatorioPago> obtenerPorEstudiante(Estudiante estudiante) {
        return recordatorioPagoRepository.findByEstudianteOrderByFechaEnvioDesc(estudiante);
    }
    
    /**
     * Obtiene todos los recordatorios de una membresía.
     */
    public List<RecordatorioPago> obtenerPorMembresia(Membresia membresia) {
        return recordatorioPagoRepository.findByMembresia(membresia);
    }
    
    /**
     * Verifica si ya se envió un recordatorio específico.
     */
    public boolean existeRecordatorio(Membresia membresia, TipoRecordatorio tipo, LocalDate fechaVencimiento) {
        return recordatorioPagoRepository.existsByMembresiaAndTipoRecordatorioAndFechaVencimientoReferencia(
                membresia, tipo, fechaVencimiento);
    }
    
    /**
     * Obtiene recordatorios fallidos pendientes de reintento.
     */
    public List<RecordatorioPago> obtenerFallidosPendientes(int maxIntentos) {
        return recordatorioPagoRepository.findRecordatoriosParaReintentar(maxIntentos);
    }
    
    /**
     * Obtiene estadísticas de recordatorios por estado.
     */
    public Map<EstadoEnvio, Long> obtenerEstadisticasPorEstado() {
        return recordatorioPagoRepository.findAll().stream()
                .filter(r -> r.getEstadoEnvio() != null)
                .collect(Collectors.groupingBy(
                        RecordatorioPago::getEstadoEnvio, 
                        Collectors.counting()
                ));
    }
    
    /**
     * Obtiene el historial de recordatorios de un estudiante por su ID.
     */
    public List<RecordatorioPago> obtenerHistorialPorEstudianteId(Integer idEstudiante) {
        return recordatorioPagoRepository.findAll().stream()
                .filter(r -> r.getEstudiante() != null && 
                            r.getEstudiante().getIdEstudiante().equals(idEstudiante))
                .sorted((r1, r2) -> r2.getFechaEnvio().compareTo(r1.getFechaEnvio()))
                .collect(Collectors.toList());
    }
}
