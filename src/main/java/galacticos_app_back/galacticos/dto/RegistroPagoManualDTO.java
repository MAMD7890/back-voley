package galacticos_app_back.galacticos.dto;

import galacticos_app_back.galacticos.entity.Pago;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class RegistroPagoManualDTO {

    private Integer idEstudiante;
    private BigDecimal valor;
    private Pago.MetodoPago metodoPago;  // EFECTIVO o TRANSFERENCIA
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String observacion;
    private String mesPagado;
}
