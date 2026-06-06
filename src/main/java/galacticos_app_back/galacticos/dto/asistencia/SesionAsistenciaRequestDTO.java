package galacticos_app_back.galacticos.dto.asistencia;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class SesionAsistenciaRequestDTO {

    private Integer idSede;
    private Integer idEquipo;
    private LocalDate fecha;
    private List<RegistroAsistenciaDTO> registros;

    @Data
    @NoArgsConstructor
    public static class RegistroAsistenciaDTO {
        private Integer idEstudiante;
        private Boolean asistio;
        private String observaciones;
    }
}
