package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.ReportePagoWompiDTO;
import galacticos_app_back.galacticos.dto.ResumenPagosWompiDTO;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Pago;
import galacticos_app_back.galacticos.repository.EstudianteRepository;
import galacticos_app_back.galacticos.repository.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PagoService {
    
    @Autowired
    private PagoRepository pagoRepository;
    
    @Autowired
    private EstudianteRepository estudianteRepository;
    
    // Obtener todos los pagos
    public List<Pago> obtenerTodos() {
        return pagoRepository.findAll();
    }
    
    // Obtener pago por ID
    public Optional<Pago> obtenerPorId(Integer id) {
        return pagoRepository.findById(id);
    }
    
    // Obtener pagos de un estudiante
    public List<Pago> obtenerPorEstudiante(Integer idEstudiante) {
        return pagoRepository.findByEstudianteIdEstudiante(idEstudiante);
    }
    
    // Obtener pagos pendientes
    public List<Pago> obtenerPendientes() {
        return pagoRepository.findByEstadoPago(Pago.EstadoPago.PENDIENTE);
    }
    
    // Obtener pagos pagados
    public List<Pago> obtenerPagados() {
        return pagoRepository.findByEstadoPago(Pago.EstadoPago.PAGADO);
    }
    
    // Obtener pagos vencidos
    public List<Pago> obtenerVencidos() {
        return pagoRepository.findByEstadoPago(Pago.EstadoPago.VENCIDO);
    }
    
    // Registrar pago
    public Pago registrarPago(Pago pago) {
        pago.setEstadoPago(Pago.EstadoPago.PAGADO);
        return pagoRepository.save(pago);
    }
    
    // Actualizar pago completo
    public Pago actualizar(Integer id, Pago pago) {
        Optional<Pago> existente = pagoRepository.findById(id);
        if (existente.isPresent()) {
            pago.setIdPago(id);
            return pagoRepository.save(pago);
        }
        return null;
    }
    
    // Actualizar estado de pago
    public Pago actualizarEstado(Integer idPago, Pago.EstadoPago nuevoEstado) {
        Optional<Pago> pago = pagoRepository.findById(idPago);
        if (pago.isPresent()) {
            pago.get().setEstadoPago(nuevoEstado);
            return pagoRepository.save(pago.get());
        }
        return null;
    }
    
    // Eliminar pago
    public void eliminar(Integer idPago) {
        pagoRepository.deleteById(idPago);
    }
    
    // ====================================
    // REPORTES DE PAGOS WOMPI
    // ====================================
    
    /**
     * Obtiene todos los pagos realizados con información del estudiante
     */
    public List<ReportePagoWompiDTO> obtenerReportePagosWompi() {
        List<Pago> pagos = pagoRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaPago", "horaPago"));
        return pagos.stream()
                .map(this::convertirAPagoWompiDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene pagos paginados para el reporte
     */
    public Page<ReportePagoWompiDTO> obtenerReportePagosPaginado(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaPago", "horaPago"));
        Page<Pago> pagos = pagoRepository.findAll(pageable);
        return pagos.map(this::convertirAPagoWompiDTO);
    }
    
    /**
     * Obtiene solo pagos online (Wompi)
     */
    public List<ReportePagoWompiDTO> obtenerPagosOnline() {
        List<Pago> pagos = pagoRepository.findByMetodoPago(Pago.MetodoPago.ONLINE);
        return pagos.stream()
                .sorted((p1, p2) -> {
                    if (p2.getFechaPago() == null) return -1;
                    if (p1.getFechaPago() == null) return 1;
                    return p2.getFechaPago().compareTo(p1.getFechaPago());
                })
                .map(this::convertirAPagoWompiDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene pagos por rango de fechas
     */
    public List<ReportePagoWompiDTO> obtenerPagosPorFechas(LocalDate desde, LocalDate hasta) {
        List<Pago> pagos = pagoRepository.findByFechaPagoBetween(desde, hasta);
        return pagos.stream()
                .map(this::convertirAPagoWompiDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene pagos de un estudiante específico
     */
    public List<ReportePagoWompiDTO> obtenerPagosEstudiante(Integer idEstudiante) {
        List<Pago> pagos = pagoRepository.findByEstudianteIdEstudiante(idEstudiante);
        return pagos.stream()
                .sorted((p1, p2) -> {
                    if (p2.getFechaPago() == null) return -1;
                    if (p1.getFechaPago() == null) return 1;
                    return p2.getFechaPago().compareTo(p1.getFechaPago());
                })
                .map(this::convertirAPagoWompiDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene resumen estadístico de pagos
     */
    public ResumenPagosWompiDTO obtenerResumenPagos() {
        List<Pago> todosPagos = pagoRepository.findAll();
        
        // Contar por estado
        long pagosAprobados = todosPagos.stream()
                .filter(p -> p.getEstadoPago() == Pago.EstadoPago.PAGADO)
                .count();
        long pagosPendientes = todosPagos.stream()
                .filter(p -> p.getEstadoPago() == Pago.EstadoPago.PENDIENTE)
                .count();
        long pagosVencidos = todosPagos.stream()
                .filter(p -> p.getEstadoPago() == Pago.EstadoPago.VENCIDO)
                .count();
        
        // Calcular montos
        BigDecimal montoRecaudado = todosPagos.stream()
                .filter(p -> p.getEstadoPago() == Pago.EstadoPago.PAGADO)
                .map(Pago::getValor)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal montoPendiente = todosPagos.stream()
                .filter(p -> p.getEstadoPago() == Pago.EstadoPago.PENDIENTE)
                .map(Pago::getValor)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Por método de pago
        long pagosOnline = todosPagos.stream()
                .filter(p -> p.getMetodoPago() == Pago.MetodoPago.ONLINE)
                .count();
        long pagosEfectivo = todosPagos.stream()
                .filter(p -> p.getMetodoPago() == Pago.MetodoPago.EFECTIVO)
                .count();
        
        BigDecimal montoOnline = todosPagos.stream()
                .filter(p -> p.getMetodoPago() == Pago.MetodoPago.ONLINE && p.getEstadoPago() == Pago.EstadoPago.PAGADO)
                .map(Pago::getValor)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal montoEfectivo = todosPagos.stream()
                .filter(p -> p.getMetodoPago() == Pago.MetodoPago.EFECTIVO && p.getEstadoPago() == Pago.EstadoPago.PAGADO)
                .map(Pago::getValor)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Promedio
        BigDecimal promedio = pagosAprobados > 0 
                ? montoRecaudado.divide(BigDecimal.valueOf(pagosAprobados), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        // Estadísticas de estudiantes
        List<Estudiante> estudiantes = estudianteRepository.findAll();
        long estudiantesAlDia = estudiantes.stream()
                .filter(e -> e.getEstadoPago() == Estudiante.EstadoPago.AL_DIA)
                .count();
        long estudiantesEnMora = estudiantes.stream()
                .filter(e -> e.getEstadoPago() == Estudiante.EstadoPago.EN_MORA)
                .count();
        long estudiantesPendientes = estudiantes.stream()
                .filter(e -> e.getEstadoPago() == Estudiante.EstadoPago.PENDIENTE)
                .count();
        long estudiantesConCompromiso = estudiantes.stream()
                .filter(e -> e.getEstadoPago() == Estudiante.EstadoPago.COMPROMISO_PAGO)
                .count();
        
        // Últimos 10 pagos
        List<ReportePagoWompiDTO> ultimosPagos = todosPagos.stream()
                .filter(p -> p.getEstadoPago() == Pago.EstadoPago.PAGADO)
                .sorted((p1, p2) -> {
                    if (p2.getFechaPago() == null) return -1;
                    if (p1.getFechaPago() == null) return 1;
                    int cmp = p2.getFechaPago().compareTo(p1.getFechaPago());
                    if (cmp != 0) return cmp;
                    if (p2.getHoraPago() == null) return -1;
                    if (p1.getHoraPago() == null) return 1;
                    return p2.getHoraPago().compareTo(p1.getHoraPago());
                })
                .limit(10)
                .map(this::convertirAPagoWompiDTO)
                .collect(Collectors.toList());
        
        // Pagos por mes (últimos 6 meses)
        List<ResumenPagosWompiDTO.PagosPorMesDTO> pagosPorMes = calcularPagosPorMes(todosPagos);
        
        return ResumenPagosWompiDTO.builder()
                .totalPagos((long) todosPagos.size())
                .pagosAprobados(pagosAprobados)
                .pagosPendientes(pagosPendientes)
                .pagosRechazados(pagosVencidos)
                .montoTotalRecaudado(montoRecaudado)
                .montoPendiente(montoPendiente)
                .montoPromedioTransaccion(promedio)
                .pagosOnline(pagosOnline)
                .pagosEfectivo(pagosEfectivo)
                .montoOnline(montoOnline)
                .montoEfectivo(montoEfectivo)
                .estudiantesAlDia(estudiantesAlDia)
                .estudiantesEnMora(estudiantesEnMora)
                .estudiantesPendientes(estudiantesPendientes)
                .estudiantesConCompromiso(estudiantesConCompromiso)
                .ultimosPagos(ultimosPagos)
                .pagosPorMes(pagosPorMes)
                .build();
    }
    
    /**
     * Calcula pagos agrupados por mes
     */
    private List<ResumenPagosWompiDTO.PagosPorMesDTO> calcularPagosPorMes(List<Pago> pagos) {
        List<ResumenPagosWompiDTO.PagosPorMesDTO> resultado = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        
        // Agrupar por mes
        pagos.stream()
                .filter(p -> p.getFechaPago() != null && p.getEstadoPago() == Pago.EstadoPago.PAGADO)
                .collect(Collectors.groupingBy(
                        p -> p.getFechaPago().format(formatter),
                        Collectors.toList()
                ))
                .forEach((mes, pagosMes) -> {
                    BigDecimal total = pagosMes.stream()
                            .map(Pago::getValor)
                            .filter(v -> v != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    resultado.add(ResumenPagosWompiDTO.PagosPorMesDTO.builder()
                            .mes(mes)
                            .cantidad((long) pagosMes.size())
                            .monto(total)
                            .build());
                });
        
        // Ordenar por mes
        resultado.sort((a, b) -> b.getMes().compareTo(a.getMes()));
        
        // Limitar a 6 meses
        return resultado.stream().limit(6).collect(Collectors.toList());
    }
    
    /**
     * Convierte una entidad Pago a DTO con información del estudiante
     */
    private ReportePagoWompiDTO convertirAPagoWompiDTO(Pago pago) {
        ReportePagoWompiDTO.ReportePagoWompiDTOBuilder builder = ReportePagoWompiDTO.builder()
                .idPago(pago.getIdPago())
                .referenciaPago(pago.getReferenciaPago())
                .wompiTransactionId(pago.getWompiTransactionId())
                .monto(pago.getValor())
                .moneda("COP")
                .fechaPago(pago.getFechaPago())
                .horaPago(pago.getHoraPago())
                .mesPagado(pago.getMesPagado())
                .metodoPago(pago.getMetodoPago() != null ? pago.getMetodoPago().name() : null)
                .estadoPago(pago.getEstadoPago() != null ? pago.getEstadoPago().name() : null);
        
        // Agregar información del estudiante
        if (pago.getEstudiante() != null) {
            Estudiante est = pago.getEstudiante();
            builder.idEstudiante(est.getIdEstudiante())
                    .nombreEstudiante(est.getNombreCompleto())
                    .emailEstudiante(est.getCorreoEstudiante())
                    .telefonoEstudiante(est.getCelularEstudiante())
                    .documentoEstudiante(est.getNumeroDocumento())
                    .fotoEstudiante(est.getFotoUrl())
                    .nombreCompleto(est.getNombreCompleto());
            
            // Estado de pago del estudiante con color
            if (est.getEstadoPago() != null) {
                builder.estadoPagoEstudiante(est.getEstadoPago().name());
                switch (est.getEstadoPago()) {
                    case AL_DIA:
                        builder.colorEstadoPago("#28a745");
                        builder.descripcionEstadoPago("Al día");
                        break;
                    case EN_MORA:
                        builder.colorEstadoPago("#dc3545");
                        builder.descripcionEstadoPago("En mora");
                        break;
                    case PENDIENTE:
                        builder.colorEstadoPago("#ffc107");
                        builder.descripcionEstadoPago("Pendiente");
                        break;
                    case COMPROMISO_PAGO:
                        builder.colorEstadoPago("#17a2b8");
                        builder.descripcionEstadoPago("Compromiso de pago");
                        break;
                }
            }
        }
        
        return builder.build();
    }
}