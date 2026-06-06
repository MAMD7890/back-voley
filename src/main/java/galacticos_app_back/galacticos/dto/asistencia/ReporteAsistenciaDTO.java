package galacticos_app_back.galacticos.dto.asistencia;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

public class ReporteAsistenciaDTO {

    @Data
    @Builder
    public static class Stats {
        private long totalRegistros;
        private long totalEstudiantes;
        private long totalDias;
        private long totalPresentes;
        private long totalAusentes;
        private double porcentajeGeneral;
    }

    @Data
    @Builder
    public static class ResumenEstudiante {
        private Integer idEstudiante;
        private String nombreCompleto;
        private String numeroDocumento;
        private String nombreSede;
        private long totalDias;
        private long diasPresente;
        private long diasAusente;
        private double porcentajeAsistencia;
    }

    @Data
    @Builder
    public static class DetalleRegistro {
        private Integer idAsistencia;
        private LocalDate fecha;
        private Integer idEstudiante;
        private String nombreCompleto;
        private String numeroDocumento;
        private String nombreSede;
        private Boolean asistio;
        private String observaciones;
    }

    @Data
    @Builder
    public static class DetalleEstudiante {
        private Integer idEstudiante;
        private String nombreCompleto;
        private String numeroDocumento;
        private String nombreSede;
        private long totalDias;
        private long diasPresente;
        private long diasAusente;
        private double porcentajeAsistencia;
        private List<DetalleRegistro> historial;
    }
}
