package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "membresia_historial")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembresiaHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idHistorial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_membresia")
    private Membresia membresia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;

    @Column
    private LocalDate fechaInicioAnterior;

    @Column
    private LocalDate fechaFinAnterior;

    @Column
    private LocalDate fechaInicioNueva;

    @Column
    private LocalDate fechaFinNueva;

    @Column
    private Boolean estadoAnterior;

    @Column
    private Boolean estadoNuevo;

    @Column(length = 200)
    private String motivo;

    @Column(length = 100)
    private String origen;

    @Column
    private LocalDateTime fechaCambio;
}
