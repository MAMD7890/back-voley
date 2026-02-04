package galacticos_app_back.galacticos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualización parcial de datos del estudiante.
 * Todos los campos son opcionales - solo se actualizarán los campos enviados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarEstudianteDTO {
    
    @Size(max = 200, message = "El nombre completo no puede exceder 200 caracteres")
    private String nombreCompleto;
    
    @Size(max = 20, message = "El celular no puede exceder 20 caracteres")
    private String celularEstudiante;
    
    @Size(max = 20, message = "El WhatsApp no puede exceder 20 caracteres")
    private String whatsappEstudiante;
    
    @Email(message = "El correo debe tener un formato válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correoEstudiante;
    
    @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
    private String direccionResidencia;
    
    @Size(max = 100, message = "El barrio no puede exceder 100 caracteres")
    private String barrio;
    
    @Size(max = 100, message = "La EPS no puede exceder 100 caracteres")
    private String eps;
    
    @Size(max = 5, message = "El tipo de sangre no puede exceder 5 caracteres")
    private String tipoSangre;
    
    private String alergias;
    
    private String enfermedadesCondiciones;
    
    private String medicamentos;
    
    private Boolean certificadoMedicoDeportivo;
    
    // Datos del tutor
    @Size(max = 200, message = "El nombre del tutor no puede exceder 200 caracteres")
    private String nombreTutor;
    
    @Size(max = 50, message = "El parentesco del tutor no puede exceder 50 caracteres")
    private String parentescoTutor;
    
    @Size(max = 20, message = "El documento del tutor no puede exceder 20 caracteres")
    private String documentoTutor;
    
    @Size(max = 20, message = "El teléfono del tutor no puede exceder 20 caracteres")
    private String telefonoTutor;
    
    @Email(message = "El correo del tutor debe tener un formato válido")
    @Size(max = 100, message = "El correo del tutor no puede exceder 100 caracteres")
    private String correoTutor;
    
    @Size(max = 100, message = "La ocupación del tutor no puede exceder 100 caracteres")
    private String ocupacionTutor;
    
    // Contacto de emergencia
    @Size(max = 200, message = "El nombre de emergencia no puede exceder 200 caracteres")
    private String nombreEmergencia;
    
    @Size(max = 20, message = "El teléfono de emergencia no puede exceder 20 caracteres")
    private String telefonoEmergencia;
    
    @Size(max = 50, message = "El parentesco de emergencia no puede exceder 50 caracteres")
    private String parentescoEmergencia;
    
    // Datos de camiseta
    @Size(max = 50, message = "El nombre de camiseta no puede exceder 50 caracteres")
    private String nombreCamiseta;
    
    private Integer numeroCamiseta;
}
