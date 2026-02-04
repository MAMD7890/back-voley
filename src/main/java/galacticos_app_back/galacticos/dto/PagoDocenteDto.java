package galacticos_app_back.galacticos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PagoDocenteDto {
    private Integer idProfesor;
    private String nombreProfesor;
    private Integer asistencias;
    private BigDecimal salarioPorClase;
    private BigDecimal totalCalculado;
    private LocalDate periodoDesde;
    private LocalDate periodoHasta;

    public Integer getIdProfesor() { return idProfesor; }
    public void setIdProfesor(Integer idProfesor) { this.idProfesor = idProfesor; }
    public String getNombreProfesor() { return nombreProfesor; }
    public void setNombreProfesor(String nombreProfesor) { this.nombreProfesor = nombreProfesor; }
    public Integer getAsistencias() { return asistencias; }
    public void setAsistencias(Integer asistencias) { this.asistencias = asistencias; }
    public BigDecimal getSalarioPorClase() { return salarioPorClase; }
    public void setSalarioPorClase(BigDecimal salarioPorClase) { this.salarioPorClase = salarioPorClase; }
    public BigDecimal getTotalCalculado() { return totalCalculado; }
    public void setTotalCalculado(BigDecimal totalCalculado) { this.totalCalculado = totalCalculado; }
    public LocalDate getPeriodoDesde() { return periodoDesde; }
    public void setPeriodoDesde(LocalDate periodoDesde) { this.periodoDesde = periodoDesde; }
    public LocalDate getPeriodoHasta() { return periodoHasta; }
    public void setPeriodoHasta(LocalDate periodoHasta) { this.periodoHasta = periodoHasta; }
}
