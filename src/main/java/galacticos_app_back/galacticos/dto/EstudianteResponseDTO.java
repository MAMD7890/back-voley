package galacticos_app_back.galacticos.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import galacticos_app_back.galacticos.entity.Estudiante;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO para respuesta completa de GET /estudiantes/{id}
 * Incluye todos los campos registrados en la base de datos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EstudianteResponseDTO {
    
    // Identificación
    private Integer idEstudiante;
    private Integer idSede;
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
    private String medicamentos;
    private Boolean certificadoMedicoDeportivo;
    
    // Día de pago
    private Integer diaPagoMes;
    
    // Información de emergencia
    private String nombreEmergencia;
    private String telefonoEmergencia;
    private String parentescoEmergencia;
    private String ocupacionEmergencia;
    private String correoEmergencia;
    
    // Poblaciones vulnerables
    private Boolean pertenecelgbtiq;
    private Boolean personaDiscapacidad;
    private String condicionDiscapacidad;
    private Boolean migranteRefugiado;
    private String poblacionEtnica;
    private String religion;
    
    // Información deportiva
    private String experienciaVoleibol;
    private String otrasDisciplinas;
    private String posicionPreferida;
    private String dominancia;
    private String nivelActual;
    private String clubesAnteriores;
    
    // Camiseta
    private String nombreCamiseta;
    private Integer numeroCamiseta;
    
    // Consentimiento y firma
    private Boolean aceptaConsentimiento;
    private String firmaDigital;
    private LocalDate fechaDiligenciamiento;
    
    // Fotos
    private String fotoUrl;
    private String fotoNombre;
    
    // Estado
    private Boolean estado;
    private String estadoPago;
    
    /**
     * Convierte una entidad Estudiante a EstudianteResponseDTO
     */
    public static EstudianteResponseDTO fromEntity(Estudiante estudiante) {
        if (estudiante == null) {
            return null;
        }
        
        return EstudianteResponseDTO.builder()
                // Identificación
                .idEstudiante(estudiante.getIdEstudiante())
                .idSede(estudiante.getSede() != null ? estudiante.getSede().getIdSede() : null)
                .nombreCompleto(estudiante.getNombreCompleto())
                .tipoDocumento(estudiante.getTipoDocumento() != null ? estudiante.getTipoDocumento().name() : null)
                .numeroDocumento(estudiante.getNumeroDocumento())
                .fechaNacimiento(estudiante.getFechaNacimiento())
                .edad(estudiante.getEdad())
                .sexo(estudiante.getSexo() != null ? estudiante.getSexo().name() : null)
                
                // Información de contacto
                .direccionResidencia(estudiante.getDireccionResidencia())
                .barrio(estudiante.getBarrio())
                .celularEstudiante(estudiante.getCelularEstudiante())
                .whatsappEstudiante(estudiante.getWhatsappEstudiante())
                .correoEstudiante(estudiante.getCorreoEstudiante())
                
                // Información del tutor
                .nombreTutor(estudiante.getNombreTutor())
                .parentescoTutor(estudiante.getParentescoTutor())
                .documentoTutor(estudiante.getDocumentoTutor())
                .telefonoTutor(estudiante.getTelefonoTutor())
                .correoTutor(estudiante.getCorreoTutor())
                .ocupacionTutor(estudiante.getOcupacionTutor())
                
                // Información académica
                .institucionEducativa(estudiante.getInstitucionEducativa())
                .jornada(estudiante.getJornada() != null ? estudiante.getJornada().name() : null)
                .gradoActual(estudiante.getGradoActual())
                
                // Información médica
                .eps(estudiante.getEps())
                .tipoSangre(estudiante.getTipoSangre())
                .alergias(estudiante.getAlergias())
                .enfermedadesCondiciones(estudiante.getEnfermedadesCondiciones())
                .medicamentos(estudiante.getMedicamentos())
                .certificadoMedicoDeportivo(estudiante.getCertificadoMedicoDeportivo())
                
                // Día de pago
                .diaPagoMes(estudiante.getDiaPagoMes())
                
                // Información de emergencia
                .nombreEmergencia(estudiante.getNombreEmergencia())
                .telefonoEmergencia(estudiante.getTelefonoEmergencia())
                .parentescoEmergencia(estudiante.getParentescoEmergencia())
                .ocupacionEmergencia(estudiante.getOcupacionEmergencia())
                .correoEmergencia(estudiante.getCorreoEmergencia())
                
                // Poblaciones vulnerables
                .pertenecelgbtiq(estudiante.getPertenecelgbtiq())
                .personaDiscapacidad(estudiante.getPersonaDiscapacidad())
                .condicionDiscapacidad(estudiante.getCondicionDiscapacidad())
                .migranteRefugiado(estudiante.getMigranteRefugiado())
                .poblacionEtnica(estudiante.getPoblacionEtnica())
                .religion(estudiante.getReligion())
                
                // Información deportiva
                .experienciaVoleibol(estudiante.getExperienciaVoleibol())
                .otrasDisciplinas(estudiante.getOtrasDisciplinas())
                .posicionPreferida(estudiante.getPosicionPreferida())
                .dominancia(estudiante.getDominancia() != null ? estudiante.getDominancia().name() : null)
                .nivelActual(estudiante.getNivelActual() != null ? estudiante.getNivelActual().name() : null)
                .clubesAnteriores(estudiante.getClubesAnteriores())
                
                // Camiseta
                .nombreCamiseta(estudiante.getNombreCamiseta())
                .numeroCamiseta(estudiante.getNumeroCamiseta())
                
                // Consentimiento y firma
                .aceptaConsentimiento(estudiante.getAceptaConsentimiento())
                .firmaDigital(estudiante.getFirmaDigital())
                .fechaDiligenciamiento(estudiante.getFechaDiligenciamiento())
                
                // Fotos
                .fotoUrl(estudiante.getFotoUrl())
                .fotoNombre(estudiante.getFotoNombre())
                
                // Estado
                .estado(estudiante.getEstado())
                .estadoPago(estudiante.getEstadoPago() != null ? estudiante.getEstadoPago().name() : null)
                
                .build();
    }
}
