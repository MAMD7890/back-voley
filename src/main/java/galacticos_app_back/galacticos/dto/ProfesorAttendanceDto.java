package galacticos_app_back.galacticos.dto;

import java.time.LocalDate;
import java.math.BigDecimal;

public class ProfesorAttendanceDto {
    private Integer idProfesor;
    private String nombre;
    private Boolean asistio;
    private LocalDate fechaAsistencia;
    private BigDecimal salarioPorClase;

    public Integer getIdProfesor() { return idProfesor; }
    public void setIdProfesor(Integer idProfesor) { this.idProfesor = idProfesor; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Boolean getAsistio() { return asistio; }
    public void setAsistio(Boolean asistio) { this.asistio = asistio; }
    public LocalDate getFechaAsistencia() { return fechaAsistencia; }
    public void setFechaAsistencia(LocalDate fechaAsistencia) { this.fechaAsistencia = fechaAsistencia; }
    public BigDecimal getSalarioPorClase() { return salarioPorClase; }
    public void setSalarioPorClase(BigDecimal salarioPorClase) { this.salarioPorClase = salarioPorClase; }
}
