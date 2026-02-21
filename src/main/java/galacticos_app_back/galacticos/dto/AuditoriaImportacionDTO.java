package galacticos_app_back.galacticos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO para registrar auditoría de importaciones
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaImportacionDTO {
    
    private Integer usuarioId;
    private Integer sedeId;
    private Integer totalProcesadas;
    private Integer exitosos;
    private Integer errores;
    private String nombreArchivo;
    private Long tamanioArchivo;
    private LocalDateTime fechaImportacion;
    private String ipOrigen;
    private String detalles;
    
    /**
     * Resumen para logging
     */
    public String getResumen() {
        return String.format(
            "Importación: Usuario=%d, Sede=%d, Total=%d, Exitosos=%d, Errores=%d",
            usuarioId, sedeId, totalProcesadas, exitosos, errores
        );
    }
}
