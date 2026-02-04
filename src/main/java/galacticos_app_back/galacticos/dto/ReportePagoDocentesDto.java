package galacticos_app_back.galacticos.dto;

import java.math.BigDecimal;
import java.util.List;

public class ReportePagoDocentesDto {
    private List<PagoDocenteDto> pagos;
    private BigDecimal totalAPagar;
    private Integer cantidadDocentes;
    private BigDecimal salarioPromedio;

    public List<PagoDocenteDto> getPagos() { return pagos; }
    public void setPagos(List<PagoDocenteDto> pagos) { this.pagos = pagos; }
    public BigDecimal getTotalAPagar() { return totalAPagar; }
    public void setTotalAPagar(BigDecimal totalAPagar) { this.totalAPagar = totalAPagar; }
    public Integer getCantidadDocentes() { return cantidadDocentes; }
    public void setCantidadDocentes(Integer cantidadDocentes) { this.cantidadDocentes = cantidadDocentes; }
    public BigDecimal getSalarioPromedio() { return salarioPromedio; }
    public void setSalarioPromedio(BigDecimal salarioPromedio) { this.salarioPromedio = salarioPromedio; }
}
