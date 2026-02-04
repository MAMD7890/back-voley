package galacticos_app_back.galacticos.dto;

import java.time.LocalDate;

public class ProfesorAttendanceMarkRequest {
    private Integer idProfesor;
    private Integer idEquipo;
    private LocalDate fecha;
    private Boolean asistio = true;

    public Integer getIdProfesor() { return idProfesor; }
    public void setIdProfesor(Integer idProfesor) { this.idProfesor = idProfesor; }
    public Integer getIdEquipo() { return idEquipo; }
    public void setIdEquipo(Integer idEquipo) { this.idEquipo = idEquipo; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public Boolean getAsistio() { return asistio; }
    public void setAsistio(Boolean asistio) { this.asistio = asistio; }
}
