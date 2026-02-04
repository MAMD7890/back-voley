package galacticos_app_back.galacticos.dto;

public class StudentExistenceDto {
    private Integer idEstudiante;
    private String nombreCompleto;
    private Boolean marcado;
    private String membershipStatus;

    public Integer getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(Integer idEstudiante) { this.idEstudiante = idEstudiante; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public Boolean getMarcado() { return marcado; }
    public void setMarcado(Boolean marcado) { this.marcado = marcado; }
    public String getMembershipStatus() { return membershipStatus; }
    public void setMembershipStatus(String membershipStatus) { this.membershipStatus = membershipStatus; }
}
