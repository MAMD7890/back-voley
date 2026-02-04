package galacticos_app_back.galacticos.dto;

import java.math.BigDecimal;

public class ProfesorPagoDto {
    private Integer idProfesor;
    private String nombre;
    private Integer asistencias;
    private BigDecimal salarioPorClase;
    private BigDecimal totalPagar;

    public Integer getIdProfesor() { return idProfesor; }
    public void setIdProfesor(Integer idProfesor) { this.idProfesor = idProfesor; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getAsistencias() { return asistencias; }
    public void setAsistencias(Integer asistencias) { this.asistencias = asistencias; }
    public BigDecimal getSalarioPorClase() { return salarioPorClase; }
    public void setSalarioPorClase(BigDecimal salarioPorClase) { this.salarioPorClase = salarioPorClase; }
    public BigDecimal getTotalPagar() { return totalPagar; }
    public void setTotalPagar(BigDecimal totalPagar) { this.totalPagar = totalPagar; }
}
