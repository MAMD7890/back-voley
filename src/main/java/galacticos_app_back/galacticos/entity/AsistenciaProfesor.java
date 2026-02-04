package galacticos_app_back.galacticos.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "asistencia_profesor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaProfesor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAsistenciaProfesor;
    
    @ManyToOne
    @JoinColumn(name = "id_profesor")
    private Profesor profesor;
    
    @ManyToOne
    @JoinColumn(name = "id_equipo")
    private Equipo equipo;
    
    @Column
    private LocalDate fecha;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean asistio = true;
}
