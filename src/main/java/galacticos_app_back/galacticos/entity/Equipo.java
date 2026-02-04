package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "equipo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Equipo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEquipo;
    
    @Column(length = 100)
    private String nombre;
    
    @ManyToOne
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;
    
    @ManyToOne
    @JoinColumn(name = "id_sede")
    private Sede sede;
    
    @Column(length = 100)
    private String horario;
    
    @Column(length = 255)
    private String fotoUrl;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean estado = true;
}
