package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.GastoReporteDto;
import galacticos_app_back.galacticos.dto.GastoResumenReporteDto;
import galacticos_app_back.galacticos.entity.Gasto;
import galacticos_app_back.galacticos.repository.GastoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GastoService {
    
    @Autowired
    private GastoRepository gastoRepository;
    
    public List<Gasto> obtenerTodos() {
        return gastoRepository.findAll();
    }
    
    public Optional<Gasto> obtenerPorId(Integer id) {
        return gastoRepository.findById(id);
    }
    
    public Gasto crear(Gasto gasto) {
        return gastoRepository.save(gasto);
    }
    
    public Gasto actualizar(Integer id, Gasto gasto) {
        Optional<Gasto> existente = gastoRepository.findById(id);
        if (existente.isPresent()) {
            gasto.setIdGasto(id);
            return gastoRepository.save(gasto);
        }
        return null;
    }
    
    public void eliminar(Integer id) {
        gastoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public GastoResumenReporteDto generarReporte(LocalDate desde, LocalDate hasta, 
                                                  Integer sedeId, String concepto, String descripcion) {
        List<Gasto> gastos = gastoRepository.findByFechaBetween(desde, hasta);
        
        // Aplicar filtros opcionales
        if (sedeId != null) {
            gastos = gastos.stream()
                    .filter(g -> g.getSede() != null && g.getSede().getIdSede().equals(sedeId))
                    .collect(Collectors.toList());
        }
        if (concepto != null && !concepto.isBlank()) {
            gastos = gastos.stream()
                    .filter(g -> g.getConcepto() != null && g.getConcepto().equalsIgnoreCase(concepto))
                    .collect(Collectors.toList());
        }
        if (descripcion != null && !descripcion.isBlank()) {
            gastos = gastos.stream()
                    .filter(g -> g.getDescripcion() != null && g.getDescripcion().toLowerCase().contains(descripcion.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Mapear a DTOs
        List<GastoReporteDto> dtos = gastos.stream().map(this::mapToDto).collect(Collectors.toList());

        // Calcular totales
        BigDecimal totalGastos = gastos.stream()
                .map(g -> g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Gasto mayor
        GastoReporteDto gastoMayor = null;
        if (!gastos.isEmpty()) {
            Gasto gm = gastos.stream()
                    .max(Comparator.comparing(g -> g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO))
                    .orElse(null);
            if (gm != null) gastoMayor = mapToDto(gm);
        }

        // Día con mayor gasto
        LocalDate diaMaxGasto = null;
        BigDecimal totalDiaMaxGasto = BigDecimal.ZERO;
        if (!gastos.isEmpty()) {
            Map<LocalDate, BigDecimal> porDia = new TreeMap<>();
            for (Gasto g : gastos) {
                BigDecimal monto = g.getMonto() != null ? g.getMonto() : BigDecimal.ZERO;
                porDia.put(g.getFecha(), porDia.getOrDefault(g.getFecha(), BigDecimal.ZERO).add(monto));
            }
            Map.Entry<LocalDate, BigDecimal> maxDia = porDia.entrySet().stream()
                    .max(Comparator.comparing(Map.Entry::getValue))
                    .orElse(null);
            if (maxDia != null) {
                diaMaxGasto = maxDia.getKey();
                totalDiaMaxGasto = maxDia.getValue();
            }
        }

        GastoResumenReporteDto resumen = new GastoResumenReporteDto();
        resumen.setGastos(dtos);
        resumen.setTotalGastos(totalGastos);
        resumen.setGastoMayor(gastoMayor);
        resumen.setDiaMaxGasto(diaMaxGasto);
        resumen.setTotalDiaMaxGasto(totalDiaMaxGasto);
        resumen.setCantidadGastos(gastos.size());
        return resumen;
    }

    private GastoReporteDto mapToDto(Gasto g) {
        GastoReporteDto dto = new GastoReporteDto();
        dto.setIdGasto(g.getIdGasto());
        dto.setConcepto(g.getConcepto());
        dto.setDescripcion(g.getDescripcion());
        dto.setMonto(g.getMonto());
        dto.setFecha(g.getFecha());
        if (g.getSede() != null) {
            dto.setSedeName(g.getSede().getNombre());
        }
        return dto;
    }
}
