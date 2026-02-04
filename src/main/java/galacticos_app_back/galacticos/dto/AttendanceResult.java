package galacticos_app_back.galacticos.dto;

public class AttendanceResult {
    private Integer idEstudiante;
    private Boolean existio;
    private Boolean alreadyMarked;
    private Boolean saved;
    private String membershipStatus;
    private String nombreCompleto;

    public Integer getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(Integer idEstudiante) { this.idEstudiante = idEstudiante; }
    public Boolean getExistio() { return existio; }
    public void setExistio(Boolean existio) { this.existio = existio; }
    public Boolean getAlreadyMarked() { return alreadyMarked; }
    public void setAlreadyMarked(Boolean alreadyMarked) { this.alreadyMarked = alreadyMarked; }
    public Boolean getSaved() { return saved; }
    public void setSaved(Boolean saved) { this.saved = saved; }
    public String getMembershipStatus() { return membershipStatus; }
    public void setMembershipStatus(String membershipStatus) { this.membershipStatus = membershipStatus; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
}
