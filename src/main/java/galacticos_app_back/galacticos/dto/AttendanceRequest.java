package galacticos_app_back.galacticos.dto;

import java.time.LocalDateTime;

public class AttendanceRequest {
    private Integer idEstudiante;
    private LocalDateTime fechaHora;
    private Boolean asistio = true;

    public Integer getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(Integer idEstudiante) { this.idEstudiante = idEstudiante; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public Boolean getAsistio() { return asistio == null || asistio; }
    public void setAsistio(Boolean asistio) { this.asistio = asistio; }
}
