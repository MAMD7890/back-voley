package galacticos_app_back.galacticos.dto;

import galacticos_app_back.galacticos.entity.Estudiante;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * DTO para cambiar el estado de pago de un estudiante manualmente
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambioEstadoPagoDTO {
    
    /**
     * Nuevo estado de pago del estudiante
     * Valores válidos: PENDIENTE, AL_DIA, EN_MORA, COMPROMISO_PAGO
     */
    private Estudiante.EstadoPago nuevoEstado;
    
    /**
     * Observación o motivo del cambio (opcional)
     */
    private String observacion;
    
    /**
     * ID del mes pagado si aplica (formato: "ENERO_2026", "FEBRERO_2026", etc.)
     */
    private String mesPagado;

    /**
     * Fecha límite del acuerdo de pago. Solo aplica cuando nuevoEstado = COMPROMISO_PAGO.
     * Si llega esta fecha sin cambio a AL_DIA, el sistema cambia el estado a EN_MORA automáticamente.
     */
    private LocalDate fechaLimiteCompromiso;

    /**
     * Inicio del período del acuerdo. Opcional: si no se envía, se usa la fechaFin
     * de la última FINALIZADA del estudiante.
     */
    private LocalDate fechaInicio;

    /**
     * Fin del período del acuerdo. Opcional: si no se envía, se calcula 1 mes desde fechaInicio.
     */
    private LocalDate fechaFin;
}
