package galacticos_app_back.galacticos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GastoReporteDto {
    private Integer idGasto;
    private String concepto;
    private String descripcion;
    private BigDecimal monto;
    private LocalDate fecha;
    private String sedeName;

    public Integer getIdGasto() { return idGasto; }
    public void setIdGasto(Integer idGasto) { this.idGasto = idGasto; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getSedeName() { return sedeName; }
    public void setSedeName(String sedeName) { this.sedeName = sedeName; }
}
