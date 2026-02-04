package galacticos_app_back.galacticos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO para las estadísticas del sistema de recordatorios.
 * 
 * @author Galacticos App
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordatorioEstadisticasDto {
    
    private LocalDateTime fechaConsulta;
    
    // Estado del servicio
    private boolean servicioTwilioActivo;
    private boolean recordatoriosHabilitados;
    private int maxReintentos;
    
    // Estadísticas del día
    private Map<String, Long> enviosHoy;
    private int pendientesReintento;
    
    // Estadísticas generales
    private long totalRecordatorios;
    private Map<String, Long> estadisticasPorEstado;
    private Map<String, Long> estadisticasPorTipo;
}
