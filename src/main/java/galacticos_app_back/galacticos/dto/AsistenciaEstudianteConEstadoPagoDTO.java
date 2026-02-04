package galacticos_app_back.galacticos.dto;

import galacticos_app_back.galacticos.entity.AsistenciaEstudiante;
import galacticos_app_back.galacticos.entity.Estudiante;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para la lista de asistencia de estudiantes que incluye el estado de pago
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsistenciaEstudianteConEstadoPagoDTO {
    
    // Datos de la asistencia
    private Integer idAsistencia;
    private LocalDate fecha;
    private Boolean asistio;
    private String observaciones;
    
    // Datos del estudiante
    private Integer idEstudiante;
    private String nombreCompleto;
    private String numeroDocumento;
    private String fotoUrl;
    private Integer edad;
    
    // Estado de pago del estudiante
    private Estudiante.EstadoPago estadoPago;
    private String estadoPagoDescripcion;
    private String colorEstadoPago;
    
    // Datos del equipo
    private Integer idEquipo;
    private String nombreEquipo;
    
    /**
     * Crea un DTO a partir de una entidad AsistenciaEstudiante
     */
    public static AsistenciaEstudianteConEstadoPagoDTO fromEntity(AsistenciaEstudiante asistencia) {
        Estudiante estudiante = asistencia.getEstudiante();
        
        return AsistenciaEstudianteConEstadoPagoDTO.builder()
                .idAsistencia(asistencia.getIdAsistencia())
                .fecha(asistencia.getFecha())
                .asistio(asistencia.getAsistio())
                .observaciones(asistencia.getObservaciones())
                .idEstudiante(estudiante != null ? estudiante.getIdEstudiante() : null)
                .nombreCompleto(estudiante != null ? estudiante.getNombreCompleto() : null)
                .numeroDocumento(estudiante != null ? estudiante.getNumeroDocumento() : null)
                .fotoUrl(estudiante != null ? estudiante.getFotoUrl() : null)
                .edad(estudiante != null ? estudiante.getEdad() : null)
                .estadoPago(estudiante != null ? estudiante.getEstadoPago() : null)
                .estadoPagoDescripcion(estudiante != null ? 
                        EstudianteConEstadoPagoDTO.getDescripcionEstadoPago(estudiante.getEstadoPago()) : null)
                .colorEstadoPago(estudiante != null ? 
                        EstudianteConEstadoPagoDTO.getColorEstadoPago(estudiante.getEstadoPago()) : null)
                .idEquipo(asistencia.getEquipo() != null ? asistencia.getEquipo().getIdEquipo() : null)
                .nombreEquipo(asistencia.getEquipo() != null ? asistencia.getEquipo().getNombre() : null)
                .build();
    }
}
