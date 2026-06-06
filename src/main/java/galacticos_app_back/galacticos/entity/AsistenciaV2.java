package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "asistencia_v2",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_sesion", "id_estudiante"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaV2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAsistencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sesion", nullable = false)
    private SesionAsistencia sesion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Column(nullable = false)
    private Boolean asistio = false;

    @Column(length = 300)
    private String observaciones;
}
