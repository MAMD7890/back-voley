package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.PagoDocenteDto;
import galacticos_app_back.galacticos.dto.ReportePagoDocentesDto;
import galacticos_app_back.galacticos.entity.AsistenciaProfesor;
import galacticos_app_back.galacticos.entity.Profesor;
import galacticos_app_back.galacticos.repository.AsistenciaProfesorRepository;
import galacticos_app_back.galacticos.repository.ProfesorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReportePagoDocenteService {

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private AsistenciaProfesorRepository asistenciaProfesorRepository;

    @Transactional(readOnly = true)
    public ReportePagoDocentesDto generarReporte(LocalDate desde, LocalDate hasta, 
                                                   String nombreDocente, Integer idDocente) {
        List<Profesor> profesores = new ArrayList<>();

        // Filtrar docentes según los parámetros
        if (idDocente != null) {
            Optional<Profesor> prof = profesorRepository.findById(idDocente);
            if (prof.isPresent()) {
                profesores.add(prof.get());
            }
        } else if (nombreDocente != null && !nombreDocente.isBlank()) {
            profesores = profesorRepository.findByNombreContainingIgnoreCaseAndEstado(nombreDocente, true);
        } else {
            profesores = profesorRepository.findByEstado(true);
        }

        List<PagoDocenteDto> pagos = new ArrayList<>();
        BigDecimal totalAPagar = BigDecimal.ZERO;

        for (Profesor p : profesores) {
            List<AsistenciaProfesor> asistencias = asistenciaProfesorRepository
                    .findByProfesorIdProfesorAndFechaBetween(p.getIdProfesor(), desde, hasta);

            int cantAsistencias = asistencias.size();
            BigDecimal salarioPorClase = p.getSalarioPorClase() != null ? p.getSalarioPorClase() : BigDecimal.ZERO;
            BigDecimal total = salarioPorClase.multiply(BigDecimal.valueOf(cantAsistencias));

            PagoDocenteDto dto = new PagoDocenteDto();
            dto.setIdProfesor(p.getIdProfesor());
            dto.setNombreProfesor(p.getNombre());
            dto.setAsistencias(cantAsistencias);
            dto.setSalarioPorClase(salarioPorClase);
            dto.setTotalCalculado(total);
            dto.setPeriodoDesde(desde);
            dto.setPeriodoHasta(hasta);

            pagos.add(dto);
            totalAPagar = totalAPagar.add(total);
        }

        BigDecimal salarioPromedio = BigDecimal.ZERO;
        if (!pagos.isEmpty()) {
            salarioPromedio = totalAPagar.divide(BigDecimal.valueOf(pagos.size()), 2, RoundingMode.HALF_UP);
        }

        ReportePagoDocentesDto reporte = new ReportePagoDocentesDto();
        reporte.setPagos(pagos);
        reporte.setTotalAPagar(totalAPagar);
        reporte.setCantidadDocentes(pagos.size());
        reporte.setSalarioPromedio(salarioPromedio);

        return reporte;
    }
}
