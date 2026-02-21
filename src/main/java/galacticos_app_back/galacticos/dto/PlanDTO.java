package galacticos_app_back.galacticos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO para crear/editar un Plan
 * Incluye información de matrícula para mostrar en el formulario del frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDTO {
    
    private Integer idPlan;
    
    private String nombre;
    
    private String descripcion;
    
    private Integer duracionMeses;
    
    private BigDecimal precio;
    
    private BigDecimal precioMensual;
    
    private String descripcionCorta;
    
    private Boolean activo;
    
    private Boolean masPopular;
    
    private Integer ordenVisualizacion;
    
    /**
     * Valor de matrícula (cargado desde Configuracion)
     * Incluido para mostrar en el formulario del front
     */
    private BigDecimal precioMatricula;
    
    /**
     * Descripción de matrícula (cargado desde Configuracion)
     */
    private String descripcionMatricula;
}
