package galacticos_app_back.galacticos.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Registro de un abono de cartera: dinero que el estudiante paga para ponerse
 * a paz y salvo con deudas/mora, sin que esto extienda ni cree una membresía.
 */
@Data
@NoArgsConstructor
public class RegistroAcuerdoCarteraDTO {

    private Integer idEstudiante;
    private BigDecimal valor;
    private String observacion;
}
