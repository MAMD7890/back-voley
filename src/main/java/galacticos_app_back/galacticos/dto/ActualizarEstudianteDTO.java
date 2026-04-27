package galacticos_app_back.galacticos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * DTO para actualización parcial de datos del estudiante.
 * Todos los campos son opcionales — solo se actualizan los que no sean null.
 * Campos alineados con el Excel de inscripción de 33 columnas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarEstudianteDTO {

    // ── Información personal ─────────────────────────────────────────────────
    @Size(max = 200)
    private String nombreCompleto;

    private String tipoDocumento;

    @Size(max = 20)
    private String numeroDocumento;

    private LocalDate fechaNacimiento;

    private Integer edad;

    private String sexo;

    // ── Contacto del estudiante ──────────────────────────────────────────────
    @Size(max = 200)
    private String direccionResidencia;

    @Size(max = 100)
    private String barrio;

    @Size(max = 20)
    private String celularEstudiante;

    @Size(max = 20)
    private String whatsappEstudiante;

    @Email
    @Size(max = 100)
    private String correoEstudiante;

    // ── Tutor / padre / madre ────────────────────────────────────────────────
    @Size(max = 200)
    private String nombreTutor;

    @Size(max = 50)
    private String parentescoTutor;

    @Size(max = 20)
    private String documentoTutor;

    @Size(max = 20)
    private String telefonoTutor;

    @Email
    @Size(max = 100)
    private String correoTutor;

    @Size(max = 100)
    private String ocupacionTutor;

    // ── Información académica ────────────────────────────────────────────────
    @Size(max = 150)
    private String institucionEducativa;

    private String jornada;

    private Integer gradoActual;

    // ── Información médica ───────────────────────────────────────────────────
    @Size(max = 100)
    private String eps;

    @Size(max = 20)
    private String tipoSangre;

    private String alergias;

    private String enfermedadesCondiciones;

    // ── Pagos ────────────────────────────────────────────────────────────────
    private Integer diaPagoMes;

    // ── Contacto de emergencia ───────────────────────────────────────────────
    @Size(max = 200)
    private String nombreEmergencia;

    @Size(max = 20)
    private String telefonoEmergencia;

    @Size(max = 50)
    private String parentescoEmergencia;

    // ── Poblaciones vulnerables ──────────────────────────────────────────────
    private Boolean personaDiscapacidad;

    private String condicionDiscapacidad;

    private Boolean migranteRefugiado;

    @Size(max = 100)
    private String poblacionEtnica;

    // ── Estado ──────────────────────────────────────────────────────────────
    private String estadoPago;

    // ── Datos extra de la app (no están en el Excel pero se editan en UI) ────
    @Size(max = 50)
    private String nombreCamiseta;

    private Integer numeroCamiseta;

    /** ID de la sede para reasignar al estudiante */
    private Integer sedeId;
}
