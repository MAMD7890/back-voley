package galacticos_app_back.galacticos.dto.asistencia;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import galacticos_app_back.galacticos.dto.EstudianteResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstudianteAsistenciaDiaDTO {

    @JsonUnwrapped
    private EstudianteResponseDTO estudiante;

    private Integer idAsistencia;
    private Boolean asistio;
    private String observaciones;
}
