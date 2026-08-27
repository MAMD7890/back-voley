package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

@Entity
@Table(name = "pago")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPago;
    
    @ManyToOne
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;
    
    @Column(length = 20)
    private String mesPagado;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal valor;
    
    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago;
    
    @Column(length = 100)
    private String referenciaPago;
    
    @Column
    private LocalDate fechaPago;
    
    @Column
    private LocalTime horaPago;
    
    @Enumerated(EnumType.STRING)
    private EstadoPago estadoPago;
    
    @Column(length = 100)
    private String wompiTransactionId;

    @Column(length = 255)
    private String observacion;

    public enum MetodoPago {
        ONLINE, EFECTIVO, TRANSFERENCIA,
        /**
         * Abono para ponerse a paz y salvo con deudas/cartera vencida.
         * A diferencia de los demás métodos, NUNCA debe usarse para crear,
         * extender o recalcular una membresía (ver PagoRepository: las
         * queries de reconciliación de membresías lo excluyen a propósito).
         */
        ACUERDO_CARTERA
    }
    
    public enum EstadoPago {
        PAGADO, PENDIENTE, VENCIDO, RECHAZADO
    }
}
