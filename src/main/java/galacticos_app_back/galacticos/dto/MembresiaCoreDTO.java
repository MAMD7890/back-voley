package galacticos_app_back.galacticos.dto;

import galacticos_app_back.galacticos.entity.MembresiaCore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MembresiaCoreDTO {

    private Integer idMembresiaCore;

    // Estudiante (aplanado)
    private Integer idEstudiante;
    private String nombreEstudiante;

    // Equipo (aplanado)
    private Integer idEquipo;
    private String nombreEquipo;

    // Plan (aplanado)
    private Integer idPlan;
    private String nombrePlan;

    // Pago origen (aplanado)
    private Integer idPagoOrigen;
    private BigDecimal valorPagoOrigen;
    private LocalDate fechaPagoOrigen;
    private String metodoPagoOrigen;
    private String mesPagadoOrigen;
    private String observacionPago;

    // Campos propios de MembresiaCore
    private MembresiaCore.TipoMembresia tipoMembresia;
    private MembresiaCore.EstadoMembresia estadoMembresia;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal valorMensual;
    private String observacion;
    private LocalDate fechaLimiteCompromiso;
    private LocalDate fechaLimiteGracia;
    private MembresiaCore.OrigenAcuerdo origenAcuerdo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaUltimoCambio;
    private String motivoCambio;
    private Boolean esActiva;

    public static MembresiaCoreDTO from(MembresiaCore m) {
        MembresiaCoreDTO dto = new MembresiaCoreDTO();
        dto.setIdMembresiaCore(m.getIdMembresiaCore());

        if (m.getEstudiante() != null) {
            dto.setIdEstudiante(m.getEstudiante().getIdEstudiante());
            dto.setNombreEstudiante(m.getEstudiante().getNombreCompleto());
        }
        if (m.getEquipo() != null) {
            dto.setIdEquipo(m.getEquipo().getIdEquipo());
            dto.setNombreEquipo(m.getEquipo().getNombre());
        }
        if (m.getPlan() != null) {
            dto.setIdPlan(m.getPlan().getIdPlan());
            dto.setNombrePlan(m.getPlan().getNombre());
        }
        if (m.getPagoOrigen() != null) {
            dto.setIdPagoOrigen(m.getPagoOrigen().getIdPago());
            dto.setValorPagoOrigen(m.getPagoOrigen().getValor());
            dto.setFechaPagoOrigen(m.getPagoOrigen().getFechaPago());
            if (m.getPagoOrigen().getMetodoPago() != null) {
                dto.setMetodoPagoOrigen(m.getPagoOrigen().getMetodoPago().name());
            }
            dto.setMesPagadoOrigen(m.getPagoOrigen().getMesPagado());
            dto.setObservacionPago(m.getPagoOrigen().getObservacion());
        }

        dto.setTipoMembresia(m.getTipoMembresia());
        dto.setEstadoMembresia(m.getEstadoMembresia());
        dto.setFechaInicio(m.getFechaInicio());
        dto.setFechaFin(m.getFechaFin());
        dto.setValorMensual(m.getValorMensual());
        dto.setObservacion(m.getObservacion());
        dto.setFechaLimiteCompromiso(m.getFechaLimiteCompromiso());
        dto.setFechaLimiteGracia(m.getFechaLimiteGracia());
        dto.setOrigenAcuerdo(m.getOrigenAcuerdo());
        dto.setFechaCreacion(m.getFechaCreacion());
        dto.setFechaUltimoCambio(m.getFechaUltimoCambio());
        dto.setMotivoCambio(m.getMotivoCambio());
        dto.setEsActiva(m.getEsActiva());
        return dto;
    }
}
