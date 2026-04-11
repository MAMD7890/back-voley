package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "recordatorio_pago",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"id_membresia", "tipo_recordatorio", "fecha_vencimiento_referencia"}
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordatorioPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idRecordatorio;

    @ManyToOne
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;

    @ManyToOne
    @JoinColumn(name = "id_membresia")
    private Membresia membresia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_recordatorio", length = 30)
    private TipoRecordatorio tipoRecordatorio;

    @Column(name = "fecha_vencimiento_referencia")
    private LocalDate fechaVencimientoReferencia;

    @Column(columnDefinition = "DATETIME")
    private LocalDateTime fechaEnvio;

    @Column(length = 500)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoEnvio estadoEnvio;

    @Column(length = 100)
    private String twilioMessageSid;

    @Column(columnDefinition = "TEXT")
    private String errorDetalle;

    private Integer intentos;

    /**
     * Tipos de recordatorio según los días respecto a la fecha de vencimiento.
     *
     * ANTES del vencimiento (membresía aún activa):
     *   CINCO_DIAS_ANTES  (-5): Aviso preventivo temprano
     *   DOS_DIAS_ANTES    (-2): Recordatorio urgente
     *   DIA_VENCIMIENTO   ( 0): Último día, membresía vence hoy
     *
     * DESPUÉS del vencimiento (membresía vencida / en mora):
     *   UN_DIA_DESPUES    (+1): Primer aviso de mora
     *   DOS_DIAS_DESPUES  (+2): Segundo aviso de mora
     *   TRES_DIAS_DESPUES (+3): Aviso final, contactar profesor
     */
    public enum TipoRecordatorio {
        CINCO_DIAS_ANTES(-5, "5 días antes del vencimiento"),
        DOS_DIAS_ANTES(-2,   "2 días antes del vencimiento"),
        DIA_VENCIMIENTO(0,   "Día del vencimiento"),
        UN_DIA_DESPUES(1,    "1 día después del vencimiento"),
        DOS_DIAS_DESPUES(2,  "2 días después del vencimiento"),
        TRES_DIAS_DESPUES(3, "3 días después del vencimiento");

        private final int diasDiferencia;
        private final String descripcion;

        TipoRecordatorio(int diasDiferencia, String descripcion) {
            this.diasDiferencia = diasDiferencia;
            this.descripcion = descripcion;
        }

        public int getDiasDiferencia() { return diasDiferencia; }
        public String getDescripcion() { return descripcion; }

        public static TipoRecordatorio fromDiasDiferencia(int dias) {
            for (TipoRecordatorio tipo : values()) {
                if (tipo.diasDiferencia == dias) return tipo;
            }
            return null;
        }
    }

    public enum EstadoEnvio {
        ENVIADO,
        FALLIDO,
        PENDIENTE
    }
}
