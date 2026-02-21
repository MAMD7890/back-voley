package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.PlanDTO;
import galacticos_app_back.galacticos.entity.Plan;
import galacticos_app_back.galacticos.repository.PlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PlanService {
    
    @Autowired
    private PlanRepository planRepository;
    
    @Autowired
    private ConfiguracionService configuracionService;
    
    /**
     * Obtener todos los planes activos ordenados por visualización
     */
    public List<PlanDTO> obtenerPlanesActivos() {
        return planRepository.findByActivoTrueOrderByOrdenVisualizacion()
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtener todos los planes (incluidos los inactivos) - ADMIN ONLY
     */
    public List<PlanDTO> obtenerTodosLosPlanes() {
        return planRepository.findAll()
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtener un plan por ID
     */
    public Optional<PlanDTO> obtenerPlan(Integer idPlan) {
        return planRepository.findById(idPlan)
                .map(this::convertirADTO);
    }
    
    /**
     * Crear un nuevo plan
     */
    @Transactional
    public PlanDTO crearPlan(PlanDTO planDTO) {
        // Validaciones
        if (planDTO.getNombre() == null || planDTO.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del plan es requerido");
        }
        if (planDTO.getDuracionMeses() == null || planDTO.getDuracionMeses() <= 0) {
            throw new IllegalArgumentException("La duración en meses debe ser mayor a 0");
        }
        if (planDTO.getPrecio() == null || planDTO.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a 0");
        }
        
        // Verificar que no existe otro plan con el mismo nombre
        if (planRepository.findByNombre(planDTO.getNombre()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un plan con ese nombre");
        }
        
        Plan plan = new Plan();
        plan.setNombre(planDTO.getNombre());
        plan.setDescripcion(planDTO.getDescripcion());
        plan.setDuracionMeses(planDTO.getDuracionMeses());
        plan.setPrecio(planDTO.getPrecio());
        
        // Calcular precio mensual
        BigDecimal precioMensual = planDTO.getPrecio()
                .divide(new BigDecimal(planDTO.getDuracionMeses()), 2, RoundingMode.HALF_UP);
        plan.setPrecioMensual(precioMensual);
        
        plan.setDescripcionCorta(planDTO.getDescripcionCorta());
        plan.setActivo(planDTO.getActivo() != null ? planDTO.getActivo() : true);
        plan.setMasPopular(planDTO.getMasPopular() != null ? planDTO.getMasPopular() : false);
        plan.setOrdenVisualizacion(planDTO.getOrdenVisualizacion() != null ? planDTO.getOrdenVisualizacion() : 0);
        plan.setPrecioMatricula(planDTO.getPrecioMatricula());
        plan.setDescripcionMatricula(planDTO.getDescripcionMatricula());
        
        Plan planGuardado = planRepository.save(plan);
        return convertirADTO(planGuardado);
    }
    
    /**
     * Actualizar un plan existente
     */
    @Transactional
    public PlanDTO actualizarPlan(Integer idPlan, PlanDTO planDTO) {
        Plan plan = planRepository.findById(idPlan)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado"));
        
        // Actualizar campos
        if (planDTO.getNombre() != null && !planDTO.getNombre().trim().isEmpty()) {
            // Verificar que el nuevo nombre no sea duplicado (excepto si es el mismo plan)
            Optional<Plan> planExistente = planRepository.findByNombre(planDTO.getNombre());
            if (planExistente.isPresent() && !planExistente.get().getIdPlan().equals(idPlan)) {
                throw new IllegalArgumentException("Ya existe otro plan con ese nombre");
            }
            plan.setNombre(planDTO.getNombre());
        }
        
        if (planDTO.getDescripcion() != null) {
            plan.setDescripcion(planDTO.getDescripcion());
        }
        
        if (planDTO.getDuracionMeses() != null && planDTO.getDuracionMeses() > 0) {
            plan.setDuracionMeses(planDTO.getDuracionMeses());
        }
        
        if (planDTO.getPrecio() != null && planDTO.getPrecio().compareTo(BigDecimal.ZERO) > 0) {
            plan.setPrecio(planDTO.getPrecio());
            // Recalcular precio mensual
            BigDecimal precioMensual = planDTO.getPrecio()
                    .divide(new BigDecimal(plan.getDuracionMeses()), 2, RoundingMode.HALF_UP);
            plan.setPrecioMensual(precioMensual);
        }
        
        if (planDTO.getDescripcionCorta() != null) {
            plan.setDescripcionCorta(planDTO.getDescripcionCorta());
        }
        
        if (planDTO.getActivo() != null) {
            plan.setActivo(planDTO.getActivo());
        }
        
        if (planDTO.getMasPopular() != null) {
            plan.setMasPopular(planDTO.getMasPopular());
        }
        
        if (planDTO.getOrdenVisualizacion() != null) {
            plan.setOrdenVisualizacion(planDTO.getOrdenVisualizacion());
        }
        
        if (planDTO.getPrecioMatricula() != null) {
            plan.setPrecioMatricula(planDTO.getPrecioMatricula());
        }
        
        if (planDTO.getDescripcionMatricula() != null) {
            plan.setDescripcionMatricula(planDTO.getDescripcionMatricula());
        }
        
        Plan planActualizado = planRepository.save(plan);
        return convertirADTO(planActualizado);
    }
    
    /**
     * Eliminar un plan
     */
    @Transactional
    public void eliminarPlan(Integer idPlan) {
        Plan plan = planRepository.findById(idPlan)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado"));
        planRepository.delete(plan);
    }
    
    /**
     * Desactivar un plan (soft delete)
     */
    @Transactional
    public PlanDTO desactivarPlan(Integer idPlan) {
        Plan plan = planRepository.findById(idPlan)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado"));
        plan.setActivo(false);
        Plan planActualizado = planRepository.save(plan);
        return convertirADTO(planActualizado);
    }
    
    /**
     * Convertir entidad a DTO
     * Usa los valores de la entidad (que pueden provenir del plan o de configuración)
     */
    private PlanDTO convertirADTO(Plan plan) {
        return PlanDTO.builder()
                .idPlan(plan.getIdPlan())
                .nombre(plan.getNombre())
                .descripcion(plan.getDescripcion())
                .duracionMeses(plan.getDuracionMeses())
                .precio(plan.getPrecio())
                .precioMensual(plan.getPrecioMensual())
                .descripcionCorta(plan.getDescripcionCorta())
                .activo(plan.getActivo())
                .masPopular(plan.getMasPopular())
                .ordenVisualizacion(plan.getOrdenVisualizacion())
                .precioMatricula(plan.getPrecioMatricula())
                .descripcionMatricula(plan.getDescripcionMatricula())
                .build();
    }
}
