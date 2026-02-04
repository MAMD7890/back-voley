package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "asistencia_estudiante")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaEstudiante {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAsistencia;
    
    @ManyToOne
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;
    
    @ManyToOne
    @JoinColumn(name = "id_equipo")
    private Equipo equipo;
    
    @Column
    private LocalDate fecha;
    
    @Column(columnDefinition = "BOOLEAN")
    private Boolean asistio;
    
    @Column(length = 200)
    private String observaciones;
}
