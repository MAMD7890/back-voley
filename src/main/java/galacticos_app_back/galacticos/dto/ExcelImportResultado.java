package galacticos_app_back.galacticos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para cada resultado de importación de un estudiante
 * Especificación: POST /api/estudiantes/importar-excel
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelImportResultado {
    
    // Información de fila
    private Integer fila;
    
    // Información exitosa
    private Integer estudianteId;
    private String nombreEstudiante;
    private String usuarioCreado;
    private String passwordGenerada;
    
    // Estado
    private String estado;  // "exitoso" o "error"
    private String mensaje;
    private String detalles;
}
