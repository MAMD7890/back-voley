package galacticos_app_back.galacticos.dto;

import galacticos_app_back.galacticos.entity.Estudiante;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO que incluye información del estudiante con su estado de pago
 * Usado para la tabla de estudiantes y la lista de asistencia
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstudianteConEstadoPagoDTO {
    
    private Integer idEstudiante;
    private String nombreCompleto;
    private String numeroDocumento;
    private String tipoDocumento;
    private Integer edad;
    private String fotoUrl;
    private String celularEstudiante;
    private String correoEstudiante;
    private Boolean estado;
    
    // Información de estado de pago
    private Estudiante.EstadoPago estadoPago;
    private String estadoPagoDescripcion;
    private String colorEstadoPago;
    
    // Información de la sede
    private Integer idSede;
    private String nombreSede;
    
    // Información del nivel
    private String nivelActual;
    
    // Información del último pago (opcional)
    private LocalDate fechaUltimoPago;
    private String mesUltimoPago;
    
    /**
     * Crea un DTO a partir de una entidad Estudiante
     */
    public static EstudianteConEstadoPagoDTO fromEntity(Estudiante estudiante) {
        return EstudianteConEstadoPagoDTO.builder()
                .idEstudiante(estudiante.getIdEstudiante())
                .nombreCompleto(estudiante.getNombreCompleto())
                .numeroDocumento(estudiante.getNumeroDocumento())
                .tipoDocumento(estudiante.getTipoDocumento() != null ? estudiante.getTipoDocumento().name() : null)
                .edad(estudiante.getEdad())
                .fotoUrl(estudiante.getFotoUrl())
                .celularEstudiante(estudiante.getCelularEstudiante())
                .correoEstudiante(estudiante.getCorreoEstudiante())
                .estado(estudiante.getEstado())
                .estadoPago(estudiante.getEstadoPago())
                .estadoPagoDescripcion(getDescripcionEstadoPago(estudiante.getEstadoPago()))
                .colorEstadoPago(getColorEstadoPago(estudiante.getEstadoPago()))
                .idSede(estudiante.getSede() != null ? estudiante.getSede().getIdSede() : null)
                .nombreSede(estudiante.getSede() != null ? estudiante.getSede().getNombre() : null)
                .nivelActual(estudiante.getNivelActual() != null ? estudiante.getNivelActual().name() : null)
                .build();
    }
    
    /**
     * Obtiene la descripción legible del estado de pago
     */
    public static String getDescripcionEstadoPago(Estudiante.EstadoPago estadoPago) {
        if (estadoPago == null) {
            return "Sin estado";
        }
        switch (estadoPago) {
            case PENDIENTE:
                return "Pendiente por pagar";
            case AL_DIA:
                return "Al día";
            case EN_MORA:
                return "En mora";
            case COMPROMISO_PAGO:
                return "Compromiso de pago";
            default:
                return "Desconocido";
        }
    }
    
    /**
     * Obtiene el color asociado al estado de pago para el frontend
     */
    public static String getColorEstadoPago(Estudiante.EstadoPago estadoPago) {
        if (estadoPago == null) {
            return "#9E9E9E"; // Gris
        }
        switch (estadoPago) {
            case PENDIENTE:
                return "#FFA726"; // Naranja
            case AL_DIA:
                return "#66BB6A"; // Verde
            case EN_MORA:
                return "#EF5350"; // Rojo
            case COMPROMISO_PAGO:
                return "#42A5F5"; // Azul
            default:
                return "#9E9E9E"; // Gris
        }
    }
}
