package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "membresia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Membresia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMembresia;
    
    @ManyToOne
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;
    
    @ManyToOne
    @JoinColumn(name = "id_equipo")
    private Equipo equipo;
    
    @Column
    private LocalDate fechaInicio;
    
    @Column
    private LocalDate fechaFin;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal valorMensual;
    
    @Column
    private Boolean estado;
    
    // Estados como constantes estáticas para referencia
    public static final Boolean ESTADO_ACTIVO = true;
    public static final Boolean ESTADO_INACTIVO = false;
}
