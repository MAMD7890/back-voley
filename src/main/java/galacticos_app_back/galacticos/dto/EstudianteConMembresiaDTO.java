package galacticos_app_back.galacticos.dto;

import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.MembresiaCore;
import galacticos_app_back.galacticos.entity.Sede;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstudianteConMembresiaDTO {

    private Integer idEstudiante;
    private String nombreCompleto;
    private String tipoDocumento;
    private String numeroDocumento;
    private String correoEstudiante;
    private String celularEstudiante;
    private String whatsappEstudiante;
    private Boolean estado;
    private Estudiante.EstadoPago estadoPago;
    private String observacionPago;
    private String fotoUrl;
    private Integer diaPago;
    private LocalDate fechaRegistro;

    private Integer idSede;
    private String nombreSede;

    private MembresiaResumen membresiaActiva;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MembresiaResumen {
        private Integer idMembresiaCore;
        private String tipoMembresia;
        private String estadoMembresia;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private LocalDate fechaLimiteGracia;
        private LocalDate fechaLimiteCompromiso;
        private BigDecimal valorMensual;
        private String observacion;
        private String mesPagado;
        private Long diasRestantes;
        private Boolean esActiva;
    }

    public static EstudianteConMembresiaDTO of(Estudiante est, MembresiaCore mc) {
        EstudianteConMembresiaDTO dto = new EstudianteConMembresiaDTO();
        dto.setIdEstudiante(est.getIdEstudiante());
        dto.setNombreCompleto(est.getNombreCompleto());
        dto.setTipoDocumento(est.getTipoDocumento() != null ? est.getTipoDocumento().name() : null);
        dto.setNumeroDocumento(est.getNumeroDocumento());
        dto.setCorreoEstudiante(est.getCorreoEstudiante());
        dto.setCelularEstudiante(est.getCelularEstudiante());
        dto.setWhatsappEstudiante(est.getWhatsappEstudiante());
        dto.setEstado(est.getEstado());
        dto.setEstadoPago(est.getEstadoPago());
        dto.setObservacionPago(est.getObservacionPago());
        dto.setFotoUrl(est.getFotoUrl());
        dto.setDiaPago(est.getDiaPago());
        dto.setFechaRegistro(est.getFechaRegistro());

        Sede sede = est.getSede();
        if (sede != null) {
            dto.setIdSede(sede.getIdSede());
            dto.setNombreSede(sede.getNombre());
        }

        if (mc != null) {
            LocalDate hoy = LocalDate.now();
            Long diasRestantes = mc.getFechaFin() != null
                    ? Math.max(0L, ChronoUnit.DAYS.between(hoy, mc.getFechaFin()))
                    : null;

            MembresiaResumen resumen = new MembresiaResumen();
            resumen.setIdMembresiaCore(mc.getIdMembresiaCore());
            resumen.setTipoMembresia(mc.getTipoMembresia() != null ? mc.getTipoMembresia().name() : null);
            resumen.setEstadoMembresia(mc.getEstadoMembresia() != null ? mc.getEstadoMembresia().name() : null);
            resumen.setFechaInicio(mc.getFechaInicio());
            resumen.setFechaFin(mc.getFechaFin());
            resumen.setFechaLimiteGracia(mc.getFechaLimiteGracia());
            resumen.setFechaLimiteCompromiso(mc.getFechaLimiteCompromiso());
            resumen.setValorMensual(mc.getValorMensual());
            boolean esPagoManual = mc.getTipoMembresia() == MembresiaCore.TipoMembresia.EFECTIVO;
            String obs = mc.getObservacion() != null ? mc.getObservacion()
                    : (esPagoManual && mc.getPagoOrigen() != null ? mc.getPagoOrigen().getObservacion() : null);
            resumen.setObservacion(obs);
            if (mc.getPagoOrigen() != null) {
                resumen.setMesPagado(mc.getPagoOrigen().getMesPagado());
            }
            resumen.setDiasRestantes(diasRestantes);
            resumen.setEsActiva(mc.getEsActiva());
            dto.setMembresiaActiva(resumen);
        }

        return dto;
    }
}
