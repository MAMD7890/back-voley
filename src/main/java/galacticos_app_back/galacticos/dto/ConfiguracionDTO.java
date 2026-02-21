package galacticos_app_back.galacticos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO para configuración del sistema (ej: precio de matrícula)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionDTO {
    
    private Integer idConfiguracion;
    
    private String clave;
    
    private String descripcion;
    
    private String valor;
    
    private String tipo;
}
