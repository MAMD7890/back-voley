package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "membresia_core")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembresiaCore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMembresiaCore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_equipo")
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plan")
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago_origen")
    private Pago pagoOrigen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMembresia tipoMembresia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMembresia estadoMembresia;

    @Column
    private LocalDate fechaInicio;

    @Column
    private LocalDate fechaFin;

    @Column(precision = 10, scale = 2)
    private BigDecimal valorMensual;

    @Column(length = 500)
    private String observacion;

    @Column
    private LocalDate fechaLimiteCompromiso;

    @Column
    private LocalDate fechaLimiteGracia;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrigenAcuerdo origenAcuerdo;

    @Column
    private LocalDateTime fechaCreacion;

    @Column
    private LocalDateTime fechaUltimoCambio;

    @Column(length = 200)
    private String motivoCambio;

    /**
     * Marca la membresía que representa el período actual del estudiante.
     * Solo UNA membresía por estudiante puede tener esActiva = true.
     * Las PAGADA futuras (pago anticipado) tienen esActiva = false hasta que
     * Job 1 las activa. Las FINALIZADAS y CANCELADAS siempre tienen esActiva = false.
     */
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean esActiva = false;

    @PrePersist
    private void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.fechaUltimoCambio == null) {
            this.fechaUltimoCambio = LocalDateTime.now();
        }
    }

    public enum TipoMembresia {
        ONLINE,
        EFECTIVO,
        ACUERDO_PAGO,
        MORA,
        PENDIENTE_REGISTRO
    }

    public enum EstadoMembresia {
        PENDIENTE_PAGO,
        EN_MORA,
        PAGADA,
        FINALIZADA,
        CANCELADA
    }

    public enum OrigenAcuerdo {
        DESDE_PENDIENTE,
        DESDE_MORA
    }
}
