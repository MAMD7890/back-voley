package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plan")
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago_origen")
    private Pago pagoOrigen;

    @Column
    private LocalDateTime fechaCreacion;

    @Column
    private LocalDateTime fechaUltimoCambio;

    @Column(length = 200)
    private String motivoCambio;

    // Estados como constantes estáticas para referencia
    public static final Boolean ESTADO_ACTIVO = true;
    public static final Boolean ESTADO_INACTIVO = false;
}
