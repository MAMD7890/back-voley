package galacticos_app_back.galacticos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * DTO para mapear datos del Excel de inscripción de estudiantes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelEstudianteImportDTO {
    
    // Información personal del estudiante
    private String nombreCompleto;
    private String tipoDocumento;
    private String numeroDocumento;
    private LocalDate fechaNacimiento;
    private Integer edad;
    private String sexo;
    
    // Información de contacto estudiante
    private String direccionResidencia;
    private String barrio;
    private String celularEstudiante;
    private String whatsappEstudiante;
    private String correoEstudiante;
    
    // Información de la sede
    private String nombreSede;
    
    // Información del tutor/padre
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
    
    // Información de pagos
    private Integer diaPagoMes;
    
    // Información contacto emergencia
    private String nombreEmergencia;
    private String telefonoEmergencia;
    private String parentescoEmergencia;
    private String ocupacionEmergencia;
    private String correoEmergencia;
    
    // Información sobre poblaciones vulnerables
    private Boolean perteneceIgbtiq;
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
    
    // Consentimiento informado
    private Boolean consentimientoInformado;
    private String firmaDigital;
    private LocalDate fechaDiligenciamiento;
    
    // Información para validación y reporte
    private Integer numeroFila;
    private String erroresValidacion;
    private Boolean procesadoExitosamente;
}
