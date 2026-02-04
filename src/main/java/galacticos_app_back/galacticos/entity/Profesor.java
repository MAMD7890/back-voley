package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "profesor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profesor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idProfesor;
    
    @Column(length = 100)
    private String nombre;
    
    @Column(length = 50)
    private String documento;
    
    @Column(length = 20)
    private String telefono;
    
    @Column(length = 100)
    private String correo;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal salarioPorClase;
    
    @Column(length = 255)
    private String fotoUrl;
    
    @Column(length = 100)
    private String fotoNombre;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean estado = true;
}
