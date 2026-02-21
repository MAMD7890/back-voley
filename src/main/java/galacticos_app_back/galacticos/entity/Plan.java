package galacticos_app_back.galacticos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Plan de precios para membresías
 * Representa los planes disponibles para estudiantes (1, 2, 3 meses, etc.)
 */
@Entity
@Table(name = "plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPlan;
    
    /**
     * Nombre del plan (ej: "Plan 1 mes", "Plan 2 meses", "Plan 3 meses")
     */
    @Column(nullable = false, length = 100)
    private String nombre;
    
    /**
     * Descripción del plan
     */
    @Column(length = 255)
    private String descripcion;
    
    /**
     * Duración en meses
     */
    @Column(nullable = false)
    private Integer duracionMeses;
    
    /**
     * Precio total del plan
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;
    
    /**
     * Precio mensual (precio / duracionMeses)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal precioMensual;
    
    /**
     * Descripción corta que aparece en la tarjeta
     */
    @Column(length = 255)
    private String descripcionCorta;
    
    /**
     * Indica si el plan está activo
     */
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean activo = true;
    
    /**
     * Indica si es el plan más popular (mostrar badge)
     */
    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean masPopular = false;
    
    /**
     * Orden de visualización en la UI (menor = primero)
     */
    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer ordenVisualizacion = 0;
    
    /**
     * Precio de matrícula asociado al plan
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal precioMatricula;
    
    /**
     * Descripción de la matrícula
     */
    @Column(length = 255)
    private String descripcionMatricula;
    
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
