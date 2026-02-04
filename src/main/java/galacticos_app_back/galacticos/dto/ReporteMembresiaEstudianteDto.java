package galacticos_app_back.galacticos.dto;

import java.math.BigDecimal;
import java.util.Map;

public class ReporteMembresiaEstudianteDto {
    private Integer totalEstudiantes;
    private Integer estudiantesAlDia;
    private Integer estudiantesEnMora;
    private Integer estudiantesSuspendidos;
    private BigDecimal porcentajeAlDia;
    private BigDecimal porcentajeEnMora;
    private BigDecimal porcentajeSuspendido;
    private Map<String, Object> detalleEstados;
    private String periodoDesde;
    private String periodoHasta;

    public Integer getTotalEstudiantes() { return totalEstudiantes; }
    public void setTotalEstudiantes(Integer totalEstudiantes) { this.totalEstudiantes = totalEstudiantes; }
    public Integer getEstudiantesAlDia() { return estudiantesAlDia; }
    public void setEstudiantesAlDia(Integer estudiantesAlDia) { this.estudiantesAlDia = estudiantesAlDia; }
    public Integer getEstudiantesEnMora() { return estudiantesEnMora; }
    public void setEstudiantesEnMora(Integer estudiantesEnMora) { this.estudiantesEnMora = estudiantesEnMora; }
    public Integer getEstudiantesSuspendidos() { return estudiantesSuspendidos; }
    public void setEstudiantesSuspendidos(Integer estudiantesSuspendidos) { this.estudiantesSuspendidos = estudiantesSuspendidos; }
    public BigDecimal getPorcentajeAlDia() { return porcentajeAlDia; }
    public void setPorcentajeAlDia(BigDecimal porcentajeAlDia) { this.porcentajeAlDia = porcentajeAlDia; }
    public BigDecimal getPorcentajeEnMora() { return porcentajeEnMora; }
    public void setPorcentajeEnMora(BigDecimal porcentajeEnMora) { this.porcentajeEnMora = porcentajeEnMora; }
    public BigDecimal getPorcentajeSuspendido() { return porcentajeSuspendido; }
    public void setPorcentajeSuspendido(BigDecimal porcentajeSuspendido) { this.porcentajeSuspendido = porcentajeSuspendido; }
    public Map<String, Object> getDetalleEstados() { return detalleEstados; }
    public void setDetalleEstados(Map<String, Object> detalleEstados) { this.detalleEstados = detalleEstados; }
    public String getPeriodoDesde() { return periodoDesde; }
    public void setPeriodoDesde(String periodoDesde) { this.periodoDesde = periodoDesde; }
    public String getPeriodoHasta() { return periodoHasta; }
    public void setPeriodoHasta(String periodoHasta) { this.periodoHasta = periodoHasta; }
}
