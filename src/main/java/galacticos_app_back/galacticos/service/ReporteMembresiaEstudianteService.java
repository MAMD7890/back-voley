package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.ReporteMembresiaEstudianteDto;
import galacticos_app_back.galacticos.entity.Membresia;
import galacticos_app_back.galacticos.repository.MembresiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReporteMembresiaEstudianteService {

    @Autowired
    private MembresiaRepository membresiaRepository;

    @Transactional(readOnly = true)
    public ReporteMembresiaEstudianteDto generarReporte(LocalDate desde, LocalDate hasta, Integer equipoId, Integer sedeId) {
        List<Membresia> membresias;

        // Obtener membresías en el rango de fechas
        membresias = membresiaRepository.findByFechaInicioBetween(desde, hasta);

        // Aplicar filtros opcionales
        if (equipoId != null) {
            membresias = membresias.stream()
                    .filter(m -> m.getEquipo() != null && m.getEquipo().getIdEquipo().equals(equipoId))
                    .collect(Collectors.toList());
        }

        if (sedeId != null) {
            membresias = membresias.stream()
                    .filter(m -> m.getEquipo() != null && m.getEquipo().getSede() != null 
                            && m.getEquipo().getSede().getIdSede().equals(sedeId))
                    .collect(Collectors.toList());
        }

        // Contar por estado
        long alDia = membresias.stream()
                .filter(m -> m.getEstado() == true) // AL_DIA ahora es true
                .count();
        long enMora = membresias.stream()
                .filter(m -> m.getEstado() == false) // EN_MORA ahora es false
                .count();
        long suspendido = membresias.stream()
                .filter(m -> m.getEstado() == false) // SUSPENDIDO ahora es false
                .count();

        int total = membresias.size();

        // Calcular porcentajes
        BigDecimal porcentajeAlDia = total > 0 
                ? BigDecimal.valueOf(alDia).multiply(BigDecimal.valueOf(100))
                  .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal porcentajeEnMora = total > 0 
                ? BigDecimal.valueOf(enMora).multiply(BigDecimal.valueOf(100))
                  .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal porcentajeSuspendido = total > 0 
                ? BigDecimal.valueOf(suspendido).multiply(BigDecimal.valueOf(100))
                  .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Crear detalle de estados
        Map<String, Object> detalleEstados = new HashMap<>();
        detalleEstados.put("AL_DIA", alDia);
        detalleEstados.put("EN_MORA", enMora);
        detalleEstados.put("SUSPENDIDO", suspendido);

        // Armar reporte
        ReporteMembresiaEstudianteDto reporte = new ReporteMembresiaEstudianteDto();
        reporte.setTotalEstudiantes(total);
        reporte.setEstudiantesAlDia((int) alDia);
        reporte.setEstudiantesEnMora((int) enMora);
        reporte.setEstudiantesSuspendidos((int) suspendido);
        reporte.setPorcentajeAlDia(porcentajeAlDia);
        reporte.setPorcentajeEnMora(porcentajeEnMora);
        reporte.setPorcentajeSuspendido(porcentajeSuspendido);
        reporte.setDetalleEstados(detalleEstados);
        reporte.setPeriodoDesde(desde.toString());
        reporte.setPeriodoHasta(hasta.toString());

        return reporte;
    }
}
