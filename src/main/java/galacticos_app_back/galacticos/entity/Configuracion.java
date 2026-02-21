package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad para configuración del sistema
 * Almacena valores como el precio de matrícula
 */
@Entity
@Table(name = "configuracion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Configuracion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idConfiguracion;
    
    /**
     * Clave única de la configuración (ej: PRECIO_MATRICULA, VALOR_INICIAL)
     */
    @Column(nullable = false, unique = true, length = 100)
    private String clave;
    
    /**
     * Descripción de la configuración
     */
    @Column(length = 255)
    private String descripcion;
    
    /**
     * Valor de la configuración (puede ser JSON, número, texto, etc.)
     */
    @Column(columnDefinition = "LONGTEXT")
    private String valor;
    
    /**
     * Tipo de valor (STRING, BIGDECIMAL, INTEGER, BOOLEAN, JSON)
     */
    @Column(length = 50)
    private String tipo;
    
    /**
     * Fecha de creación
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    
    /**
     * Fecha de última actualización
     */
    @Column
    private LocalDateTime fechaActualizacion;
    
    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
