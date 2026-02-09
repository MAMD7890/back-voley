package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.ReportePagoWompiDTO;
import galacticos_app_back.galacticos.dto.ResumenPagosWompiDTO;
import galacticos_app_back.galacticos.entity.Pago;
import galacticos_app_back.galacticos.service.PagoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {
    
    @Autowired
    private PagoService pagoService;
    
    @GetMapping
    public ResponseEntity<List<Pago>> obtenerTodos() {
        return ResponseEntity.ok(pagoService.obtenerTodos());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Pago> obtenerPorId(@PathVariable Integer id) {
        Optional<Pago> pago = pagoService.obtenerPorId(id);
        return pago.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/estudiante/{idEstudiante}")
    public ResponseEntity<List<Pago>> obtenerPorEstudiante(@PathVariable Integer idEstudiante) {
        return ResponseEntity.ok(pagoService.obtenerPorEstudiante(idEstudiante));
    }
    
    @GetMapping("/pendientes/lista")
    public ResponseEntity<List<Pago>> obtenerPendientes() {
        return ResponseEntity.ok(pagoService.obtenerPendientes());
    }
    
    @GetMapping("/pagados/lista")
    public ResponseEntity<List<Pago>> obtenerPagados() {
        return ResponseEntity.ok(pagoService.obtenerPagados());
    }
    
    @GetMapping("/vencidos/lista")
    public ResponseEntity<List<Pago>> obtenerVencidos() {
        return ResponseEntity.ok(pagoService.obtenerVencidos());
    }
    
    @PostMapping
    public ResponseEntity<Pago> crear(@RequestBody Pago pago) {
        return ResponseEntity.ok(pagoService.registrarPago(pago));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Pago> actualizar(@PathVariable Integer id, @RequestBody Pago pago) {
        Pago actualizado = pagoService.actualizar(id, pago);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }
    
    @PatchMapping("/{id}/estado/{estado}")
    public ResponseEntity<Pago> actualizarEstado(@PathVariable Integer id, @PathVariable Pago.EstadoPago estado) {
        Pago actualizado = pagoService.actualizarEstado(id, estado);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        pagoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    
    // ==================== ENDPOINTS DE REPORTES PARA ADMIN ====================
    
    /**
     * Obtiene el resumen general de pagos para el dashboard
     * Incluye: totales, montos, estadísticas por método y estado de estudiantes
     */
    @GetMapping("/reportes/resumen")
    public ResponseEntity<ResumenPagosWompiDTO> obtenerResumenPagos() {
        return ResponseEntity.ok(pagoService.obtenerResumenPagos());
    }
    
    /**
     * Obtiene todos los pagos con información del estudiante asociado
     */
    @GetMapping("/reportes/wompi")
    public ResponseEntity<List<ReportePagoWompiDTO>> obtenerReportePagosWompi() {
        return ResponseEntity.ok(pagoService.obtenerReportePagosWompi());
    }
    
    /**
     * Obtiene los pagos con paginación
     */
    @GetMapping("/reportes/wompi/paginado")
    public ResponseEntity<Page<ReportePagoWompiDTO>> obtenerReportePagosPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(pagoService.obtenerReportePagosPaginado(page, size));
    }
    
    /**
     * Obtiene solo los pagos realizados online (Wompi)
     */
    @GetMapping("/reportes/online")
    public ResponseEntity<List<ReportePagoWompiDTO>> obtenerPagosOnline() {
        return ResponseEntity.ok(pagoService.obtenerPagosOnline());
    }
    
    /**
     * Obtiene pagos filtrados por rango de fechas
     */
    @GetMapping("/reportes/fecha")
    public ResponseEntity<List<ReportePagoWompiDTO>> obtenerPagosPorFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(pagoService.obtenerPagosPorFechas(desde, hasta));
    }
    
    /**
     * Obtiene los pagos de un estudiante específico
     */
    @GetMapping("/reportes/estudiante/{idEstudiante}")
    public ResponseEntity<List<ReportePagoWompiDTO>> obtenerPagosEstudiante(
            @PathVariable Integer idEstudiante) {
        return ResponseEntity.ok(pagoService.obtenerPagosEstudiante(idEstudiante));
    }
}
