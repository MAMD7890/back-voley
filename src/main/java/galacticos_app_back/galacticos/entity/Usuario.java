package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idUsuario;
    
    @Column(length = 100)
    private String nombre;
    
    @Column(unique = true, nullable = false, length = 100)
    private String email;
    
    @Column(nullable = false, length = 255)
    private String password;
    
    @Column(length = 255)
    private String fotoUrl;
    
    @Column(length = 100)
    private String fotoNombre;
    
    @Column(length = 20)
    private String tipoDocumento;
    
    @Column(length = 50)
    private String numeroDocumento;
    
    @Column(length = 20)
    private String telefono;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean estado = true;
    
    @ManyToOne
    @JoinColumn(name = "id_rol")
    private Rol rol;
}
