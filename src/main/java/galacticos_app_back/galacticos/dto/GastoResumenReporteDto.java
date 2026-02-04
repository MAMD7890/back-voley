package galacticos_app_back.galacticos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GastoResumenReporteDto {
    private List<GastoReporteDto> gastos;
    private BigDecimal totalGastos;
    private GastoReporteDto gastoMayor;
    private LocalDate diaMaxGasto;
    private BigDecimal totalDiaMaxGasto;
    private Integer cantidadGastos;

    public List<GastoReporteDto> getGastos() { return gastos; }
    public void setGastos(List<GastoReporteDto> gastos) { this.gastos = gastos; }
    public BigDecimal getTotalGastos() { return totalGastos; }
    public void setTotalGastos(BigDecimal totalGastos) { this.totalGastos = totalGastos; }
    public GastoReporteDto getGastoMayor() { return gastoMayor; }
    public void setGastoMayor(GastoReporteDto gastoMayor) { this.gastoMayor = gastoMayor; }
    public LocalDate getDiaMaxGasto() { return diaMaxGasto; }
    public void setDiaMaxGasto(LocalDate diaMaxGasto) { this.diaMaxGasto = diaMaxGasto; }
    public BigDecimal getTotalDiaMaxGasto() { return totalDiaMaxGasto; }
    public void setTotalDiaMaxGasto(BigDecimal totalDiaMaxGasto) { this.totalDiaMaxGasto = totalDiaMaxGasto; }
    public Integer getCantidadGastos() { return cantidadGastos; }
    public void setCantidadGastos(Integer cantidadGastos) { this.cantidadGastos = cantidadGastos; }
}
