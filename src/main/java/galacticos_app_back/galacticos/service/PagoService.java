package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.RegistroAcuerdoCarteraDTO;
import galacticos_app_back.galacticos.dto.RegistroPagoManualDTO;
import galacticos_app_back.galacticos.dto.ReportePagoWompiDTO;
import galacticos_app_back.galacticos.dto.ResumenPagosWompiDTO;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Pago;
import galacticos_app_back.galacticos.repository.EstudianteRepository;
import galacticos_app_back.galacticos.repository.PagoRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    @Autowired
    private MembresiaCoreService membresiaCoreService;
    
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
    
    // Registrar pago (flujo legacy)
    public Pago registrarPago(Pago pago) {
        pago.setEstadoPago(Pago.EstadoPago.PAGADO);
        return pagoRepository.save(pago);
    }

    // Registrar pago manual (EFECTIVO / TRANSFERENCIA) con rango de fechas
    @Transactional
    public Map<String, Object> registrarPagoManual(RegistroPagoManualDTO dto) {
        Estudiante estudiante = estudianteRepository.findById(dto.getIdEstudiante())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado: " + dto.getIdEstudiante()));

        if (dto.getFechaInicio() == null || dto.getFechaFin() == null) {
            throw new IllegalArgumentException("fechaInicio y fechaFin son obligatorias");
        }
        if (dto.getMetodoPago() == Pago.MetodoPago.ONLINE) {
            throw new IllegalArgumentException("Este endpoint es solo para pagos manuales (EFECTIVO o TRANSFERENCIA)");
        }

        // Evitar duplicados por doble clic / doble envío del formulario: si ya existe
        // un pago idéntico (mismo estudiante, mismo método, mismo valor) registrado
        // hace pocos segundos, no crear otro.
        Pago.MetodoPago metodoPago = dto.getMetodoPago() != null ? dto.getMetodoPago() : Pago.MetodoPago.EFECTIVO;
        LocalTime ahora = LocalTime.now(ZoneId.of("America/Bogota"));
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));
        boolean posibleDuplicado = pagoRepository.findByEstudianteIdEstudiante(dto.getIdEstudiante()).stream()
                .anyMatch(p -> p.getEstadoPago() == Pago.EstadoPago.PAGADO
                        && p.getMetodoPago() == metodoPago
                        && dto.getValor() != null && dto.getValor().compareTo(p.getValor() != null ? p.getValor() : java.math.BigDecimal.ZERO) == 0
                        && hoy.equals(p.getFechaPago())
                        && p.getHoraPago() != null
                        && Math.abs(java.time.Duration.between(p.getHoraPago(), ahora).getSeconds()) < 15);
        if (posibleDuplicado) {
            throw new IllegalStateException(
                    "Ya se registró un pago idéntico para este estudiante hace unos segundos. Evita registrarlo dos veces.");
        }

        Pago pago = new Pago();
        pago.setEstudiante(estudiante);
        pago.setValor(dto.getValor());
        pago.setMetodoPago(dto.getMetodoPago() != null ? dto.getMetodoPago() : Pago.MetodoPago.EFECTIVO);
        pago.setObservacion(dto.getObservacion());
        pago.setMesPagado(dto.getMesPagado());
        pago.setFechaPago(LocalDate.now(ZoneId.of("America/Bogota")));
        pago.setHoraPago(LocalTime.now(ZoneId.of("America/Bogota")));
        pago.setEstadoPago(Pago.EstadoPago.PAGADO);
        pago.setReferenciaPago("EFECT-" + dto.getIdEstudiante() + "-" + System.currentTimeMillis());
        Pago pagoGuardado = pagoRepository.save(pago);

        Map<String, Object> resultado = membresiaCoreService.crearMembresiaManualConFechas(
                estudiante, pagoGuardado, dto.getFechaInicio(), dto.getFechaFin());
        resultado.put("pago", pagoGuardado);
        return resultado;
    }
    
    /**
     * Registra un abono de cartera (paz y salvo con deudas). Se guarda como un
     * Pago normal, asociado al estudiante, pero a propósito NUNCA llama a
     * membresiaCoreService: este tipo de pago no debe crear, extender ni
     * recalcular ninguna membresía. Las queries de reconciliación de
     * membresías en PagoRepository excluyen explícitamente ACUERDO_CARTERA
     * para que ningún job automático lo termine convirtiendo en membresía.
     */
    @Transactional
    public Pago registrarAcuerdoCartera(RegistroAcuerdoCarteraDTO dto) {
        Estudiante estudiante = estudianteRepository.findById(dto.getIdEstudiante())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado: " + dto.getIdEstudiante()));

        if (dto.getValor() == null || dto.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor del abono debe ser mayor a 0");
        }

        Pago pago = new Pago();
        pago.setEstudiante(estudiante);
        pago.setValor(dto.getValor());
        pago.setMetodoPago(Pago.MetodoPago.ACUERDO_CARTERA);
        pago.setObservacion(dto.getObservacion());
        pago.setFechaPago(LocalDate.now(ZoneId.of("America/Bogota")));
        pago.setHoraPago(LocalTime.now(ZoneId.of("America/Bogota")));
        pago.setEstadoPago(Pago.EstadoPago.PAGADO);
        pago.setReferenciaPago("CARTERA-" + dto.getIdEstudiante() + "-" + System.currentTimeMillis());
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
     * Obtiene pagos paginados con filtros opcionales:
     * - desde/hasta: rango de fechaPago
     * - estado: string exacto comparado con estadoPago (ej: "PAGADO", "PENDIENTE")
     * - metodo: string exacto comparado con metodoPago (ej: "ONLINE", "EFECTIVO")
     * - tipo: "CARTERA" (solo ACUERDO_CARTERA) o "MEMBRESIA" (todo lo demás). Si se
     *   envían tipo y metodo a la vez, ambos se aplican (deben ser compatibles).
     * - busqueda: coincidencia parcial en nombre, email o referenciaPago
     * - idSede: id de la sede del estudiante
     */
    public Page<ReportePagoWompiDTO> obtenerReportePagosPaginado(
            int page, int size,
            LocalDate desde, LocalDate hasta,
            String estado, String metodo, String tipo, String busqueda, Integer idSede) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaPago", "horaPago"));
        Specification<Pago> spec = construirSpecPagos(desde, hasta, estado, metodo, tipo, busqueda, idSede);

        Page<Pago> pagos = pagoRepository.findAll(spec, pageable);
        return pagos.map(this::convertirAPagoWompiDTO);
    }

    /**
     * Construye el filtro de búsqueda de pagos, usado tanto por el listado
     * paginado como por la exportación a Excel — así ambos aplican exactamente
     * los mismos criterios.
     */
    private Specification<Pago> construirSpecPagos(
            LocalDate desde, LocalDate hasta,
            String estado, String metodo, String tipo, String busqueda, Integer idSede) {

        Specification<Pago> spec = (root, query, cb) -> cb.conjunction();

        if (desde != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("fechaPago"), desde));
        }
        if (hasta != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("fechaPago"), hasta));
        }
        if (estado != null && !estado.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("estadoPago").as(String.class), estado.toUpperCase()));
        }
        if (metodo != null && !metodo.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("metodoPago").as(String.class), metodo.toUpperCase()));
        }
        if (tipo != null && !tipo.isBlank()) {
            if ("CARTERA".equalsIgnoreCase(tipo)) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("metodoPago").as(String.class), Pago.MetodoPago.ACUERDO_CARTERA.name()));
            } else if ("MEMBRESIA".equalsIgnoreCase(tipo) || "MEMBRESÍA".equalsIgnoreCase(tipo)) {
                spec = spec.and((root, query, cb) ->
                        cb.notEqual(root.get("metodoPago").as(String.class), Pago.MetodoPago.ACUERDO_CARTERA.name()));
            }
        }
        if (busqueda != null && !busqueda.isBlank()) {
            String pattern = "%" + busqueda.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<Pago, Estudiante> est = root.join("estudiante", JoinType.LEFT);
                return cb.or(
                        cb.like(cb.lower(est.get("nombreCompleto")), pattern),
                        cb.like(cb.lower(est.get("correoEstudiante")), pattern),
                        cb.like(cb.lower(root.get("referenciaPago")), pattern)
                );
            });
        }
        if (idSede != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<Pago, Estudiante> est = root.join("estudiante", JoinType.LEFT);
                return cb.equal(est.get("sede").get("idSede"), idSede);
            });
        }
        return spec;
    }

    /**
     * Exporta a Excel (.xlsx) TODOS los pagos que cumplan los filtros — sin
     * paginar — usando exactamente los mismos criterios que obtenerReportePagosPaginado.
     */
    public byte[] exportarPagosExcel(
            LocalDate desde, LocalDate hasta,
            String estado, String metodo, String tipo, String busqueda, Integer idSede) {

        Specification<Pago> spec = construirSpecPagos(desde, hasta, estado, metodo, tipo, busqueda, idSede);
        List<Pago> pagos = pagoRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "fechaPago", "horaPago"));

        List<ReportePagoWompiDTO> filas = pagos.stream()
                .map(this::convertirAPagoWompiDTO)
                .collect(Collectors.toList());

        return generarExcelPagos(filas);
    }

    private byte[] generarExcelPagos(List<ReportePagoWompiDTO> filas) {
        String[] encabezados = {
                "ID Pago", "Referencia", "Transaction ID Wompi", "Monto", "Moneda",
                "Fecha Pago", "Hora Pago", "Mes Pagado", "Método", "Estado", "Observación",
                "ID Estudiante", "Nombre Estudiante", "Email Estudiante", "Teléfono Estudiante",
                "Documento Estudiante"
        };

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Pagos");

            org.apache.poi.ss.usermodel.CellStyle estiloEncabezado = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font fuenteEncabezado = workbook.createFont();
            fuenteEncabezado.setBold(true);
            estiloEncabezado.setFont(fuenteEncabezado);

            org.apache.poi.ss.usermodel.Row filaEncabezado = sheet.createRow(0);
            for (int i = 0; i < encabezados.length; i++) {
                org.apache.poi.ss.usermodel.Cell celda = filaEncabezado.createCell(i);
                celda.setCellValue(encabezados[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            int numFila = 1;
            for (ReportePagoWompiDTO p : filas) {
                org.apache.poi.ss.usermodel.Row fila = sheet.createRow(numFila++);
                int col = 0;
                setCelda(fila, col++, p.getIdPago());
                setCelda(fila, col++, p.getReferenciaPago());
                setCelda(fila, col++, p.getWompiTransactionId());
                setCelda(fila, col++, p.getMonto() != null ? p.getMonto().doubleValue() : null);
                setCelda(fila, col++, p.getMoneda());
                setCelda(fila, col++, p.getFechaPago() != null ? p.getFechaPago().toString() : null);
                setCelda(fila, col++, p.getHoraPago() != null ? p.getHoraPago().toString() : null);
                setCelda(fila, col++, p.getMesPagado());
                setCelda(fila, col++, p.getMetodoPago());
                setCelda(fila, col++, p.getEstadoPago());
                setCelda(fila, col++, p.getObservacion());
                setCelda(fila, col++, p.getIdEstudiante());
                setCelda(fila, col++, p.getNombreCompleto() != null ? p.getNombreCompleto() : p.getNombreEstudiante());
                setCelda(fila, col++, p.getEmailEstudiante());
                setCelda(fila, col++, p.getTelefonoEstudiante());
                setCelda(fila, col, p.getDocumentoEstudiante());
            }

            for (int i = 0; i < encabezados.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error generando el archivo Excel de pagos: " + e.getMessage(), e);
        }
    }

    private void setCelda(org.apache.poi.ss.usermodel.Row fila, int col, Object valor) {
        org.apache.poi.ss.usermodel.Cell celda = fila.createCell(col);
        if (valor == null) {
            celda.setBlank();
        } else if (valor instanceof Number) {
            celda.setCellValue(((Number) valor).doubleValue());
        } else {
            celda.setCellValue(valor.toString());
        }
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
                .estadoPago(pago.getEstadoPago() != null ? pago.getEstadoPago().name() : null)
                .observacion(pago.getObservacion());
        
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
                    case DECLINADO:
                        builder.colorEstadoPago("#6c757d");
                        builder.descripcionEstadoPago("Declinado");
                        break;
                }
            }
        }
        
        return builder.build();
    }
}