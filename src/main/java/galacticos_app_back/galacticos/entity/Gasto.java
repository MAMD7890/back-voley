package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "gasto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Gasto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idGasto;
    
    @Column(length = 100)
    private String concepto;
    
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal monto;
    
    @Column
    private LocalDate fecha;
    
    @ManyToOne
    @JoinColumn(name = "id_sede")
    private Sede sede;
}
