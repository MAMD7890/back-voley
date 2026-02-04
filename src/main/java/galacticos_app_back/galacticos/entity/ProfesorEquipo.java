package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "profesor_equipo", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id_profesor", "id_equipo"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfesorEquipo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idProfesorEquipo;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_profesor", nullable = false)
    private Profesor profesor;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_equipo", nullable = false)
    private Equipo equipo;
    
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('PRINCIPAL','ASISTENTE') DEFAULT 'PRINCIPAL'")
    private RolProfesor rol = RolProfesor.PRINCIPAL;
    
    public enum RolProfesor {
        PRINCIPAL,
        ASISTENTE
    }
}
