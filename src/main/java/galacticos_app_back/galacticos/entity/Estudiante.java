package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "estudiante")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Estudiante {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEstudiante;
    @Column(nullable = false, length = 200)
    private String nombreCompleto;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDocumento tipoDocumento;
    
    @Column(nullable = false, length = 20)
    private String numeroDocumento;
    
    @Column(nullable = false)
    private LocalDate fechaNacimiento;
    
    @Column(nullable = false)
    private Integer edad;
    
    @Enumerated(EnumType.STRING)
    private Sexo sexo;
    
    @Column(length = 200)
    private String direccionResidencia;
    
    @Column(length = 100)
    private String barrio;
    
    @Column(length = 20)
    private String celularEstudiante;
    
    @Column(length = 20)
    private String whatsappEstudiante;
    
    @Column(length = 100)
    private String correoEstudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_sede")
    private Sede sede;

    @Column(length = 200)
    private String nombreTutor;
    
    @Column(length = 50)
    private String parentescoTutor;
    
    @Column(length = 20)
    private String documentoTutor;
    
    @Column(length = 20)
    private String telefonoTutor;
    
    @Column(length = 100)
    private String correoTutor;
    
    @Column(length = 100)
    private String ocupacionTutor;

    @Column(length = 150)
    private String institucionEducativa;
    
    @Enumerated(EnumType.STRING)
    private Jornada jornada;
    
    private Integer gradoActual;

    @Column(length = 100)
    private String eps;
    
    @Column(length = 20)
    private String tipoSangre;
    
    @Column(columnDefinition = "TEXT")
    private String alergias;
    
    @Column(columnDefinition = "TEXT")
    private String enfermedadesCondiciones;
    
    @Column(columnDefinition = "TEXT")
    private String medicamentos;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean certificadoMedicoDeportivo = false;
    
    private Integer diaPagoMes;

    @Column(length = 200)
    private String nombreEmergencia;
    
    @Column(length = 20)
    private String telefonoEmergencia;
    
    @Column(length = 50)
    private String parentescoEmergencia;
    
    @Column(length = 100)
    private String ocupacionEmergencia;
    
    @Column(length = 100)
    private String correoEmergencia;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean pertenecelgbtiq = false;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean personaDiscapacidad = false;
    
    @Column(columnDefinition = "TEXT")
    private String condicionDiscapacidad;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean migranteRefugiado = false;
    
    @Column(length = 100)
    private String poblacionEtnica;
    
    @Column(length = 100)
    private String religion;

    @Column(columnDefinition = "TEXT")
    private String experienciaVoleibol;
    
    @Column(columnDefinition = "TEXT")
    private String otrasDisciplinas;
    
    @Column(length = 50)
    private String posicionPreferida;
    
    @Enumerated(EnumType.STRING)
    private Dominancia dominancia;
    
    @Enumerated(EnumType.STRING)
    private NivelActual nivelActual;
    
    @Column(columnDefinition = "TEXT")
    private String clubesAnteriores;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean aceptaConsentimiento = false;
    
    @Column(length = 200)
    private String firmaDigital;
    
    private LocalDate fechaDiligenciamiento;

    @Column(length = 50)
    private String nombreCamiseta;
    
    private Integer numeroCamiseta;

    @Column(length = 255)
    private String fotoUrl;
    
    @Column(length = 100)
    private String fotoNombre;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean estado = true;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoPago estadoPago = EstadoPago.PENDIENTE;
    
    public enum TipoDocumento {
        TI, CC, RC, PASAPORTE
    }
    
    /**
     * Estados de pago del estudiante:
     * - PENDIENTE: Estado inicial al registrar estudiante, no ha realizado ningún pago
     * - AL_DIA: Pago realizado dentro del período correspondiente
     * - EN_MORA: No realizó el pago antes de la fecha límite del mes
     * - COMPROMISO_PAGO: Acuerdo de pago posterior, establecido manualmente
     */
    public enum EstadoPago {
        PENDIENTE,
        AL_DIA,
        EN_MORA,
        COMPROMISO_PAGO
    }
    
    public enum Sexo {
        MASCULINO, FEMENINO, OTRO
    }
    
    public enum Jornada {
        MAÑANA, TARDE, NOCHE, UNICA
    }
    
    public enum Dominancia {
        DERECHA, IZQUIERDA, AMBIDIESTRO
    }
    
    public enum NivelActual {
        INICIANTE, INTERMEDIO, AVANZADO
    }
}
