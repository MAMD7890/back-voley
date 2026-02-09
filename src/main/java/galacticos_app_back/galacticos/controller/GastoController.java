package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.GastoResumenReporteDto;
import galacticos_app_back.galacticos.entity.Gasto;
import galacticos_app_back.galacticos.service.GastoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/gastos")
public class GastoController {
    
    @Autowired
    private GastoService gastoService;
    
    @GetMapping
    public ResponseEntity<List<Gasto>> obtenerTodos() {
        return ResponseEntity.ok(gastoService.obtenerTodos());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Gasto> obtenerPorId(@PathVariable Integer id) {
        Optional<Gasto> gasto = gastoService.obtenerPorId(id);
        return gasto.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Gasto> crear(@RequestBody Gasto gasto) {
        return ResponseEntity.ok(gastoService.crear(gasto));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Gasto> actualizar(@PathVariable Integer id, @RequestBody Gasto gasto) {
        Gasto actualizado = gastoService.actualizar(id, gasto);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        gastoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reporte")
    public ResponseEntity<GastoResumenReporteDto> generarReporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer sedeId,
            @RequestParam(required = false) String concepto,
            @RequestParam(required = false) String descripcion) {
        GastoResumenReporteDto res = gastoService.generarReporte(desde, hasta, sedeId, concepto, descripcion);
        return ResponseEntity.ok(res);
    }
}
