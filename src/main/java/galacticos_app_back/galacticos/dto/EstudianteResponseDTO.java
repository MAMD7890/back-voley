package galacticos_app_back.galacticos.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Sede;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EstudianteResponseDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SedeInfo {
        private Integer idSede;
        private String nombre;
        private String direccion;
        private String telefono;
        private Boolean estado;

        public static SedeInfo fromEntity(Sede sede) {
            if (sede == null) return null;
            return SedeInfo.builder()
                    .idSede(sede.getIdSede())
                    .nombre(sede.getNombre())
                    .direccion(sede.getDireccion())
                    .telefono(sede.getTelefono())
                    .estado(sede.getEstado())
                    .build();
        }
    }

    // Identificación
    private Integer idEstudiante;
    private SedeInfo sede;
    private String nombreCompleto;
    private String tipoDocumento;
    private String numeroDocumento;
    private LocalDate fechaNacimiento;
    private Integer edad;
    private String sexo;

    // Información de contacto
    private String direccionResidencia;
    private String barrio;
    private String celularEstudiante;
    private String whatsappEstudiante;
    private String correoEstudiante;

    // Información del tutor
    private String nombreTutor;
    private String parentescoTutor;
    private String documentoTutor;
    private String telefonoTutor;
    private String correoTutor;
    private String ocupacionTutor;

    // Información académica
    private String institucionEducativa;
    private String jornada;
    private Integer gradoActual;

    // Información médica
    private String eps;
    private String tipoSangre;
    private String alergias;
    private String enfermedadesCondiciones;

    // Día de pago
    private Integer diaPagoMes;

    // Información de emergencia
    private String nombreEmergencia;
    private String telefonoEmergencia;
    private String parentescoEmergencia;

    // Poblaciones vulnerables
    private Boolean personaDiscapacidad;
    private String condicionDiscapacidad;
    private Boolean migranteRefugiado;
    private String poblacionEtnica;

    // Camiseta
    private String nombreCamiseta;
    private Integer numeroCamiseta;

    // Fotos
    private String fotoUrl;
    private String fotoNombre;

    // Estado
    private Boolean estado;
    private String estadoPago;

    public static EstudianteResponseDTO fromEntity(Estudiante estudiante) {
        if (estudiante == null) {
            return null;
        }

        return EstudianteResponseDTO.builder()
                .idEstudiante(estudiante.getIdEstudiante())
                .sede(SedeInfo.fromEntity(estudiante.getSede()))
                .nombreCompleto(estudiante.getNombreCompleto())
                .tipoDocumento(estudiante.getTipoDocumento() != null ? estudiante.getTipoDocumento().name() : null)
                .numeroDocumento(estudiante.getNumeroDocumento())
                .fechaNacimiento(estudiante.getFechaNacimiento())
                .edad(estudiante.getEdad())
                .sexo(estudiante.getSexo() != null ? estudiante.getSexo().name() : null)
                .direccionResidencia(estudiante.getDireccionResidencia())
                .barrio(estudiante.getBarrio())
                .celularEstudiante(estudiante.getCelularEstudiante())
                .whatsappEstudiante(estudiante.getWhatsappEstudiante())
                .correoEstudiante(estudiante.getCorreoEstudiante())
                .nombreTutor(estudiante.getNombreTutor())
                .parentescoTutor(estudiante.getParentescoTutor())
                .documentoTutor(estudiante.getDocumentoTutor())
                .telefonoTutor(estudiante.getTelefonoTutor())
                .correoTutor(estudiante.getCorreoTutor())
                .ocupacionTutor(estudiante.getOcupacionTutor())
                .institucionEducativa(estudiante.getInstitucionEducativa())
                .jornada(estudiante.getJornada() != null ? estudiante.getJornada().name() : null)
                .gradoActual(estudiante.getGradoActual())
                .eps(estudiante.getEps())
                .tipoSangre(estudiante.getTipoSangre())
                .alergias(estudiante.getAlergias())
                .enfermedadesCondiciones(estudiante.getEnfermedadesCondiciones())
                .diaPagoMes(estudiante.getDiaPagoMes())
                .nombreEmergencia(estudiante.getNombreEmergencia())
                .telefonoEmergencia(estudiante.getTelefonoEmergencia())
                .parentescoEmergencia(estudiante.getParentescoEmergencia())
                .personaDiscapacidad(estudiante.getPersonaDiscapacidad())
                .condicionDiscapacidad(estudiante.getCondicionDiscapacidad())
                .migranteRefugiado(estudiante.getMigranteRefugiado())
                .poblacionEtnica(estudiante.getPoblacionEtnica())
                .nombreCamiseta(estudiante.getNombreCamiseta())
                .numeroCamiseta(estudiante.getNumeroCamiseta())
                .fotoUrl(estudiante.getFotoUrl())
                .fotoNombre(estudiante.getFotoNombre())
                .estado(estudiante.getEstado())
                .estadoPago(estudiante.getEstadoPago() != null ? estudiante.getEstadoPago().name() : null)
                .build();
    }
}
