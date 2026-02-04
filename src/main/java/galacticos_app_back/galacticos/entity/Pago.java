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
    
    public enum MetodoPago {
        ONLINE, EFECTIVO
    }
    
    public enum EstadoPago {
        PAGADO, PENDIENTE, VENCIDO
    }
}
