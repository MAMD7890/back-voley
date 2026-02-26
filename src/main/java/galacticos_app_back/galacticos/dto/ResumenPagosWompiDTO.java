package galacticos_app_back.galacticos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO con resumen estadístico de pagos Wompi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenPagosWompiDTO {
    
    // Totales
    private Long totalPagos;
    private Long pagosAprobados;
    private Long pagosPendientes;
    private Long pagosRechazados;
    
    // Montos
    private BigDecimal montoTotalRecaudado;
    private BigDecimal montoPendiente;
    private BigDecimal montoPromedioTransaccion;
    
    // Por método de pago
    private Long pagosOnline;
    private Long pagosEfectivo;
    private BigDecimal montoOnline;
    private BigDecimal montoEfectivo;
    
    // Estadísticas de estudiantes
    private Long estudiantesAlDia;
    private Long estudiantesEnMora;
    private Long estudiantesPendientes;
    private Long estudiantesConCompromiso;
    
    // Lista de últimos pagos
    private List<ReportePagoWompiDTO> ultimosPagos;
    
    // Pagos por mes (para gráficos)
    private List<PagosPorMesDTO> pagosPorMes;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagosPorMesDTO {
        private String mes;
        private Long cantidad;
        private BigDecimal monto;
    }
}
