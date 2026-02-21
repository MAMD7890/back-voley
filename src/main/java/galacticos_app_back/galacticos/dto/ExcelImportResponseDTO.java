package galacticos_app_back.galacticos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * DTO para la respuesta de importación de estudiantes desde Excel
 * Cumple con especificación: POST /api/estudiantes/importar-excel
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelImportResponseDTO {
    private Integer exitosos;
    private Integer errores;
    private Integer total;
    private List<ExcelImportResultado> resultados;
    private String timestamp;

    /**
     * Constructor con timestamp automático
     */
    public ExcelImportResponseDTO(Integer exitosos, Integer errores, Integer total, List<ExcelImportResultado> resultados) {
        this.exitosos = exitosos;
        this.errores = errores;
        this.total = total;
        this.resultados = resultados;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + "Z";
    }
}
